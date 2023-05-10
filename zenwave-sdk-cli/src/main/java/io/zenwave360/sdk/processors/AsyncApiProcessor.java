package io.zenwave360.sdk.processors;

import java.util.*;

import com.jayway.jsonpath.JsonPath;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;

public class AsyncApiProcessor extends AbstractBaseProcessor implements Processor {

    public enum SchemaFormatType {

        ASYNCAPI("application/vnd.aai.asyncapi;"), ASYNCAPI_JSON("application/vnd.aai.asyncapi+json;"), ASYNCAPI_YAML("application/vnd.aai.asyncapi+yaml;"), OPENAPI("application/vnd.oai.openapi;"), OPENAPI_JSON("application/vnd.oai.openapi+json;"), OPENAPI_YAML("application/vnd.oai.openapi+yaml;"), JSONSCHEMA_JSON("application/schema+json;"), JSONSCHEMA_YAML("application/schema+yaml;"), AVRO("application/vnd.apache.avro;"), AVRO_JSON("application/vnd.apache.avro+json;"), AVRO_YAML("application/vnd.apache.avro+yaml;"),
        ;

        private static final List<SchemaFormatType> ASYNCAPI_ALL = Arrays.asList(ASYNCAPI, ASYNCAPI_JSON, ASYNCAPI_YAML);
        private static final List<SchemaFormatType> OPENAPI_ALL = Arrays.asList(OPENAPI, OPENAPI_JSON, OPENAPI_YAML);
        private static final List<SchemaFormatType> JSONSCHEMA_ALL = Arrays.asList(JSONSCHEMA_JSON, JSONSCHEMA_YAML);
        private static final List<SchemaFormatType> AVRO_ALL = Arrays.asList(AVRO, AVRO_JSON, AVRO_YAML);

        private static final List<SchemaFormatType> YAML_ALL = Arrays.asList(ASYNCAPI_YAML, OPENAPI_YAML, JSONSCHEMA_YAML, AVRO_YAML);

        private String schemaFormatPrefix;

        private SchemaFormatType(String regex) {
            this.schemaFormatPrefix = regex;
        }

        public static boolean isSchemaFormat(SchemaFormatType formatType) {
            return formatType == null || ASYNCAPI_ALL.contains(formatType) || OPENAPI_ALL.contains(formatType);
        }

        public static boolean isJsonSchemaFormat(SchemaFormatType formatType) {
            return JSONSCHEMA_ALL.contains(formatType);
        }

        public static boolean isAvroFormat(SchemaFormatType formatType) {
            return AVRO_ALL.contains(formatType);
        }

        public static boolean isNativeFormat(SchemaFormatType formatType) {
            return isSchemaFormat(formatType);
        }

        public static boolean isYamlFormat(SchemaFormatType formatType) {
            return YAML_ALL.contains(formatType);
        }

        public static SchemaFormatType getFormat(String schemaFormatString) {
            if (schemaFormatString == null) {
                return ASYNCAPI_YAML;
            }
            for (SchemaFormatType schemaFormat : SchemaFormatType.values()) {
                if (schemaFormatString.startsWith(schemaFormat.schemaFormatPrefix)) {
                    return schemaFormat;
                }
            }
            return null;
        }

    }

    @DocumentedOption(description = "AsyncAPI extension property name for runtime autoconfiguration of headers.")
    public String runtimeHeadersProperty = "x-runtime-expression";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Model apiModel = targetProperty != null ? (Model) contextModel.get(targetProperty) : (Model) contextModel;
        boolean isV2 = AsyncAPIUtils.isV2(apiModel);
        boolean isV3 = AsyncAPIUtils.isV3(apiModel);

        apiModel.getRefs().getOriginalRefsList().forEach(pair -> {
            if (pair.getValue() instanceof Map) {
                ((Map) pair.getValue()).put("x--original-$ref", pair.getKey().getRef());
            }
        });

        List<Map<String, Map>> traitsParents = JSONPath.get(apiModel, "$..[?(@.traits)]");
        for (Map<String, Map> traitParent : traitsParents) {
            List<Map<String, Map>> traitsList = (List) traitParent.get("traits");
            // merge traits into parent
            for (Map<String, Map> traits : traitsList) {
                for (Map.Entry<String, Map> trait : traits.entrySet()) {
                    String traitName = trait.getKey();
                    if(traitName.startsWith("x--")) {
                        continue; // do not merge internal extensions
                    }
                    if(traitParent.containsKey(traitName)) {
                        var merged = Maps.deepMerge(Maps.copy(trait.getValue()), traitParent.get(traitName));
                        traitParent.put(traitName, merged);
                    } else {
                        traitParent.put(traitName, trait.getValue());
                    }
                }
            }
        }


