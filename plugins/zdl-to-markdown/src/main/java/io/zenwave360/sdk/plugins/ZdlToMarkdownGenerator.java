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
        glossary, task_list, aggregate, plantuml
    }

    @DocumentedOption(description = "Template type")
    public OutputFormat outputFormat = OutputFormat.glossary;

    @DocumentedOption(description = "Aggregate name")
    public String aggregateName;

    @DocumentedOption(description = "Skip generating PlantUML diagrams")
    public boolean skipDiagrams = false;

    @DocumentedOption(description = "Target file")
    public String targetFile = "zdl-glossary.md";

    private Handlebars getHandlebars() {
        return ((HandlebarsEngine) getTemplateEngine()).getHandlebars();
    }

    private final TemplateInput modelGlossaryTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownGlossary.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelTaskListTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownTaskList.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelAggregateTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownAggregate.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);
    private final TemplateInput modelUmlTemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownPlantUML.md", "{{targetFile}}").withMimeType(OutputFormatType.MARKDOWN);

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
                case task_list -> {
                    targetFile = "zdl-task-list.md";
                    yield modelTaskListTemplate;
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
            var params = new ArrayList<>();
            if(JSONPath.get(method, "paramId") != null) {
                params.add("id");
            }
            if(JSONPath.get(method, "parameter") != null) {
                params.add(JSONPath.get(method, "parameter"));
            }
            return StringUtils.join(params, ", ");
        });

        getHandlebars().registerHelper("methodReturnType", (method, options) -> {
            var returnType = JSONPath.get(method, "returnType", "");
            if(JSONPath.get(method, "returnTypeIsArray", false)) {
                returnType = returnType + "[]";
            }
            if(JSONPath.get(method, "returnTypeIsOptional", false)) {
                returnType = returnType + "?";
            }
            return returnType;
        });

        getHandlebars().registerHelper("methodEvents", (method, options) -> {
            var events = JSONPath.get(method, "withEvents", List.of());
            return StringUtils.join(events, " ").replaceAll(", ", " | ");
        });
        getHandlebars().registerHelper("methodAnnotations", (method, options) -> formatMethodAnnotations((Map<String, Object>) method));

        getHandlebars().registerHelper("serviceInputs", (service, options) -> {
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

        getHandlebars().registerHelper("serviceOutputs", (service, options) -> {
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

        getHandlebars().registerHelper("serviceEvents", (service, options) -> {
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

    private static final String DEFAULT_STATE_MACHINE_SKINPARAMS = String.join("\n",
            "hide empty description",
            "skinparam shadowing false",
            "skinparam BackgroundColor white",
            "skinparam ArrowColor #4B5563",
            "skinparam StateBorderColor #1F2937",
            "skinparam StateFontColor #111827",
            "skinparam StateBackgroundColor #F9FAFB",
            "skinparam StateStartColor #111827",
            "skinparam StateEndColor #111827",
            "skinparam NoteBackgroundColor #FFFBEA",
            "skinparam NoteBorderColor #D6B656",
            "skinparam LegendBackgroundColor #F8FAFC",
            "skinparam LegendBorderColor #CBD5E1",
            "skinparam roundcorner 12"
    );

    private static Map<String, Object> resolveAggregate(Map<String, Object> zdlModel, Object aggregateOrName) {
        if (aggregateOrName == null) {
            return null;
        }
        if (aggregateOrName instanceof Map) {
            var aggregate = (Map<String, Object>) aggregateOrName;
            return isNamedAggregate(aggregate) ? aggregate : null;
        }
        if (aggregateOrName instanceof String name && zdlModel != null) {
            var aggregate = (Map<String, Object>) JSONPath.get(zdlModel, "$.aggregates." + name);
            return isNamedAggregate(aggregate) ? aggregate : null;
        }
        return null;
    }

    private static boolean isNamedAggregate(Map<String, Object> aggregate) {
        if (aggregate == null) {
            return false;
        }
        String name = JSONPath.get(aggregate, "$.name", (String) null);
        String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", (String) null);
        return StringUtils.isNotBlank(name) && StringUtils.isNotBlank(aggregateRoot);
    }

    private static Map<String, Object> resolveAggregateRootEntity(Map<String, Object> zdlModel, Map<String, Object> aggregate) {
        if (zdlModel == null || aggregate == null) {
            return null;
        }
        String aggregateRoot = (String) aggregate.get("aggregateRoot");
        String aggregateName = (String) aggregate.get("name");
        String entityName = aggregateRoot != null ? aggregateRoot : aggregateName;
        if (entityName == null) {
            return null;
        }
        var entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + entityName);
        if (entity == null) {
            entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + entityName);
        }
        return entity;
    }

    private static Map<String, Object> resolveDiagramEntity(Map<String, Object> zdlModel, Object aggregateOrEntityName) {
        if (zdlModel == null || aggregateOrEntityName == null) {
            return null;
        }
        Map<String, Object> aggregate = resolveAggregate(zdlModel, aggregateOrEntityName);
        if (aggregate != null) {
            return resolveAggregateRootEntity(zdlModel, aggregate);
        }
        return (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + aggregateOrEntityName);
    }

    private static Map<String, Object> lifecycleOf(Map<String, Object> model) {
        if (model == null) {
            return null;
        }
        Map<String, Object> lifecycle = (Map<String, Object>) JSONPath.get(model, "$.lifecycle");
        if (lifecycle == null) {
            lifecycle = (Map<String, Object>) JSONPath.get(model, "$.options.lifecycle");
        }
        return lifecycle;
    }

    private static List<Map<String, Object>> commandsOf(Map<String, Object> aggregateOrEntity) {
        if (aggregateOrEntity == null) {
            return List.of();
        }
        // commands might be a Map; JSONPath '$.commands[*]' works for both Map and List.
        return (List<Map<String, Object>>) JSONPath.get(aggregateOrEntity, "$.commands[*]", List.<Map<String, Object>>of());
    }

    private static List<Map<String, Object>> collectEntityAssociations(Map<String, Object> entity, Map<String, Object> zdlModel) {
        if (entity == null || zdlModel == null) {
            return List.of();
        }
        var associations = new ArrayList<Map<String, Object>>();
        var visitedEntities = new LinkedHashSet<String>();
        var visitedLinks = new LinkedHashSet<String>();
        collectEntityAssociations(entity, zdlModel, associations, visitedEntities, visitedLinks);
        return associations;
    }

    private static List<Map<String, Object>> collectCollectionAssociations(Collection<Map<String, Object>> entities, Map<String, Object> zdlModel) {
        if (entities == null || zdlModel == null) {
            return List.of();
        }
        var associations = new ArrayList<Map<String, Object>>();
        var visitedEntities = new LinkedHashSet<String>();
        var visitedLinks = new LinkedHashSet<String>();
        for (var entity : entities) {
            collectEntityAssociations(entity, zdlModel, associations, visitedEntities, visitedLinks);
        }
        return associations;
    }

    private static void collectEntityAssociations(Map<String, Object> entity,
                                                  Map<String, Object> zdlModel,
                                                  List<Map<String, Object>> associations,
                                                  Set<String> visitedEntities,
                                                  Set<String> visitedLinks) {
        if (entity == null) {
            return;
        }
        String sourceName = JSONPath.get(entity, "$.name", (String) null);
        if (sourceName == null || !visitedEntities.add(sourceName)) {
            return;
        }

        var compositions = JSONPath.get(entity, "fields[*][?(@.isEntity==true || @.isEnum==true)].type", List.<String>of());
        for (String targetName : compositions) {
            Map<String, Object> target = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + targetName);
            addAssociation(sourceName, "*--", target, associations, visitedLinks);
            if (target != null && !"enums".equals(JSONPath.get(target, "type", ""))) {
                collectEntityAssociations(target, zdlModel, associations, visitedEntities, visitedLinks);
            }
        }

        String sourceBoundary = aggregateBoundaryName(zdlModel, sourceName);
        var relationships = JSONPath.get(entity, "relationships[?(@.fieldName)]", List.<Map<String, Object>>of());
        for (Map<String, Object> relationship : relationships) {
            if (!Boolean.TRUE.equals(JSONPath.get(relationship, "$.ownerSide", false))) {
                continue;
            }
            String targetName = JSONPath.get(relationship, "$.otherEntityName", (String) null);
            if (targetName == null) {
                continue;
            }
            Map<String, Object> target = JSONPath.get(zdlModel, "$.entities." + targetName);
            String targetBoundary = aggregateBoundaryName(zdlModel, targetName);
            boolean sameBoundary = Objects.equals(sourceBoundary, targetBoundary);
            addAssociation(sourceName, sameBoundary ? "o--" : "..>", target, associations, visitedLinks);
            if (sameBoundary) {
                collectEntityAssociations(target, zdlModel, associations, visitedEntities, visitedLinks);
            }
        }

    }

    private static String aggregateBoundaryName(Map<String, Object> zdlModel, String entityName) {
        if (zdlModel == null || entityName == null) {
            return null;
        }
        if (Boolean.TRUE.equals(JSONPath.get(zdlModel, "$.entities." + entityName + ".options.aggregate", false))) {
            return entityName;
        }
        for (String aggregateRoot : aggregateBoundaryRoots(zdlModel)) {
            if (isEntityWithinAggregateBoundary(zdlModel, aggregateRoot, entityName, new LinkedHashSet<>())) {
                return aggregateRoot;
            }
        }
        return null;
    }

    private static List<String> aggregateBoundaryRoots(Map<String, Object> zdlModel) {
        var roots = new LinkedHashSet<String>();
        var aggregates = (Map<String, Object>) JSONPath.get(zdlModel, "$.aggregates", Map.<String, Object>of());
        for (Object value : aggregates.values()) {
            if (!(value instanceof Map<?, ?> aggregate)) {
                continue;
            }
            String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", (String) null);
            if (aggregateRoot != null) {
                roots.add(aggregateRoot);
            }
        }
        var entities = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities", Map.<String, Object>of());
        for (Object value : entities.values()) {
            if (!(value instanceof Map<?, ?> entity)) {
                continue;
            }
            if (Boolean.TRUE.equals(JSONPath.get(entity, "$.options.aggregate", false))) {
                String entityName = JSONPath.get(entity, "$.name", (String) null);
                if (entityName != null) {
                    roots.add(entityName);
                }
            }
        }
        return new ArrayList<>(roots);
    }

    private static boolean isEntityWithinAggregateBoundary(Map<String, Object> zdlModel,
                                                           String currentEntityName,
                                                           String targetEntityName,
                                                           Set<String> visited) {
        if (zdlModel == null || currentEntityName == null || targetEntityName == null || !visited.add(currentEntityName)) {
            return false;
        }
        if (currentEntityName.equals(targetEntityName)) {
            return true;
        }
        var entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + currentEntityName);
        if (entity == null) {
            return false;
        }
        var nestedTypes = new LinkedHashSet<String>();
        nestedTypes.addAll(JSONPath.get(entity, "fields[*][?(@.isEntity==true)].type", List.<String>of()));
        nestedTypes.addAll(JSONPath.get(entity, "relationships[?(@.fieldName)].otherEntityName", List.<String>of()));
        for (String nestedType : nestedTypes) {
            if (targetEntityName.equals(nestedType)) {
                return true;
            }
            if (Boolean.TRUE.equals(JSONPath.get(zdlModel, "$.entities." + nestedType + ".options.aggregate", false))) {
                continue;
            }
            if (isEntityWithinAggregateBoundary(zdlModel, nestedType, targetEntityName, visited)) {
                return true;
            }
        }
        return false;
    }

    private static void addAssociation(String sourceName,
                                       String linkType,
                                       Map<String, Object> target,
                                       List<Map<String, Object>> associations,
                                       Set<String> visitedLinks) {
        if (target == null) {
            return;
        }
        String targetName = JSONPath.get(target, "$.name", (String) null);
        if (targetName == null) {
            return;
        }
        String key = sourceName + "|" + linkType + "|" + targetName;
        if (visitedLinks.add(key)) {
            associations.add(Map.of("source", sourceName, "linkType", linkType, "entity", target));
        }
    }

    private static Object transitionFrom(Map<String, Object> methodOrCommand) {
        if (methodOrCommand == null) {
            return null;
        }
        Object from = JSONPath.get(methodOrCommand, "$.transition.from");
        if (from == null) {
            from = JSONPath.get(methodOrCommand, "$.from");
        }
        return from;
    }

    private static Object transitionTo(Map<String, Object> methodOrCommand) {
        if (methodOrCommand == null) {
            return null;
        }
        Object to = JSONPath.get(methodOrCommand, "$.transition.to");
        if (to == null) {
            to = JSONPath.get(methodOrCommand, "$.to");
        }
        return to;
    }

    private static List<String> normalizeToStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List list) {
            return (List<String>) list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private static String sanitizeStateId(String state) {
        if (state == null) {
            return null;
        }
        // PlantUML identifiers: keep alphanumerics and underscores; replace others with underscore.
        String id = state.replaceAll("[^A-Za-z0-9_]", "_");
        if (id.isEmpty()) {
            return "STATE";
        }
        if (!Character.isLetter(id.charAt(0)) && id.charAt(0) != '_') {
            id = "S_" + id;
        }
        return id;
    }

    private static String methodParamLabel(Map<String, Object> methodOrCommand) {
        var params = new ArrayList<String>();
        if (methodOrCommand == null) {
            return "";
        }
        if (JSONPath.get(methodOrCommand, "$.paramId") != null) {
            params.add("id");
        }
        if (JSONPath.get(methodOrCommand, "$.parameter") != null) {
            params.add("input");
        }
        return StringUtils.join(params, ", ");
    }

    private static String transitionLabel(Map<String, Object> methodOrCommand) {
        String name = JSONPath.get(methodOrCommand, "$.name", "");
        String params = methodParamLabel(methodOrCommand);
        String signature = params.isEmpty() ? name + "()" : name + "(" + params + ")";

        List<Object> events = JSONPath.get(methodOrCommand, "$.withEvents", List.of());
        var eventNames = new ArrayList<String>();
        for (Object event : events) {
            if (event == null) {
                continue;
            }
            if (event instanceof List list) {
                for (Object inner : list) {
                    if (inner != null) {
                        eventNames.add(inner.toString());
                    }
                }
            } else {
                eventNames.add(event.toString());
            }
        }
        if (!eventNames.isEmpty()) {
            return signature + "\\n/ " + StringUtils.join(eventNames, ", ");
        }
        return signature;
    }

    private static List<String> formatMethodAnnotations(Map<String, Object> method) {
        if (method == null) {
            return List.of();
        }
        var annotations = new ArrayList<String>();
        Map<String, Object> options = JSONPath.get(method, "$.options", Map.of());
        for (var entry : options.entrySet()) {
            annotations.add(formatAnnotation(entry.getKey(), entry.getValue()));
        }
        return annotations;
    }

    private static String formatAnnotation(String name, Object value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            return "@" + name + "()";
        }
        if (value instanceof String stringValue) {
            return "@" + name + "(\"" + stringValue + "\")";
        }
        if (value instanceof Collection<?> collectionValue) {
            return "@" + name + "(" + collectionValue.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")";
        }
        if (value instanceof Map<?, ?> mapValue) {
            return "@" + name + "(" + mapValue.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + formatAnnotationValue(entry.getValue()))
                    .collect(Collectors.joining(", ")) + ")";
        }
        return "@" + name + "(" + value + ")";
    }

    private static String formatAnnotationValue(Object value) {
        if (value instanceof Collection<?> collectionValue) {
            return "[" + collectionValue.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
        }
        if (value instanceof Map<?, ?> mapValue) {
            return "{" + mapValue.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + formatAnnotationValue(entry.getValue()))
                    .collect(Collectors.joining(", ")) + "}";
        }
        return String.valueOf(value);
    }

    private static List<Map<String, Object>> uniqueAssociationTargets(Collection<Map<String, Object>> associations, Object excludedNames) {
        if (associations == null) {
            return List.of();
        }
        Set<String> excluded = normalizeExcludedNames(excludedNames);
        var unique = new LinkedHashMap<String, Map<String, Object>>();
        for (var association : associations) {
            if (association == null) {
                continue;
            }
            var entity = (Map<String, Object>) association.get("entity");
            String name = JSONPath.get(entity, "$.name", (String) null);
            if (name != null && !excluded.contains(name)) {
                unique.putIfAbsent(name, entity);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static Set<String> normalizeExcludedNames(Object excludedNames) {
        if (excludedNames == null) {
            return Set.of();
        }
        if (excludedNames instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of(excludedNames.toString());
    }

    private static String lifecycleInitialState(Map<String, Object> lifecycle) {
        if (lifecycle == null) {
            return null;
        }
        String initial = JSONPath.get(lifecycle, "$.initial", (String) null);
        if (initial == null) {
            // backward/alternate naming
            initial = JSONPath.get(lifecycle, "$.initialState", (String) null);
        }
        return initial;
    }

    private static String buildAggregateLifecyclePlantUml(Map<String, Object> aggregate, Map<String, Object> rootEntity) {
        if (aggregate == null) {
            return null;
        }
        var lifecycle = lifecycleOf(aggregate);
        if (lifecycle == null) {
            lifecycle = lifecycleOf(rootEntity);
        }
        if (lifecycle == null) {
            return null;
        }

        var commands = commandsOf(aggregate);
        if (commands.isEmpty() && rootEntity != null) {
            commands = commandsOf(rootEntity);
        }

        // build transitions
        record Transition(String from, String to, String label) {}
        var transitions = new ArrayList<Transition>();
        for (var cmd : commands) {
            var fromStates = normalizeToStringList(transitionFrom(cmd));
            String to = transitionTo(cmd) != null ? transitionTo(cmd).toString() : null;
            if (to == null || fromStates.isEmpty()) {
                continue;
            }
            String label = transitionLabel(cmd);
            for (String from : fromStates) {
                transitions.add(new Transition(from, to, label));
            }
        }
        if (transitions.isEmpty()) {
            return null;
        }

        String aggregateName = JSONPath.get(aggregate, "$.name", "Aggregate");
        String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", JSONPath.get(rootEntity, "$.name", ""));
        String lifecycleField = JSONPath.get(lifecycle, "$.field", "status");
        String initialState = lifecycleInitialState(lifecycle);

        var allStates = new LinkedHashSet<String>();
        if (initialState != null) allStates.add(initialState);
        transitions.forEach(t -> {
            allStates.add(t.from);
            allStates.add(t.to);
        });

        var outgoing = transitions.stream().map(t -> t.from).collect(Collectors.toSet());
        var terminalStates = allStates.stream().filter(s -> !outgoing.contains(s)).collect(Collectors.toSet());

        var sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("title ").append(aggregateName).append(" lifecycle\n\n");
        sb.append(DEFAULT_STATE_MACHINE_SKINPARAMS).append("\n\n");

        // states
        for (String state : allStates) {
            String id = sanitizeStateId(state);
            sb.append("state \"").append(state).append("\" as ").append(id);
            if (terminalStates.contains(state)) {
                sb.append(" <<terminal>>");
            }
            sb.append("\n");
        }
        sb.append("\n");

        if (initialState != null) {
            sb.append("[*] --> ").append(sanitizeStateId(initialState)).append(" : initialState\n\n");
        }

        // transitions
        for (var t : transitions) {
            sb.append(sanitizeStateId(t.from)).append(" --> ").append(sanitizeStateId(t.to))
                    .append(" : ").append(t.label).append("\n");
        }

        // note + legend
        if (initialState != null) {
            sb.append("\n");
            sb.append("note right of ").append(sanitizeStateId(initialState)).append("\n");
            sb.append("Aggregate root: ").append(aggregateRoot).append("\n");
            sb.append("Status field: ").append(lifecycleField).append("\n");
            sb.append("Initial state: ").append(initialState).append("\n");
            sb.append("end note\n\n");
        } else {
            sb.append("\n");
        }

        sb.append("legend right\n");
        sb.append("  <b>Aggregate:</b> ").append(aggregateName).append("\n");
        if (aggregateRoot != null && !aggregateRoot.isBlank()) {
            sb.append("  <b>Root:</b> ").append(aggregateRoot).append("\n");
        }
        sb.append("  <b>Label format:</b>\n");
        sb.append("  command(parameter) / emitted events\n");
        sb.append("endlegend\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    private static String buildEntityServiceLifecyclePlantUml(Map<String, Object> service, Map<String, Object> entity) {
        if (service == null || entity == null) {
            return null;
        }
        var lifecycle = lifecycleOf(entity);
        if (lifecycle == null) {
            return null;
        }

        String entityName = JSONPath.get(entity, "$.name", (String) null);
        if (entityName == null) {
            return null;
        }

        // Find service methods that target this entity and have transitions
        Map<String, Object> methodsMap = JSONPath.get(service, "$.methods", Map.of());

        record Transition(String from, String to, String label) {}
        var transitions = new ArrayList<Transition>();
        for (Object value : methodsMap.values()) {
            if (!(value instanceof Map)) continue;
            var method = (Map<String, Object>) value;
            String methodEntity = JSONPath.get(method, "$.entity", (String) null);
            if (!entityName.equals(methodEntity)) {
                continue;
            }
            var fromStates = normalizeToStringList(transitionFrom(method));
            String to = transitionTo(method) != null ? transitionTo(method).toString() : null;
            if (to == null || fromStates.isEmpty()) {
                continue;
            }
            String label = transitionLabel(method);
            for (String from : fromStates) {
                transitions.add(new Transition(from, to, label));
            }
        }

        if (transitions.isEmpty()) {
            return null;
        }

        String serviceName = JSONPath.get(service, "$.name", "Service");
        String lifecycleField = JSONPath.get(lifecycle, "$.field", "status");
        String initialState = lifecycleInitialState(lifecycle);

        var allStates = new LinkedHashSet<String>();
        if (initialState != null) allStates.add(initialState);
        transitions.forEach(t -> {
            allStates.add(t.from);
            allStates.add(t.to);
        });

        var outgoing = transitions.stream().map(t -> t.from).collect(Collectors.toSet());
        var terminalStates = allStates.stream().filter(s -> !outgoing.contains(s)).collect(Collectors.toSet());

        var sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("title ").append(entityName).append(" lifecycle (via ").append(serviceName).append(")\n\n");
        sb.append(DEFAULT_STATE_MACHINE_SKINPARAMS).append("\n\n");

        for (String state : allStates) {
            String id = sanitizeStateId(state);
            sb.append("state \"").append(state).append("\" as ").append(id);
            if (terminalStates.contains(state)) {
                sb.append(" <<terminal>>");
            }
            sb.append("\n");
        }
        sb.append("\n");

        if (initialState != null) {
            sb.append("[*] --> ").append(sanitizeStateId(initialState)).append(" : initialState\n\n");
        }

        for (var t : transitions) {
            sb.append(sanitizeStateId(t.from)).append(" --> ").append(sanitizeStateId(t.to))
                    .append(" : ").append(t.label).append("\n");
        }

        if (initialState != null) {
            sb.append("\n");
            sb.append("note right of ").append(sanitizeStateId(initialState)).append("\n");
            sb.append("Entity: ").append(entityName).append("\n");
            sb.append("Service: ").append(serviceName).append("\n");
            sb.append("Status field: ").append(lifecycleField).append("\n");
            sb.append("Initial state: ").append(initialState).append("\n");
            sb.append("end note\n\n");
        } else {
            sb.append("\n");
        }

        sb.append("legend right\n");
        sb.append("  <b>Entity:</b> ").append(entityName).append("\n");
        sb.append("  <b>Service:</b> ").append(serviceName).append("\n");
        sb.append("  <b>Label format:</b>\n");
        sb.append("  method(params) / emitted events\n");
        sb.append("endlegend\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        return getTemplateEngine().processTemplate(model, template);
    }
}
