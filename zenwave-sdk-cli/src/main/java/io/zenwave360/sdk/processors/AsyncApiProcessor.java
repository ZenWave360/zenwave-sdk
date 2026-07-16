package io.zenwave360.sdk.processors;

import java.util.*;

import io.zenwave360.asyncapi.AsyncApiDiagnostic;
import io.zenwave360.asyncapi.AsyncApiTraitPresets;
import io.zenwave360.asyncapi.InvalidTraitHandling;
import io.zenwave360.asyncapi.TraitsProcessor;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class AsyncApiProcessor extends AbstractBaseProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

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
            return formatType == null || ASYNCAPI_ALL.contains(formatType) || OPENAPI_ALL.contains(formatType) || JSONSCHEMA_ALL.contains(formatType);
        }

        public static boolean isJsonSchemaFormat(SchemaFormatType formatType) {
            return JSONSCHEMA_ALL.contains(formatType);
        }

        public static boolean isAvroFormat(SchemaFormatType formatType) {
            return AVRO_ALL.contains(formatType);
        }

        public static boolean isNativeFormat(SchemaFormatType formatType) {
            return formatType == null || ASYNCAPI_ALL.contains(formatType) || OPENAPI_ALL.contains(formatType);
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

        public String getSchemaFormat(String asyncApiVersion) {
            return schemaFormatPrefix + "version=" + asyncApiVersion;
        }

    }

    @DocumentedOption(description = "Sets the prefix for model classes and enums")
    public String modelNamePrefix = "";

    @DocumentedOption(description = "Sets the suffix for model classes and enums")
    public String modelNameSuffix = "";

    @DocumentedOption(description = "AsyncAPI extension property name for runtime autoconfiguration of headers.")
    public String runtimeHeadersProperty = "x-runtime-expression";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Model apiModel = targetProperty != null ? (Model) contextModel.get(targetProperty) : (Model) contextModel;
        boolean isV2 = AsyncapiVersionType.isV2(apiModel);
        boolean isV3 = AsyncapiVersionType.isV3(apiModel);

        int majorVersion = isV2 ? 2 : isV3 ? 3 : -1;
        if (majorVersion > 0) {
            List<AsyncApiDiagnostic> diagnostics = new TraitsProcessor().apply(
                    apiModel.model(), AsyncApiTraitPresets.forVersion(majorVersion), InvalidTraitHandling.COLLECT_AND_SKIP);
            for (AsyncApiDiagnostic diagnostic : diagnostics) {
                log.warn("AsyncAPI parser diagnostic [{}] at {}: {}", diagnostic.getCode(), diagnostic.getPointer(), diagnostic.getMessage());
            }
        }

        apiModel.getRefs().getOriginalRefsList().forEach(pair -> {
            if (pair.getValue() instanceof Map) {
                ((Map) pair.getValue()).put("x--original-$ref", pair.getKey().getRef());
            }
        });

        apiModel.getRefs().getReplacedRefsList().forEach(pair -> {
            if (pair.getValue() instanceof Map) {
                ((Map) pair.getValue()).put("x--original-$ref", pair.getKey().getRef());
            }
        });

        Map<String, Map> schemas = JSONPath.get(apiModel, "$.components.schemas", Collections.emptyMap());
        for (Map.Entry<String, Map> entry : schemas.entrySet()) {
            entry.getValue().put("x--schema-name", entry.getKey());
        }
        List<Map> resolvedSchemas = JSONPath.get(apiModel, "$..[?(@.x--original-$ref =~ /#\\/components\\/schemas\\/.*/)]");
        for (Map resolvedSchema : resolvedSchemas) {
            if(!resolvedSchema.containsKey("x--schema-name")) {
                String originalRef = JSONPath.get(resolvedSchema, "$.x--original-$ref");
                resolvedSchema.put("x--schema-name", originalRef.replace("#/components/schemas/", ""));
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
                    setHasRuntimeHeaders(apiModel, channel.get("publish"));
                    setHasRuntimeHeaders(apiModel, channel.get("subscribe"));
                    setDeprecatedXMessages(apiModel, channel.get("publish"));
                    setDeprecatedXMessages(apiModel, channel.get("subscribe"));
                }
            }
            if (isV3) {
                ((Map) channel).put("x--channel", channelEntry.getKey());
                setDeprecatedXMessagesForChannel(apiModel, channel, channelEntry.getKey());
            }
        }

        if(isV3) {
            var operations = JSONPath.get(apiModel, "$.operations", Collections.<String, Map>emptyMap());
            for (Map.Entry<String, Map> operationEntry : operations.entrySet()) {
                operationEntry.getValue().put("operationId", operationEntry.getKey());
                addOperationIdVariants(operationEntry.getValue());
                addNormalizedTagName(operationEntry.getValue());
                addChannelNameToOperation(operationEntry.getValue(), JSONPath.get(operationEntry.getValue(), "$.channel.x--channel"));
                setDeprecatedXMessages(apiModel, operationEntry.getValue());
            }
        }

        Map<String, Map> componentsMessages = JSONPath.get(apiModel, "$.components.messages", Collections.emptyMap());
        for (Map.Entry<String, Map> message : componentsMessages.entrySet()) {
            if (!message.getValue().containsKey("name")) {
                message.getValue().put("name", message.getKey());
            }
        }

        Set<Map<String, Object>> allMessages = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String operationId : AsyncAPIUtils.allOperationIds(apiModel)) {
            allMessages.addAll(AsyncAPIUtils.operationMessages(apiModel, operationId));
        }
        allMessages.forEach(message -> calculateMessageParamType(apiModel, message));

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

    private void setHasRuntimeHeaders(Map apiModel, Map operation) {
        if(operation != null) {
            String operationId = (String) operation.get("operationId");
            List<Map<String, Object>> messages = operationId == null ? List.of() : AsyncAPIUtils.operationMessages(apiModel, operationId);
            boolean hasAutoheader = !JSONPath.get(messages, String.format("$[*]..headers..[?(@.%s)]", runtimeHeadersProperty), Collections.emptyList()).isEmpty();
            if(hasAutoheader) {
                operation.put("x--has-runtime-headers", true);
            }
        }
    }

    /**
     * @deprecated no longer used by the SDK, which navigates messages through {@link AsyncAPIUtils}
     * instead. Retained as a no-op for binary/source compatibility with any code that calls it directly;
     * {@code x--messages} population now happens in {@link #process}. Will be removed in a future release.
     */
    @Deprecated
    public void collectMessages(Map<String, Object> operation) {
        // no-op: see setDeprecatedXMessages/setDeprecatedXMessagesForChannel
    }

    /**
     * @deprecated {@code x--messages} is a derived, non-canonical property kept only so that existing
     * custom templates that read it directly keep working. The SDK's own templates/processors navigate
     * messages through {@link AsyncAPIUtils#operationMessages}/{@link AsyncAPIUtils#channelMessages}
     * instead and never read this property. It may be removed in a future release.
     */
    @Deprecated
    private void setDeprecatedXMessages(Map apiModel, Map operation) {
        if (operation != null) {
            String operationId = (String) operation.get("operationId");
            if (operationId != null) {
                operation.put("x--messages", AsyncAPIUtils.operationMessages(apiModel, operationId));
            }
        }
    }

    /** @deprecated see {@link #setDeprecatedXMessages}. */
    @Deprecated
    private void setDeprecatedXMessagesForChannel(Map apiModel, Map channel, String channelId) {
        if (channel != null) {
            channel.put("x--messages", AsyncAPIUtils.channelMessages(apiModel, channelId));
        }
    }

    private String findSchemaFormat(Map<String, Object> apiModel, Map<String, Object> message) {
        var asyncapiVersion = JSONPath.get(apiModel, "$.asyncapi");
        var defaultSchemaFormat = AsyncApiProcessor.SchemaFormatType.ASYNCAPI_YAML.getSchemaFormat((String) asyncapiVersion);
        var schemaFormat = firstNonNull(JSONPath.getFirst(message, "$.payload.schemaFormat", "$.schemaFormat"), defaultSchemaFormat);
        return normalizeSchemaFormat((String) schemaFormat);
    }

    public void calculateMessageParamType(Map<String, Object> apiModel, Map<String, Object> message) {
        String schemaFormat = findSchemaFormat(apiModel, message);
        String javaType = null;
        if ("avro".equals(schemaFormat)) {
            String name = JSONPath.getFirst(message, "$.payload.schema.name", "$.payload.name");
            String namespace = JSONPath.getFirst(message,"$.payload.schema.namespace", "$.payload.namespace");
            javaType = namespace + "." + name;
        }
        if ("jsonSchema".equals(schemaFormat)) {
            javaType = JSONPath.getFirst(message,"$.payload.schema.javaType", "$.payload.javaType");
        }
        if ("asyncapi".equals(schemaFormat) || "openapi".equals(schemaFormat)) {
            javaType = normalizeTagName(JSONPath.getFirst(message, "$.payload.schema.x--schema-name", "$.payload.x--schema-name"));
            if (javaType == null) {
                javaType = normalizeTagName((String) message.getOrDefault("x-javaType", message.getOrDefault("messageId", message.get("name"))));
            }
            javaType = modelNamePrefix + javaType + modelNameSuffix;
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
