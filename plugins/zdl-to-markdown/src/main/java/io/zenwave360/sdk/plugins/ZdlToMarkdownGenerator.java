package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;

public class ZdlToMarkdownGenerator extends AbstractZDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "zdl";

    public enum OutputFormat {
        glossary, task_list, aggregate
    }

    @DocumentedOption(description = "Template type")
    public OutputFormat outputFormat = OutputFormat.glossary;

    @DocumentedOption(description = "Aggregate name")
    public String aggregateName;

    @DocumentedOption(description = "Skip generating PlantUML diagrams")
    public boolean skipDiagrams = false;

    @DocumentedOption(description = "Target file")
    public String targetFile = "zdl-glossary.md";

    private final HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput modelGlossaryTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownGenerator.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelTaskListTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownTaskList.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelAggregateTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownAggregate.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);

    public ZdlToMarkdownGenerator withOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    public ZdlToMarkdownGenerator withSkipDiagrams(boolean skipDiagrams) {
        this.skipDiagrams = skipDiagrams;
        return this;
    }

    public ZdlToMarkdownGenerator withAggregateName(String aggregateName) {
        this.aggregateName = aggregateName;
        return this;
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Map<String, Object> zdlModel = (Map) contextModel.get(sourceProperty);

        if(outputFormat == OutputFormat.aggregate) {

            targetFile = "zdl-aggregate-" + aggregateName + ".md";
            var aggregate = (Map) JSONPath.get(zdlModel, "$.aggregates." + aggregateName);
            var entity = (Map) JSONPath.get(zdlModel, "$.entities." + aggregateName);
            if (entity == null) {
                entity = (Map) JSONPath.get(zdlModel, "$.entities." + aggregate.get("aggregateRoot"));
            }
            zdlModel.put("aggregate", aggregate);
            zdlModel.put("entity", entity);
            return List.of(generateTemplateOutput(contextModel, modelAggregateTemplate, zdlModel));

        } else {

            var template = outputFormat == OutputFormat.glossary ? modelGlossaryTemplate : modelTaskListTemplate;
            targetFile = outputFormat == OutputFormat.glossary ? "zdl-glossary.md" : "zdl-task-list.md";

            return List.of(generateTemplateOutput(contextModel, template, zdlModel));
        }
    }

    private final Map<String, String> inverseRelationshipTypes = Map.of("OneToMany", "ManyToOne","ManyToOne", "OneToMany","ManyToMany", "ManyToMany", "OneToOne", "OneToOne");
    {
        handlebarsEngine.getHandlebars().registerHelper("entityAssociations", (entity, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            var compositions = JSONPath.get(entity, "fields[*][?(@.isEntity==true || @.isEnum==true)].type", List.of());
            var associations = JSONPath.get(entity, "relationships[?(@.fieldName)].otherEntityName", List.of());
            var entityAssociations = new ArrayList<Map>();
            compositions.stream().map(name -> Maps.of("linkType", "*--", "entity", JSONPath.get(zdlModel, "$.entities." + name))).forEach(entityAssociations::add);
            compositions.stream().map(name -> Maps.of("linkType", "*--", "entity", JSONPath.get(zdlModel, "$.enums." + name))).forEach(entityAssociations::add);
            associations.stream().map(name -> Maps.of("linkType", "o--", "entity", JSONPath.get(zdlModel, "$.entities." + name))).forEach(entityAssociations::add);
            return entityAssociations.stream().filter(e -> e.get("entity") != null).collect(Collectors.toList());
        });
        handlebarsEngine.getHandlebars().registerHelper("relationshipType", (relationship, options) -> {
            boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
            var type = JSONPath.get(relationship, "type");
            return isOwnerSide ? type : inverseRelationshipTypes.get(type);
        });
        handlebarsEngine.getHandlebars().registerHelper("methodParamsSignature", (method, options) -> {
            var params = new ArrayList<>();
            if(JSONPath.get(method, "paramId") != null) {
                params.add("id");
            }
            if(JSONPath.get(method, "parameter") != null) {
                params.add(JSONPath.get(method, "parameter"));
            }
            return StringUtils.join(params, ", ");
        });

        handlebarsEngine.getHandlebars().registerHelper("methodReturnType", (method, options) -> {
            var returnType = JSONPath.get(method, "returnType", "");
            if(JSONPath.get(method, "returnTypeIsArray", false)) {
                returnType = returnType + "[]";
            }
            if(JSONPath.get(method, "returnTypeIsOptional", false)) {
                returnType = returnType + "?";
            }
            return returnType;
        });

        handlebarsEngine.getHandlebars().registerHelper("methodEvents", (method, options) -> {
            var events = JSONPath.get(method, "withEvents", List.of());
            return StringUtils.join(events, " ").replaceAll(", ", " | ");
        });

        handlebarsEngine.getHandlebars().registerHelper("serviceInputs", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            var inputs = new LinkedHashSet<>();
            var methods = JSONPath.get(service, "methods", Map.of());
            for (Object value : methods.values()) {
                var method = (Map) value;
                if(JSONPath.get(method, "parameter") != null) {
                    inputs.add(JSONPath.get(method, "parameter"));
                }
            }
            return inputs.stream().map(input -> {
                var entity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + input);
                if(entity != null && !"entities".equals(JSONPath.get(entity, "type"))) {
                    return entity;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        });

        handlebarsEngine.getHandlebars().registerHelper("serviceOutputs", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            var outputs = new LinkedHashSet<>();
            var methods = JSONPath.get(service, "methods", Map.of());
            for (Object method : methods.values()) {
                outputs.add(JSONPath.get(method, "returnType"));
            }
            return outputs.stream().map(input -> {
                var entity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + input);
                if(entity != null && !"entities".equals(JSONPath.get(entity, "type"))) {
                    return entity;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        });

        handlebarsEngine.getHandlebars().registerHelper("serviceEvents", (service, options) -> {
            var zdlModel = options.context.get("zdlModel", true);
            var events = new LinkedHashSet<>();
            var methods = JSONPath.get(service, "methods", Map.of());
            for (Object method : methods.values()) {
                var methodEvents = JSONPath.get(method, "withEvents", List.of());
                for (Object methodEvent : methodEvents) {
                    if(methodEvent instanceof List) {
                        events.addAll((List) methodEvent);
                    } else {
                        events.add(methodEvent);
                    }
                }
            }

            return events.stream().map(event -> JSONPath.get(zdlModel, "$.events." + event))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        });
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }
}
