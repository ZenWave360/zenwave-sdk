package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.generators.JDLEntitiesToAvroConverter;
import io.zenwave360.sdk.generators.JDLEntitiesToSchemasConverter;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import org.apache.commons.lang3.ObjectUtils;

import static io.zenwave360.sdk.utils.NamingUtils.asInstanceName;
import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;

public class ZDLToAsyncAPIGenerator extends AbstractZDLGenerator {

    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper jsonMapper = new ObjectMapper();

    enum SchemaFormat {
        schema, avro
    }

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Include channels and messages to publish domain events")
    public boolean includeEvents = true;

    @DocumentedOption(description = "Include channels and messages to listen for async command requests")
    public boolean includeCommands = false;

    @DocumentedOption(description = "Target file")
    public String targetFile = "asyncapi.yml";
    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Schema format for messages' payload")
    public SchemaFormat schemaFormat = SchemaFormat.schema;

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    @DocumentedOption(description = "Package name for generated Avro Schemas (.avsc)")
    public String avroPackage = "io.example.domain.model";

    public String defaultSchemaFormat = "application/vnd.aai.asyncapi;version=3.0.0";
    public String avroSchemaFormat = "application/vnd.apache.avro+json;version=1.9.0";


    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToAsyncAPITemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZDLToAsyncAPIGenerator/ZDLToAsyncAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }


    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> outputList = new ArrayList<>();
        Map<String, Object> model = getModel(contextModel);
        List<String> serviceNames = JSONPath.get(model, "$.services[*].name");
        model.put("serviceNames", serviceNames);

        Map<String, Object> channels = new LinkedHashMap<>();
        model.put("channels", channels);
        Map<String, Object> operations = new LinkedHashMap<>();
        model.put("operations", operations);

        // input commands
        var commands = new LinkedHashMap<>();
        model.put("commands", commands);
        var methodsWithCommands = JSONPath.get(model, "$.services[*].methods[*][?(@.options.asyncapi)]", Collections.<Map>emptyList());
        for (Map<String, Object> method : methodsWithCommands) {
            buildMethodCommand(method, channels, operations, model, commands);
        }

