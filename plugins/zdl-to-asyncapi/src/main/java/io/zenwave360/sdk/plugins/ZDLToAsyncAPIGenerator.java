package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.github.jknack.handlebars.Handlebars;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.generators.EntitiesToAvroConverter;
import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
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

    private enum MessageKind {
        COMMAND("Command"),
        EVENT("Event"),
        RESPONSE("Response");

        private final String suffix;

        MessageKind(String suffix) {
            this.suffix = suffix;
        }
    }

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

    @DocumentedOption(description = "AsyncAPI document version (info.version). Defaults to the ZDL config version, or 0.0.1")
    public String version;

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

    {
        ((HandlebarsEngine) getTemplateEngine()).getHandlebars().registerHelper("firstItem", (context, options) -> {
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

        // input commands: one binding per provider-side @asyncapi occurrence (repeatable annotation)
        var messages = new LinkedHashMap<>();
        model.put("messages", messages);
        var ownEventChannels = new HashSet<String>(JSONPath.get(model, "$.events[*].options.asyncapi.channel", List.<String>of()));
        List<AsyncApiBinding> providerBindings = providerAsyncApiBindings(model);
        Set<String> responseEventNames = new HashSet<>();
        providerBindings.forEach(binding -> responseEventNames.addAll(ZDLFindUtils.methodEventsFlatList(binding.method())));
        List<Map> methodsWithCommands = new ArrayList<>();
        for (AsyncApiBinding binding : providerBindings) {
            if (ownEventChannels.contains(binding.channel())) {
                // self-consumed: this service receives its own event channel; the channel carries only the event message(s)
                if (AsyncapiVersionType.v3.equals(asyncapiVersion)) {
                    buildSelfConsumedOperation(binding, channels, operations, model);
                }
                continue;
            }
            if (!methodsWithCommands.contains(binding.method())) {
                methodsWithCommands.add(binding.method());
            }
            if (AsyncapiVersionType.v3.equals(asyncapiVersion)) {
                buildMethodCommand(binding, channels, operations, model, messages);
            }
            if (AsyncapiVersionType.v2.equals(asyncapiVersion)) {
                buildCommandChannelV2(binding, channels, model, messages);
            }
        }
        addAllEventsAsMessages(messages, JSONPath.get(model, "$.events", Map.of()), responseEventNames);

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
                    buildEventChannel(event, channels, responseEventNames);
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
                var avroIdType = "string".equals(idType) ? "string" : "long";
                EntitiesToAvroConverter toAvroConverter = new EntitiesToAvroConverter().withIdType(avroIdType).withNamespace(avroPackage);
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
        generatedProjectFiles.singleFiles.add(template);

        return generatedProjectFiles;
    }

    private void addAllEventsAsMessages(LinkedHashMap<Object, Object> allMessages, Map<String, Map> events, Set<String> responseEventNames) {
        for (Map.Entry<String, Map> event : events.entrySet()) {
            if(JSONPath.get(event.getValue(), "$.options.embedded") != null) {
                continue;
            }
            var message = new LinkedHashMap<>(event.getValue());
            message.put("summary", firstNonNull(event.getValue().get("javadoc"), event.getKey()));
            var messageKind = responseEventNames.contains(event.getKey()) ? MessageKind.RESPONSE : MessageKind.EVENT;
            message.put("messageName", messageName(event.getKey(), messageKind));
            allMessages.put(event.getKey(), message);
        }
    }

    private String messageName(Object payloadName, MessageKind messageKind) {
        return asJavaTypeName(String.valueOf(payloadName)) + messageKind.suffix;
    }

    private void buildEventOperation(Map<String, Object> method, String eventName, Map<String, Object> model, Map<String, Object> operations) {
        var operationId = "on" + asJavaTypeName(eventName);
        var channelName = JSONPath.get(model, "$.events." + eventName + ".options.asyncapi.channel", eventName + "Channel");
        operations.put(operationId, Map.of("action", "send","serviceName", method.get("serviceName"), "channel", channelName));
    }

    private void buildEventChannel(Map<String, Object> event, Map<String, Object> channels, Set<String> responseEventNames) {
        var channelName = JSONPath.get(event, "$.options.asyncapi.channel", event.get("name") + "Channel");
        var channel = (Map) channels.getOrDefault(channelName, Maps.of("address", "add topic here"));
        var topic = JSONPath.get(event, "$.options.asyncapi.topic");
        if(topic != null) {
            channel.put("address", topic);
        }
        var eventName = (String) event.get("name");
        var messageKind = responseEventNames.contains(eventName) ? MessageKind.RESPONSE : MessageKind.EVENT;
        var messageName = messageName(eventName, messageKind);
        var channelMessages = (Map) channel.getOrDefault("messages", new HashMap<>());
        channelMessages.put(messageName, Maps.of("$ref", "#/components/messages/" + messageName));
        channel.put("messages", channelMessages);
        channels.put(channelName, channel);
    }

    /**
     * Provider-side {@code @asyncapi} occurrences: no {@code api}, or an {@code api} whose role is not {@code client}.
     * Every occurrence of the repeatable annotation is a binding (not only the last one in {@code options.asyncapi}).
     */
    private List<AsyncApiBinding> providerAsyncApiBindings(Map<String, Object> model) {
        List<AsyncApiBinding> bindings = new ArrayList<>();
        List<Map<String, Object>> methods = JSONPath.get(model, "$.services[*].methods[*]", List.of());
        for (Map<String, Object> method : methods) {
            for (Object occurrence : ZDLFindUtils.methodOptionOccurrences(method, "asyncapi")) {
                var asyncapi = occurrence instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.<String, Object>of();
                var api = (String) asyncapi.get("api");
                var role = api != null ? JSONPath.get(model, "$.apis." + api + ".role") : null;
                if (role == null || "provider".equals(role)) {
                    bindings.add(new AsyncApiBinding(method, asyncapi));
                }
            }
        }
        return bindings;
    }

    private record AsyncApiBinding(Map<String, Object> method, Map<String, Object> asyncapi) {
        String operationName(String prefix) {
            return firstNonNull((String) asyncapi.get("operationId"), prefix + asJavaTypeName((String) method.get("name")));
        }

        String channel() {
            return (String) asyncapi.get("channel");
        }
    }

    /**
     * Receive operation on an event channel this service publishes itself (private consumption, e.g. a retry loop).
     * Reuses the event message(s) of the channel: no command message is added, so the topic keeps a single payload contract.
     */
    private void buildSelfConsumedOperation(AsyncApiBinding binding, Map<String, Object> channels, Map<String, Object> operations, Map<String, Object> model) {
        var channelName = binding.channel();
        channels.computeIfAbsent(channelName, k -> Maps.of("address", "add topic here")); // keeps channel order; messages and address come from the event(s)
        operations.put(selfConsumedOperationName(binding, operations, model), Maps.of("action", "receive", "serviceName", binding.method().get("serviceName"), "channel", channelName));
    }

    /**
     * do&lt;Event&gt;, named after the first event of the channel, as the operation naming rules expect for a receive
     * of that message. An explicit operationId wins; do&lt;Method&gt; when two methods consume the same channel.
     */
    private String selfConsumedOperationName(AsyncApiBinding binding, Map<String, Object> operations, Map<String, Object> model) {
        var operationId = (String) binding.asyncapi().get("operationId");
        if (operationId != null) {
            return operationId;
        }
        Map<String, Map<String, Object>> events = JSONPath.get(model, "$.events", Map.of());
        var eventName = events.values().stream()
                .filter(event -> Objects.equals(binding.channel(), JSONPath.get(event, "$.options.asyncapi.channel")))
                .map(event -> (String) event.get("name"))
                .findFirst()
                .orElse(null);
        var name = eventName != null ? "do" + asJavaTypeName(eventName) : binding.operationName("do");
        return operations.containsKey(name) ? binding.operationName("do") : name;
    }

    private void buildMethodCommand(AsyncApiBinding binding, Map<String, Object> channels, Map<String, Object> operations, Map<String, Object> model, LinkedHashMap<Object, Object> messages) {
        var method = binding.method();
        var commandName = binding.operationName("do");
        var channelName = firstNonNull(binding.channel(), commandName + "Channel");
        var topic = firstNonNull(JSONPath.get(method, "$.options.topic"), binding.asyncapi().get("topic"));

        var channel = (Map) channels.getOrDefault(channelName, Maps.of("address", "add topic here"));
        if(topic != null) {
            channel.put("address", topic);
        }

        var hasId = method.get("paramId") != null;
        var commandType = (String) method.get("parameter");
        var inputEntity = (Map) firstNonNull(JSONPath.get(model, "$.inputs['" + commandType + "']"), JSONPath.get(model, "$.entities['" + commandType + "']"));
        var messageName = messageName(commandType, MessageKind.COMMAND);
        var message = new LinkedHashMap<>(inputEntity);
        message.put("messageName", messageName);
        messages.put(commandType, message);

        var channelMessages = (Map) channel.getOrDefault("messages", new LinkedHashMap<>());
        channelMessages.put(messageName, Map.of("$ref", "#/components/messages/" + messageName));
        channel.put("messages", channelMessages);
        channels.put(channelName, channel);
        operations.put(commandName, Maps.of("action", "receive", "serviceName", method.get("serviceName"), "channel", channelName));
    }

    private void buildCommandChannelV2(AsyncApiBinding binding, Map<String, Object> channels, Map<String, Object> model, LinkedHashMap<Object, Object> messages) {
        var method = binding.method();
        var operationId = binding.operationName("do");
        var channelName = firstNonNull(binding.channel(), operationId + "Channel");
        var topic = firstNonNull(JSONPath.get(method, "$.options.topic"), binding.asyncapi().get("topic"));

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
        model.put("apiVersion", firstNonNull(trimToNull(version), trimToNull(JSONPath.get(zdlModel, "$.config.version", (String) null)), "0.0.1"));
        model.put("isDefaultSchemaFormat", schemaFormat == SchemaFormat.schema);
        model.put("schemaFormatString", schemaFormat == SchemaFormat.schema ? defaultSchemaFormat : avroSchemaFormat);
        model.put("schemasAsString", schemasAsString);
        return getTemplateEngine().processTemplate(model, template);
    }

    private Handlebars getHandlebars() {
        return ((HandlebarsEngine) getTemplateEngine()).getHandlebars();
    }

    {
        getHandlebars().registerHelper("asTagName", (context, options) -> {
            if (context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });

        getHandlebars().registerHelper("payloadRef", (context, options) -> {
            Map entity = (Map) context;
            if (schemaFormat == SchemaFormat.avro) {
                return String.format("avro/%s.avsc", entity.get("name"));
            }
            return String.format("#/components/schemas/%s", entity.get("name"));
        });
    }
}
