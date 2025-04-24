package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.generators.EntitiesToAvroConverter;
import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.processors.YamlOverlyMerger;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class ZDLToAsyncAPIGenerator extends AbstractZDLGenerator {

    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper jsonMapper = new ObjectMapper();

    public enum SchemaFormat {
        schema, avro
    }

    public String sourceProperty = "zdl";

    @DocumentedOption(description = "Target AsyncAPI version.")
    public AsyncapiVersionType asyncapiVersion = AsyncapiVersionType.v3;

    @DocumentedOption(description = "Target file")
    public String targetFile;

    @DocumentedOption(description = "AsyncAPI file to be merged on top of generated AsyncAPI file")
    public String asyncapiMergeFile;

    @DocumentedOption(description = "Overlay Spec file to apply on top of generated AsyncAPI file")
    public List<String> asyncapiOverlayFiles;

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

    @DocumentedOption(description = "Include Kafka common headers (kafka_messageKey)")
    public boolean includeKafkaCommonHeaders = false;

    @DocumentedOption(description = "Include CloudEvents headers (ce-*)")
    public boolean includeCloudEventsHeaders = false;


    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    {
        handlebarsEngine.getHandlebars().registerHelper("firstItem", (context, options) -> {
            if (context instanceof List) {
                return ((List) context).get(0);
            }
            return context;
        });
    }

    private final TemplateInput zdlToAsyncAPITemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZDLToAsyncAPIGenerator/ZDLToAsyncAPI{{asyncapiVersion}}.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }


    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        Map<String, Object> model = getModel(contextModel);
        List<String> serviceNames = JSONPath.get(model, "$.services[*].name");
        model.put("serviceNames", serviceNames);

        Map<String, Object> channels = new LinkedHashMap<>();
        model.put("channels", channels);
        Map<String, Object> operations = new LinkedHashMap<>();
        model.put("operations", operations);

        // input commands
        var messages = new LinkedHashMap<>();
        model.put("messages", messages);
        var methodsWithCommands = JSONPath.get(model, "$.services[*].methods[*][?(@.options.asyncapi)]", Collections.<Map>emptyList()).stream().filter(method -> {
            var api = JSONPath.get(method, "$.options.asyncapi.api", (String) null);
            var role = JSONPath.get(model, "$.apis." + api + ".role");
            return role == null || "provider".equals(role);
        }).toList();
        for (Map<String, Object> method : methodsWithCommands) {
            if (AsyncapiVersionType.v3.equals(asyncapiVersion)) {
                buildMethodCommand(method, channels, operations, model, messages);
            }
            if (AsyncapiVersionType.v2.equals(asyncapiVersion)) {
                buildCommandChannelV2(method, channels, model, messages);
            }
        }
        addAllEventsAsMessages(messages, JSONPath.get(model, "$.events", Map.of()));

        var methodsWithEvents = ZDLFindUtils.methodsWithEvents(model);
        for (Map<String, Object> method : methodsWithEvents) {
            var withEvents = ZDLFindUtils.methodEventsFlatList(method);
            for (int i = 0; i < withEvents.size(); i++) {
                String withEvent = (String) withEvents.get(i);
                var event = JSONPath.get(model, "$.events['" + withEvent + "']", Map.<String, Object>of());
                if (AsyncapiVersionType.v2.equals(asyncapiVersion)) {
                    buildEventChannelV2(method, event, channels);
                }
                if (AsyncapiVersionType.v3.equals(asyncapiVersion)) {
                    buildEventChannel(event, channels);
                    buildEventOperation(method, withEvent, model, operations);
                }

            }
        }

        // schemas
        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        for (Map<String, Object> schema : filterSchemasToInclude(model, methodsWithCommands)) {
            if (schemaFormat == SchemaFormat.schema) {
                EntitiesToSchemasConverter toSchemasConverter = new EntitiesToSchemasConverter().withIdType(idType, idTypeFormat);
                toSchemasConverter.includeVersion = false;
                String entityName = (String) schema.get("name");
                Map<String, Object> asyncAPISchema = toSchemasConverter.convertToSchema(schema, model);
                schemas.put(entityName, asyncAPISchema);
            }
            if (schemaFormat == SchemaFormat.avro) {
                EntitiesToAvroConverter toAvroConverter = new EntitiesToAvroConverter().withIdType(idType).withNamespace(avroPackage);
                generatedProjectFiles.singleFiles.addAll(convertToAvro(toAvroConverter, schema, model));
            }
        }

        String asyncAPISchemasString = "";
        if (schemaFormat == SchemaFormat.schema) {
            asyncAPISchemasString = writeAsString(yamlMapper, oasSchemas);
            // remove first line
            asyncAPISchemasString = asyncAPISchemasString.substring(asyncAPISchemasString.indexOf("components:") + 12);
        }

        var template = generateTemplateOutput(contextModel, zdlToAsyncAPITemplate, model, asyncAPISchemasString);
        var templateContent = YamlOverlyMerger.mergeAndOverlay(template.getContent(), asyncapiMergeFile, asyncapiOverlayFiles);
        template = new TemplateOutput(template.getTargetFile(), templateContent, template.getMimeType(), template.isSkipOverwrite());
        generatedProjectFiles.singleFiles.add(template);

        return generatedProjectFiles;
    }

    private void addAllEventsAsMessages(LinkedHashMap<Object, Object> allMessages, Map<String, Map> events) {
        for (Map.Entry<String, Map> event : events.entrySet()) {
            if(JSONPath.get(event.getValue(), "$.options.embedded") != null) {
                continue;
            }
            var message = new LinkedHashMap<>(event.getValue());
            message.put("summary", firstNonNull(event.getValue().get("javadoc"), event.getKey()));
            allMessages.put(event.getKey(), message);
        }
    }

    private void buildEventOperation(Map<String, Object> method, String eventName, Map<String, Object> model, Map<String, Object> operations) {
        var operationId = "on" + asJavaTypeName(eventName);
        var channelName = JSONPath.get(model, "$.events." + eventName + ".options.asyncapi.channel", eventName + "Channel");
        operations.put(operationId, Map.of("action", "send","serviceName", method.get("serviceName"), "channel", channelName));
    }

    private void buildEventChannel(Map<String, Object> event, Map<String, Object> channels) {
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

    private void buildMethodCommand(Map method, Map<String, Object> channels, Map<String, Object> operations, Map<String, Object> model, LinkedHashMap<Object, Object> messages) {
        if(isMethodCommandThirdPartyAPI(method, model)) {
            return;
        }
        var operationId = JSONPath.get(method, "$.options.asyncapi.operationId", (String) null);
        var commandName = firstNonNull(operationId, "do" + asJavaTypeName((String) method.get("name")));
        var channelName = JSONPath.get(method, "$.options.asyncapi.channel", commandName + "Channel");
        var topic = JSONPath.getFirst(method, "$.options.topic", "$.options.asyncapi.topic");

        var channel = (Map) channels.getOrDefault(channelName, Maps.of("address", "add topic here"));
        if(topic != null) {
            channel.put("address", topic);
        }

        var hasId = method.get("paramId") != null;
        var commandType = (String) method.get("parameter");
        var inputEntity = firstNonNull(JSONPath.get(model, "$.inputs['" + commandType + "']"), JSONPath.get(model, "$.entities['" + commandType + "']"));
        messages.put(commandType, inputEntity);

        var messageName = asJavaTypeName(commandType) + "Message";
        var channelMessages = Maps.of(messageName, Map.of("$ref", "#/components/messages/" + messageName));
        channel.put("messages", channelMessages);
        channels.put(channelName, channel);
        operations.put(commandName, Maps.of("action", "receive", "serviceName", method.get("serviceName"), "channel", channelName));
    }

    private boolean isMethodCommandThirdPartyAPI(Map method, Map<String, Object> model) {
        var api = JSONPath.get(method, "$.options.asyncapi.api", (String) null);
        var role = JSONPath.get(model, "$.apis." + api + ".role");
        return "client".equals(role);
    }

    private void buildCommandChannelV2(Map method, Map<String, Object> channels, Map<String, Object> model, LinkedHashMap<Object, Object> messages) {
        var operationId = JSONPath.get(method, "$.options.asyncapi.operationId", "do" + asJavaTypeName((String) method.get("name")));
        var channelName = JSONPath.get(method, "$.options.asyncapi.channel", operationId + "Channel");
        var topic = JSONPath.getFirst(method, "$.options.topic", "$.options.asyncapi.topic");

        var commandType = (String) method.get("parameter");
        var javadoc = firstNonNull(trimToNull((String) method.get("javadoc")), method.get("name"));
        var messageName = asJavaTypeName(commandType) + "Message";

        var operation = Maps.of(
                "operationId", operationId,
                "summary", javadoc,
                "serviceName", method.get("serviceName"),
                "messages", List.of("#/components/messages/" + messageName));
        var channel = (Map) Maps.getOrCreateDefault(channels, channelName, Maps.of("operations", Maps.of("publish", operation)));
        if(topic != null) {
            channel.put("x-address", topic);
        }

        var inputEntity = (Map) firstNonNull(JSONPath.get(model, "$.inputs['" + commandType + "']"), JSONPath.get(model, "$.entities['" + commandType + "']"));
        var message = new LinkedHashMap<>(inputEntity);
        message.put("summary", firstNonNull(inputEntity.get("javadoc"), inputEntity.get("name")));
        messages.put(commandType, message);
    }

    private void buildEventChannelV2(Map method, Map event, Map<String, Object> channels) {;
        var operationId = JSONPath.get(event, "$.options.asyncapi.operationId", "on" + asJavaTypeName((String) method.get("name")));
        var channelName = JSONPath.get(event, "$.options.asyncapi.channel", event.get("name") + "Channel");
        var topic = JSONPath.getFirst(event, "$.options.topic", "$.options.asyncapi.topic");
        var messageName = event.get("name") + "Message";

        var operation = Maps.of(
                "operationId", operationId,
                "summary", firstNonNull(event.get("javadoc"), event.get("name")),
                "serviceName", method.get("serviceName"),
                "messages", new ArrayList<>());
        operation = JSONPath.get(channels, "$." + channelName + ".operations.publish", operation);
        var messages = (List) JSONPath.get(operation, "$.messages");
        messages.add("#/components/messages/" + messageName);
        var channel = (Map) Maps.getOrCreateDefault(channels, channelName, Maps.of("operations", Maps.of("publish", operation)));
        if(topic != null) {
            channel.put("x-address", topic);
        }
    }

    protected List<Map<String, Object>> filterSchemasToInclude(Map<String, Object> model, List<Map> methodsWithCommands) {
        Map<String, Object> allEntitiesAndEnums = (Map) model.get("allEntitiesAndEnums");
        Map<String, Object> relationships = (Map) model.get("relationships");

        List<Map<String, Object>> schemasToInclude = new ArrayList<>();
        schemasToInclude.addAll(JSONPath.get(model, "$.events[*]", List.of()));
        JSONPath.get(methodsWithCommands, "$.[*].parameter", List.of()).forEach(parameter -> {
            var entity = JSONPath.get(allEntitiesAndEnums, "$.['" + parameter + "']", null);
            if(entity != null) {
                schemasToInclude.add((Map) entity);
            }
        });

        Set<String> includeNames = new HashSet<>();
        for (Map<String, Object> schema : schemasToInclude) {
            addReferencedTypeToIncludeNames(schema, allEntitiesAndEnums, includeNames);
        }

        for (String includeName : new ArrayList<>(includeNames)) {
            Map<String, Object> entity = (Map) allEntitiesAndEnums.get(includeName);
            if (entity != null) {
                addRelationshipTypeToIncludeNames(entity, allEntitiesAndEnums, relationships, includeNames);
            }
        }

        List<Map<String, Object>> schemasToIncludeList = new ArrayList<>(schemasToInclude);
        for (String includeName : includeNames) {
            Map<String, Object> entity = (Map) allEntitiesAndEnums.get(includeName);
            if (entity != null) {
                schemasToIncludeList.add(entity);
            }
        }

        return schemasToIncludeList;
    }

    private void addRelationshipTypeToIncludeNames(Map<String, Object> entity, Map<String, Object> entitiesMap, Map<String, Object> relationships, Set<String> includeNames) {
        var entityName = entity.get("name");
        var relatedTypes = new HashSet<String>(JSONPath.get(relationships, "$..[?(@.from == '" + entityName + "')].to", List.of()));
        for (String fieldType : relatedTypes) {
            if (entitiesMap.containsKey(fieldType) && !includeNames.contains(fieldType)) {
                includeNames.add(fieldType);
                addReferencedTypeToIncludeNames((Map) entitiesMap.get(fieldType), entitiesMap, includeNames);
            }
        }
    }

    protected void addReferencedTypeToIncludeNames(Map<String, Object> entity, Map<String, Object> entitiesMap, Set<String> includeNames) {
        var fieldTypes = new HashSet<String>(JSONPath.get(entity, "$.fields[*].type", List.of()));
        for (String fieldType : fieldTypes) {
            if (entitiesMap.containsKey(fieldType) && !includeNames.contains(fieldType)) {
                includeNames.add(fieldType);
                addReferencedTypeToIncludeNames((Map) entitiesMap.get(fieldType), entitiesMap, includeNames);
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

    protected List<TemplateOutput> convertToAvro(EntitiesToAvroConverter converter, Map<String, Object> entityOrEnum, Map<String, Object> zdlModel) {
        String name = (String) entityOrEnum.get("name");
        Map avro = converter.convertToAvro(entityOrEnum, zdlModel);
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
        model.put("isDefaultSchemaFormat", schemaFormat == SchemaFormat.schema);
        model.put("schemaFormatString", schemaFormat == SchemaFormat.schema ? defaultSchemaFormat : avroSchemaFormat);
        model.put("schemasAsString", schemasAsString);
        return handlebarsEngine.processTemplate(model, template);
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