//        // output events
//        List<Map<String, Object>> events = (List) JSONPath.get(model, "$.events[*]");
//        for (Map<String, Object> event : events) {
//            buildEventChannel(event, channels);
//        }

        var methodsWithEvents = JSONPath.get(model, "$.services[*].methods[*][?(@.withEvents.length() > 0)]", Collections.<Map>emptyList());
        for (Map<String, Object> method : methodsWithEvents) {
            var withEvents = allEvents((List) method.getOrDefault("withEvents", List.of())); // flatten list
            for (int i = 0; i < withEvents.size(); i++) {
                String withEvent = (String) withEvents.get(i);
                var event = JSONPath.get(model, "$.events['" + withEvent + "']", Map.<String, Object>of());
                buildEventChannel(event, channels);
                buildEventOperation(method, withEvents, withEvent, model, operations);
            }
        }

        // schemas
        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        for (Map<String, Object> schema : filterSchemasToInclude(model)) {
            if (schemaFormat == SchemaFormat.schema) {
                JDLEntitiesToSchemasConverter toSchemasConverter = new JDLEntitiesToSchemasConverter().withIdType(idType, idTypeFormat).withJdlBusinessEntityProperty(jdlBusinessEntityProperty);
                toSchemasConverter.includeVersion = false;
                String entityName = (String) schema.get("name");
                Map<String, Object> asyncAPISchema = toSchemasConverter.convertToSchema(schema, model);
                schemas.put(entityName, asyncAPISchema);
            }
            if (schemaFormat == SchemaFormat.avro) {
                JDLEntitiesToAvroConverter toAvroConverter = new JDLEntitiesToAvroConverter().withIdType(idType).withNamespace(avroPackage);
                outputList.addAll(convertToAvro(toAvroConverter, schema, model));
            }
        }

        String asyncAPISchemasString = "";
        if (schemaFormat == SchemaFormat.schema) {
            asyncAPISchemasString = writeAsString(yamlMapper, oasSchemas);
            // remove first line
            asyncAPISchemasString = asyncAPISchemasString.substring(asyncAPISchemasString.indexOf("components:") + 12);
        }

        outputList.add(generateTemplateOutput(contextModel, jdlToAsyncAPITemplate, model, asyncAPISchemasString));
        return outputList;
    }

    private static void buildEventOperation(Map<String, Object> method, List<String> withEvents, String withEvent, Map<String, Object> model, Map<String, Object> operations) {
        var operationId = "on" + asJavaTypeName((String) method.get("name"));
        var operationIdSuffix = (withEvents.size() > 0? withEvent : "");
        var channelName = JSONPath.get(model, "$.events['" + withEvent + "'].channel", withEvent);
        operations.put(operationId + operationIdSuffix, Map.of("action", "send", "channel", channelName));
    }

    private static void buildEventChannel(Map<String, Object> event, Map<String, Object> channels) {
        var channelName = JSONPath.get(event, "$.options.asyncapi.channel", event.get("name") + "Channel");
        var channel = (Map) channels.getOrDefault(channelName, Maps.of("address", "add topic here"));
        var topic = JSONPath.get(event, "$.options.asyncapi.topic");
        if(topic != null) {
            channel.put("address", topic);
        }
        var messageName = event.get("name") + "Message";
        var channelMessages = (Map) channel.getOrDefault("messages", new HashMap<>());
        channelMessages.put(messageName, Maps.of("$ref", "#/components/messages/" + messageName));
        channel.put("messages", channelMessages);
        channels.put(channelName, channel);
    }

    private static void buildMethodCommand(Map method, Map<String, Object> channels, Map<String, Object> operations, Map<String, Object> model, LinkedHashMap<Object, Object> commands) {
        var operationId = JSONPath.get(method, "$.options.asyncapi.operationId", (String) null);
        var commandName = ObjectUtils.firstNonNull(operationId, "do" + asJavaTypeName((String) method.get("name")));
        var channelName = JSONPath.get(method, "$.options.asyncapi.channel", commandName + "Channel");
        var topic = JSONPath.getFirst(method, "$.options.topic", "$.options.asyncapi.topic");
        var channel = (Map) channels.getOrDefault(channelName, Maps.of("address", "add topic here"));
        if(topic != null) {
            channel.put("address", topic);
        }
        var messageName = asJavaTypeName((String) method.get("name")) + "Message";
        var channelMessages = Maps.of(messageName, Map.of("$ref", "#/components/messages/" + messageName));
        channel.put("messages", channelMessages);
        channels.put(channelName, channel);
        operations.put(commandName, Maps.of("action", "receive", "channel", channelName));

        var hasId = method.get("paramId") != null;
        var paramType = (String) method.get("parameter");
        var command = ObjectUtils.firstNonNull(JSONPath.get(model, "$.inputs['" + paramType + "']"), JSONPath.get(model, "$.entities['" + paramType + "']"));
        commands.put(paramType, command);
    }

    protected List<String> allEvents(List events) {
        List<String> allEvents = new ArrayList<>();
        for (Object event : events) {
            if(event instanceof String) {
                allEvents.add((String) event);
            } else if(event instanceof List) {
                allEvents.addAll((Collection<? extends String>) event);
            }
        }
        return allEvents;
    }

    protected List<Map<String, Object>> filterSchemasToInclude(Map<String, Object> model) {
        List<Map<String, Object>> events = new ArrayList<>();
//        events.addAll(JSONPath.get(model, "$.commands[*]", List.of()));
        events.addAll(JSONPath.get(model, "$.events[*]", List.of()));

        Map<String, Object> entitiesMap = new HashMap<>();
        entitiesMap.putAll(JSONPath.get(model, "$.entities", Collections.emptyMap()));
        entitiesMap.putAll(JSONPath.get(model, "$.enums.enums", Collections.emptyMap()));


        Set<String> includeNames = new HashSet<>();
        for (Map<String, Object> event : events) {
            if (JSONPath.get(event, "options.entity") != null) {
                String entityName = JSONPath.get(event, "options.entity");
                copyEntityFields(event, entityName, entitiesMap);
            }
            addInclude(event, entitiesMap, includeNames);
        }

        List<Map<String, Object>> schemas = new ArrayList<>(events);
        for (String includeName : includeNames) {
            Map<String, Object> entity = (Map) entitiesMap.get(includeName);
            if (entity != null) {
                schemas.add(entity);
            }
        }
        return schemas;
    }

    protected void copyEntityFields(Map<String, Object> event, String entityName, Map<String, Object> entitiesMap) {
        Map<String, Object> entity = (Map<String, Object>) entitiesMap.get(entityName);
        if(entity != null) {
            Map<String, Map> fields = JSONPath.get(event, "$.fields");
            Map<String, Map> entityFields = JSONPath.get(entity, "$.fields");
            for (var fieldEntry : entityFields.entrySet()) {
                if(!fields.containsKey(fieldEntry.getKey())) {
                    fields.put(fieldEntry.getKey(), fieldEntry.getValue());
                }
            }
        }
    }

    protected void addInclude(Map<String, Object> entity, Map<String, Object> entitiesMap, Set<String> includeNames) {
        var fieldTypes = new HashSet<String>(JSONPath.get(entity, "$.fields[*].type", List.of()));
        for (String fieldType : fieldTypes) {
            if (entitiesMap.containsKey(fieldType) && !includeNames.contains(fieldType)) {
                includeNames.add(fieldType);
                addInclude((Map) entitiesMap.get(fieldType), entitiesMap, includeNames);
            }
        }
    }

    protected String writeAsString(ObjectMapper mapper, Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getTargetAvroFolder() {
        String targetFolder = new File(targetFile).getParent();
        return targetFolder == null ? "avro" : targetFolder + "/avro";
    }

    protected List<TemplateOutput> convertToAvro(JDLEntitiesToAvroConverter converter, Map<String, Object> entityOrEnum, Map<String, Object> jdlModel) {
        String name = (String) entityOrEnum.get("name");
        Map avro = converter.convertToAvro(entityOrEnum, jdlModel);
        String avroJson = writeAsString(jsonMapper, avro);
        String targetFolder = getTargetAvroFolder();
        List<TemplateOutput> avroList = new ArrayList<>();

        avroList.add(new TemplateOutput(String.format("%s/%s.avsc", targetFolder, name), avroJson, OutputFormatType.JSON.toString()));

        return avroList;
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        model.put("schemaFormatString", schemaFormat == SchemaFormat.schema ? defaultSchemaFormat : avroSchemaFormat);
        model.put("schemasAsString", schemasAsString);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("asTagName", (context, options) -> {
            if (context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });

        handlebarsEngine.getHandlebars().registerHelper("payloadRef", (context, options) -> {
            Map entity = (Map) context;
            if (schemaFormat == SchemaFormat.avro) {
                return String.format("avro/%s.avsc", entity.get("name"));
            }
            return String.format("#/components/schemas/%s", entity.get("name"));
        });
    }
}
