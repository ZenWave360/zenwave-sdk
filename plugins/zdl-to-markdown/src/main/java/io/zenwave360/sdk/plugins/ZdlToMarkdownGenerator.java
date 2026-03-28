package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.github.jknack.handlebars.Handlebars;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.apache.commons.lang3.StringUtils;

public class ZdlToMarkdownGenerator extends AbstractZDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "zdl";

    public enum OutputFormat {
        glossary, aggregate, plantuml
    }

    @DocumentedOption(description = "Template type")
    public OutputFormat outputFormat = OutputFormat.glossary;

    @DocumentedOption(description = "Aggregate name")
    public String aggregateName;

    @DocumentedOption(description = "Target file")
    public String targetFile = "zdl-glossary.md";

    private Handlebars getHandlebars() {
        return ((HandlebarsEngine) getTemplateEngine()).getHandlebars();
    }

    private final TemplateInput modelGlossaryTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownGlossary.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelAggregateTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownAggregate.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelUmlTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownPlantUML.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);

    public ZdlToMarkdownGenerator withOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    public ZdlToMarkdownGenerator withAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
        return this;
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        Map<String, Object> zdlModel = (Map) contextModel.get(sourceProperty);

        if(outputFormat == OutputFormat.aggregate) {

            targetFile = "zdl-aggregate-" + aggregateName + ".md";
            var aggregate = resolveAggregate(zdlModel, aggregateName);
            var entity = aggregate != null
                    ? resolveAggregateRootEntity(zdlModel, aggregate)
                    : (Map) JSONPath.get(zdlModel, "$.entities." + aggregateName);
            zdlModel.put("aggregate", aggregate);
            zdlModel.put("entity", entity);
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, modelAggregateTemplate, zdlModel));

        } else {

            TemplateInput template = switch (outputFormat) {
                case glossary -> {
                    targetFile = "zdl-glossary.md";
                    yield modelGlossaryTemplate;
                }
                case plantuml -> {
                    targetFile = "zdl-plantuml.md";
                    yield modelUmlTemplate;
                }
                default -> {
                    targetFile = "zdl-markdown.md";
                    yield modelGlossaryTemplate;
                }
            };

            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, template, zdlModel));
        }
        return generatedProjectFiles;
    }

    private final Map<String, String> inverseRelationshipTypes = Map.of("OneToMany", "ManyToOne","ManyToOne", "OneToMany","ManyToMany", "ManyToMany", "OneToOne", "OneToOne");
    {
        getHandlebars().registerHelper("entityAssociations", (entity, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            return collectEntityAssociations((Map<String, Object>) entity, (Map<String, Object>) zdlModel);
        });
        getHandlebars().registerHelper("collectionAssociations", (entities, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            return collectCollectionAssociations((Collection<Map<String, Object>>) entities, (Map<String, Object>) zdlModel);
        });
        getHandlebars().registerHelper("associationTargets", (associations, options) -> {
            Object excludedNames = options.param(0, null);
            return uniqueAssociationTargets((Collection<Map<String, Object>>) associations, excludedNames);
        });
        getHandlebars().registerHelper("resolveDiagramAggregate", (aggregateOrEntityName, options) -> {
            var zdlModel = (Map<String, Object>) options.context.get("zdlModel", true);
            return resolveAggregate(zdlModel, aggregateOrEntityName);
        });
        getHandlebars().registerHelper("resolveDiagramEntity", (aggregateOrEntityName, options) -> {
            var zdlModel = (Map<String, Object>) options.context.get("zdlModel", true);
            return resolveDiagramEntity(zdlModel, aggregateOrEntityName);
        });
        getHandlebars().registerHelper("relationshipType", (relationship, options) -> {
            boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
            var type = JSONPath.get(relationship, "type");
            return isOwnerSide ? type : inverseRelationshipTypes.get(type);
        });
        getHandlebars().registerHelper("methodParamsSignature", (method, options) -> {
            return ServiceHelperFormatter.methodParamsSignature((Map<String, Object>) method);
        });

        getHandlebars().registerHelper("methodReturnType", (method, options) -> {
            return ServiceHelperFormatter.methodReturnType((Map<String, Object>) method);
        });

        getHandlebars().registerHelper("methodEvents", (method, options) -> {
            return ServiceHelperFormatter.methodEvents((Map<String, Object>) method);
        });
        getHandlebars().registerHelper("methodAnnotations", (method, options) -> formatMethodAnnotations((Map<String, Object>) method));

        getHandlebars().registerHelper("serviceInputs", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            return ServiceHelperFormatter.serviceInputs((Map<String, Object>) service, zdlModel);
        });

        getHandlebars().registerHelper("serviceOutputs", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            return ServiceHelperFormatter.serviceOutputs((Map<String, Object>) service, zdlModel);
        });

        getHandlebars().registerHelper("serviceEvents", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            return ServiceHelperFormatter.serviceEvents((Map<String, Object>) service, zdlModel);
        });


        // ==================== State Machine Diagrams ====================
        // These helpers return the inner PlantUML script (including @startuml/@enduml)
        // or an empty string if there is no lifecycle/transition information.

        getHandlebars().registerHelper("aggregateLifecycleStateMachine", (aggregateOrName, options) -> {
            var zdlModel = (Map) options.context.get("zdlModel", true);
            Map<String, Object> aggregate = resolveAggregate(zdlModel, aggregateOrName);
            if (aggregate == null) {
                return "";
            }
            Map<String, Object> entity = resolveAggregateRootEntity(zdlModel, aggregate);
            var diagram = buildAggregateLifecyclePlantUml(aggregate, entity);
            return diagram == null ? "" : diagram;
        });

        getHandlebars().registerHelper("entityServiceLifecycleStateMachine", (service, options) -> {
            var entity = (Map<String, Object>) options.param(0);
            if (!(service instanceof Map) || entity == null) {
                return "";
            }
            var diagram = buildEntityServiceLifecyclePlantUml((Map<String, Object>) service, entity);
            return diagram == null ? "" : diagram;
        });
    }


    // ==================== PlantUML Lifecycle Diagram Builders ====================

    private static Map<String, Object> resolveAggregate(Map<String, Object> zdlModel, Object aggregateOrName) {
        return DiagramModelResolver.resolveAggregate(zdlModel, aggregateOrName);
    }

    private static boolean isNamedAggregate(Map<String, Object> aggregate) {
        return DiagramModelResolver.isNamedAggregate(aggregate);
    }

    private static Map<String, Object> resolveAggregateRootEntity(Map<String, Object> zdlModel, Map<String, Object> aggregate) {
        return DiagramModelResolver.resolveAggregateRootEntity(zdlModel, aggregate);
    }

    private static Map<String, Object> resolveDiagramEntity(Map<String, Object> zdlModel, Object aggregateOrEntityName) {
        return DiagramModelResolver.resolveDiagramEntity(zdlModel, aggregateOrEntityName);
    }

    private static List<Map<String, Object>> collectEntityAssociations(Map<String, Object> entity, Map<String, Object> zdlModel) {
        return AssociationCollector.collectEntityAssociations(entity, zdlModel);
    }

    private static List<Map<String, Object>> collectCollectionAssociations(Collection<Map<String, Object>> entities, Map<String, Object> zdlModel) {
        return AssociationCollector.collectCollectionAssociations(entities, zdlModel);
    }

    private static String sanitizeStateId(String state) {
        return LifecycleDiagramBuilder.sanitizeStateId(state);
    }

    private static String transitionLabel(Map<String, Object> methodOrCommand) {
        return LifecycleDiagramBuilder.transitionLabel(methodOrCommand);
    }

    private static List<String> formatMethodAnnotations(Map<String, Object> method) {
        return ServiceHelperFormatter.formatMethodAnnotations(method);
    }

    private static List<Map<String, Object>> uniqueAssociationTargets(Collection<Map<String, Object>> associations, Object excludedNames) {
        return AssociationCollector.uniqueAssociationTargets(associations, excludedNames);
    }

    private static String buildAggregateLifecyclePlantUml(Map<String, Object> aggregate, Map<String, Object> rootEntity) {
        return LifecycleDiagramBuilder.buildAggregateLifecyclePlantUml(aggregate, rootEntity);
    }

    private static String buildEntityServiceLifecyclePlantUml(Map<String, Object> service, Map<String, Object> entity) {
        return LifecycleDiagramBuilder.buildEntityServiceLifecyclePlantUml(service, entity);
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        return getTemplateEngine().processTemplate(model, template);
    }
}