        Map<String, Object> channels = JSONPath.get(apiModel, "$.channels", Collections.emptyMap());
        for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
            Map<String, Map<String, Object>> channel = (Map) channelEntry.getValue();
            if (isV2) {
                if (channel != null) {
                    addChannelNameToOperation(channel.get("publish"), channelEntry.getKey());
                    addChannelNameToOperation(channel.get("subscribe"), channelEntry.getKey());
                    addOperationType(channel.get("publish"), "publish");
                    addOperationType(channel.get("subscribe"), "subscribe");
                    addNormalizedTagName(channel.get("publish"));
                    addNormalizedTagName(channel.get("subscribe"));
                    addOperationIdVariants(channel.get("publish"));
                    addOperationIdVariants(channel.get("subscribe"));
                    collectMessages(channel.get("publish"));
                    collectMessages(channel.get("subscribe"));
                    setHasRuntimeHeaders(channel.get("publish"));
                    setHasRuntimeHeaders(channel.get("subscribe"));
                }
            }
            if (isV3) {
                // collect channel messages
                var messages = JSONPath.get(channel, "$.messages[*]", Collections.emptyList());
                ((Map) channel).put("x--messages", messages);
            }
        }

        if(isV3) {
            var operations = JSONPath.get(apiModel, "$.operations", Collections.<String, Map>emptyMap());
            for (Map.Entry<String, Map> operationEntry : operations.entrySet()) {
                operationEntry.getValue().put("operationId", operationEntry.getKey());
                addOperationIdVariants(operationEntry.getValue());
                operationEntry.getValue().put("x--messages", JSONPath.get(operationEntry.getValue(), "$.channel.x--messages"));
            }
        }

        List<Map<String, Object>> messages = JSONPath.get(apiModel, "$.channels..x--messages[*]");
        for (Map<String, Object> message : messages) {
            calculateMessageParamType(message);
        }

        Map<String, Map> componentsMessages = JSONPath.get(apiModel, "$.components.messages", Collections.emptyMap());
        for (Map.Entry<String, Map> message : componentsMessages.entrySet()) {
            if (!message.getValue().containsKey("name")) {
                message.getValue().put("name", message.getKey());
            }
        }

        Map<String, Map> schemas = JSONPath.get(apiModel, "$.components.schemas", Collections.emptyMap());
        for (Map.Entry<String, Map> entry : schemas.entrySet()) {
            entry.getValue().put("x--schema-name", entry.getKey());
        }

        return contextModel;
    }

    private void addChannelNameToOperation(Map<String, Object> operation, String channelName) {
        if (operation != null) {
            operation.put("x--channel", channelName);
        }
    }

    private void addOperationType(Map<String, Object> operation, String operationType) {
        if (operation != null) {
            operation.put("x--operationType", operationType);
        }
    }

    private void setHasRuntimeHeaders(Map operation) {
        if(operation != null) {
            boolean hasAutoheader = !JSONPath.get(operation, String.format("$.x--messages..headers..[?(@.%s)]", runtimeHeadersProperty), Collections.emptyList()).isEmpty();
            if(hasAutoheader) {
                operation.put("x--has-runtime-headers", true);
            }
        }
    }

    public void collectMessages(Map<String, Object> operation) {
        if (operation != null) {
            Map message = (Map) operation.get("message");
            List messages = new ArrayList();
            if (message.containsKey("oneOf")) {
                messages.addAll((List) message.get("oneOf"));
            } else {
                messages.add(message);
            }
            operation.put("x--messages", messages);
        }
    }

    public void calculateMessageParamType(Map<String, Object> message) {
        String schemaFormat = normalizeSchemaFormat((String) message.get("schemaFormat"));
        String javaType = null;
        if ("avro".equals(schemaFormat)) {
            String name = JsonPath.read(message, "payload.name");
            String namespace = JsonPath.read(message, "payload.namespace");
            javaType = namespace + "." + name;
        }
        if ("jsonSchema".equals(schemaFormat)) {
            javaType = JsonPath.read(message, "payload.javaType");
        }
        if ("asyncapi".equals(schemaFormat) || "openapi".equals(schemaFormat)) {
            javaType = normalizeTagName(JSONPath.get(message, "payload.x--schema-name"));
            if (javaType == null) {
                javaType = normalizeTagName((String) message.getOrDefault("x-javaType", message.getOrDefault("messageId", message.get("name"))));
            }
        }

        if (javaType != null) {
            message.put("x--javaType", javaType);
            message.put("x--javaTypeSimpleName", javaType.substring(javaType.lastIndexOf(".") + 1));
        }
    }

    private String normalizeSchemaFormat(String schemaFormat) {
        if (schemaFormat == null) {
            return "asyncapi";
        }
        if (schemaFormat.matches("application\\/vnd\\.aai\\.asyncapi(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "asyncapi";
        }
        if (schemaFormat.matches("application\\/vnd\\.oai\\.openapi(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "openapi";
        }
        if (schemaFormat.matches("application\\/schema(\\+json|\\+yaml)*;version=draft-\\d+")) {
            return "jsonSchema";
        }
        if (schemaFormat.matches("application\\/vnd\\.apache\\.avro(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "avro";
        }
        return null;
    }
}