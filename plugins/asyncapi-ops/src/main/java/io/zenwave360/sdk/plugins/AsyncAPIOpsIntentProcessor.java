package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds an in-memory {@link AsyncAPIOpsIntent} from all parsed AsyncAPI models in context
 * and places it under the key {@code "intent"}.
 *
 * <p>Channel ownership is determined by the absence of {@code x--external-channel}: channels
 * declared inline in a spec file are owned (generate topic + schemas); channels resolved
 * from a cross-file {@code $ref} are external (contribute ACLs and error topics only).
 *
 * <p>Retry/DLQ provisioning is driven by the {@code x-error-topics} or {@code error-topics}
 * operation binding extension.
 */
public class AsyncAPIOpsIntentProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Used to merge x-env-server-overrides/env-server-overrides from channel and error-topic bindings.")
    public String server;

    @DocumentedOption(description = "Context key holding the list of AsyncAPI models loaded by AsyncAPIOpsSpecLoader.")
    public String sourceProperty = "apis";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        List<Model> apis = (List<Model>) contextModel.getOrDefault(sourceProperty, List.of());

        if (apis.isEmpty()) {
            log.warn("No AsyncAPI models found under context key '{}'. Intent will be empty.", sourceProperty);
        }

        AsyncAPIOpsIntent intent = new AsyncAPIOpsIntent();
        intent.server = server;

        Map<Model, String> apiNamespaces = createApiNamespaces(apis);
        for (Model apiModel : apis) {
            processModel(apiModel, apiNamespaces.get(apiModel), intent);
        }

        contextModel.put("intent", intent);
        return contextModel;
    }

    private void processModel(Model apiModel, String apiNamespace, AsyncAPIOpsIntent intent) {
        Map<String, Map> channels = JSONPath.get(apiModel, "$.channels", Collections.emptyMap());
        Map<String, Map> operations = JSONPath.get(apiModel, "$.operations", Collections.emptyMap());

        for (Map.Entry<String, Map> entry : channels.entrySet()) {
            if (isOwnedChannel(entry.getValue())) {
                intent.topics.add(buildOwnedTopic(entry.getKey(), entry.getValue()));
                intent.schemas.addAll(buildSchemas(apiModel, apiNamespace, entry.getKey(), entry.getValue()));
            }
        }

        for (Map.Entry<String, Map> entry : operations.entrySet()) {
            processOperation(entry.getValue(), intent);
        }
    }

    // -------------------------------------------------------------------------
    // Ownership
    // -------------------------------------------------------------------------

    /**
     * A channel is owned when it was declared inline in the spec file.
     * {@code x--external-channel: true} is set by {@link AsyncAPIOpsSpecLoader} on channels
     * resolved from cross-file $refs, before AsyncApiProcessor overwrites x--original-$ref.
     */
    private boolean isOwnedChannel(Map channel) {
        return !Boolean.TRUE.equals(channel.get("x--external-channel"));
    }

    // -------------------------------------------------------------------------
    // Topics
    // -------------------------------------------------------------------------

    private AsyncAPIOpsIntent.TopicIntent buildOwnedTopic(String channelName, Map channel) {
        AsyncAPIOpsIntent.TopicIntent topic = new AsyncAPIOpsIntent.TopicIntent();
        String address = (String) channel.get("address");
        topic.topicName = address;
        topic.resourceName = toTerraformId(address);

        Map<String, Object> kafkaBinding = mergeChannelServerOverrides(
                JSONPath.get(channel, "$.bindings.kafka", Collections.emptyMap()));

        topic.partitions = integerValue(kafkaBinding, "partitions");
        topic.replicationFactor = integerValue(kafkaBinding, "replicas");
        topic.config = buildTopicConfig(kafkaBinding);
        return topic;
    }

    private AsyncAPIOpsIntent.TopicIntent buildErrorTopic(String topicName, Map errorTopicConfig) {
        AsyncAPIOpsIntent.TopicIntent topic = new AsyncAPIOpsIntent.TopicIntent();
        topic.topicName = topicName;
        topic.resourceName = toTerraformId(topicName);
        topic.isRetryOrDlq = true;

        if (errorTopicConfig != null) {
            Map<String, Object> merged = mergeErrorTopicServerOverrides(errorTopicConfig);
            topic.partitions = integerValue(merged, "partitions");
            topic.replicationFactor = integerValue(merged, "replicas");
            topic.config = buildTopicConfig(merged);
        }
        return topic;
    }

    // -------------------------------------------------------------------------
    // Schemas (owned channels only)
    // -------------------------------------------------------------------------

    private List<AsyncAPIOpsIntent.SchemaIntent> buildSchemas(Model apiModel, String apiNamespace, String channelName, Map channel) {
        List<AsyncAPIOpsIntent.SchemaIntent> result = new ArrayList<>();
        Map<String, Map> messages = JSONPath.get(channel, "$.messages", Collections.emptyMap());
        String topicAddress = (String) channel.get("address");

        for (Map.Entry<String, Map> entry : messages.entrySet()) {
            String messageName = entry.getKey();
            Map message = entry.getValue();

            String schemaFormat = JSONPath.get(message, "$.payload.schemaFormat");
            if (schemaFormat == null || !schemaFormat.toLowerCase().contains("avro")) {
                continue;
            }

            AsyncAPIOpsIntent.SchemaIntent schema = new AsyncAPIOpsIntent.SchemaIntent();
            schema.subject = topicAddress + "-" + messageName + "-value";
            schema.resourceName = toTerraformId(schema.subject);
            schema.compatibility = JSONPath.get(message, "$.bindings.kafka.x-schemaCompatibility");
            schema.sourceSchemaUri = resolveSchemaSourceUri(apiModel, message);
            schema.schemaFile = resolveTargetSchemaFile(apiNamespace, message);

            if (schema.sourceSchemaUri == null) {
                log.warn("Could not resolve schema file path for message '{}' in channel '{}'", messageName, channelName);
            }
            result.add(schema);
        }
        return result;
    }

    private String resolveSchemaSourceUri(Model apiModel, Map message) {
        Map schema = JSONPath.get(message, "$.payload.schema");
        if (schema == null) {
            return null;
        }
        String originalRef = (String) schema.get("x--original-$ref");
        String name = (String) schema.get("name");
        String schemaRef = originalRef != null ? originalRef : (name != null ? "avro/" + name + ".avsc" : null);
        if (schemaRef == null) {
            return null;
        }
        return resolveAgainstApi(apiModel.getUri(), schemaRef).toString();
    }

    private String resolveTargetSchemaFile(String apiNamespace, Map message) {
        Map schema = JSONPath.get(message, "$.payload.schema");
        if (schema == null) {
            return null;
        }
        String originalRef = (String) schema.get("x--original-$ref");
        String name = (String) schema.get("name");
        String schemaRef = originalRef != null ? originalRef : (name != null ? "avro/" + name + ".avsc" : null);
        if (schemaRef == null) {
            return null;
        }
        return apiNamespace + "/" + toTargetRelativePath(schemaRef, name);
    }

    // -------------------------------------------------------------------------
    // Operations → ACLs + error topics
    // -------------------------------------------------------------------------

    private void processOperation(Map operation, AsyncAPIOpsIntent intent) {
        String action = (String) operation.get("action");
        Map kafkaBinding = JSONPath.get(operation, "$.bindings.kafka", Collections.emptyMap());
        String principal = getStringFirst(kafkaBinding, "x-principal", "principal");

        Map operationChannel = JSONPath.get(operation, "$.channel");
        if (operationChannel == null) {
            return;
        }

        String topicAddress = (String) operationChannel.get("address");
        if (topicAddress == null || principal == null) {
            return;
        }

        boolean isSend = "send".equals(action);
        boolean isReceive = "receive".equals(action);

        // ACLs for the main topic
        if (isSend) {
            addTopicAcl(topicAddress, principal, "Write", intent);
            addTopicAcl(topicAddress, principal, "Describe", intent);
            addTransactionalAclIfNeeded(kafkaBinding, principal, intent);
        } else if (isReceive) {
            addTopicAcl(topicAddress, principal, "Read", intent);
            addTopicAcl(topicAddress, principal, "Describe", intent);
        }
        addSchemaRegistryReadBindings(operationChannel, principal, intent);

        // Error topics for receive operations
        if (isReceive) {
            String groupId = getGroupId(kafkaBinding);
            if (groupId != null) {
                addGroupAcl(groupId, principal, "Read", intent);
            }
            Map errorTopics = getErrorTopicsConfig(operation);
            if (errorTopics != null && groupId != null) {
                expandErrorTopics(errorTopics, groupId, topicAddress, principal, intent);
            }
        }
    }

    private String getGroupId(Map kafkaBinding) {
        String extensionGroupId = getString(kafkaBinding.get("x-groupId"));
        if (extensionGroupId != null) {
            return extensionGroupId;
        }
        Object groupIdSchema = kafkaBinding.get("groupId");
        if (groupIdSchema == null) {
            return null;
        }
        String groupId = firstStringFromSchema(groupIdSchema);
        if (groupId == null) {
            log.warn("Kafka operation binding groupId must be a schema with enum, string const, or string-array const. Skipping group-scoped resources.");
        }
        return groupId;
    }

    private String firstStringFromSchema(Object schema) {
        if (!(schema instanceof Map map)) {
            return null;
        }
        Object constValue = map.get("const");
        String constString = firstString(constValue);
        if (constString != null) {
            return constString;
        }
        return firstString(map.get("enum"));
    }

    private void expandErrorTopics(Map errorTopics, String groupId, String topicAddress, String principal, AsyncAPIOpsIntent intent) {
        int retryTopics = intValue(errorTopics, "retryTopics", 0);
        List<String> retrySuffixes = validateRetrySuffixes(errorTopics, retryTopics);

        String addressTemplate = (String) errorTopics.get("addressTemplate");
        if (addressTemplate == null) {
            log.warn("x-error-topics.addressTemplate is missing for groupId='{}' topic='{}' — skipping", groupId, topicAddress);
            return;
        }

        Map retryConfig = (Map) errorTopics.get("retry");
        Map dlqConfig = (Map) errorTopics.get("dlq");

        if (retryConfig != null) {
            for (int i = 0; i < retryTopics; i++) {
                String suffix = retrySuffixes == null ? "retry-" + i : retrySuffixes.get(i);
                String topicName = expandTemplate(addressTemplate, groupId, topicAddress, suffix);
                intent.topics.add(buildErrorTopic(topicName, retryConfig));
                addErrorTopicAcl(topicName, principal, intent);
            }
        }

        if (dlqConfig != null) {
            String topicName = expandTemplate(addressTemplate, groupId, topicAddress, "dlq");
            intent.topics.add(buildErrorTopic(topicName, dlqConfig));
            addErrorTopicAcl(topicName, principal, intent);
        }
    }

    private List<String> validateRetrySuffixes(Map errorTopics, int retryTopics) {
        if (!errorTopics.containsKey("retrySuffixes")) {
            return null;
        }

        Object value = errorTopics.get("retrySuffixes");
        if (!(value instanceof List<?> suffixes)) {
            throw new IllegalArgumentException("x-error-topics.retrySuffixes must be an array of strings");
        }
        if (suffixes.size() != retryTopics) {
            throw new IllegalArgumentException("x-error-topics.retrySuffixes length must equal retryTopics");
        }

        List<String> validatedSuffixes = new ArrayList<>(suffixes.size());
        Set<String> uniqueSuffixes = new HashSet<>();
        for (Object suffix : suffixes) {
            if (!(suffix instanceof String stringSuffix)) {
                throw new IllegalArgumentException("x-error-topics.retrySuffixes must contain only strings");
            }
            if (stringSuffix.isBlank()) {
                throw new IllegalArgumentException("x-error-topics.retrySuffixes must not contain blank values");
            }
            if (!uniqueSuffixes.add(stringSuffix)) {
                throw new IllegalArgumentException("x-error-topics.retrySuffixes values must be unique");
            }
            validatedSuffixes.add(stringSuffix);
        }
        return validatedSuffixes;
    }

    private void addErrorTopicAcl(String topicName, String principal, AsyncAPIOpsIntent intent) {
        addTopicAcl(topicName, principal, "Read", intent);
        addTopicAcl(topicName, principal, "Write", intent);
        addTopicAcl(topicName, principal, "Describe", intent);
    }

    private void addTopicAcl(String topicName, String principal, String operation, AsyncAPIOpsIntent intent) {
        addKafkaAcl("TOPIC", topicName, "LITERAL", principal, operation, intent);
    }

    private void addGroupAcl(String groupId, String principal, String operation, AsyncAPIOpsIntent intent) {
        addKafkaAcl("GROUP", groupId, "LITERAL", principal, operation, intent);
    }

    private void addTransactionalAclIfNeeded(Map kafkaBinding, String principal, AsyncAPIOpsIntent intent) {
        if (!Boolean.TRUE.equals(kafkaBinding.get("transactional"))) {
            return;
        }
        String transactionalIdPrefix = getStringFirst(kafkaBinding, "x-transactional-id-prefix", "transactional-id-prefix");
        if (transactionalIdPrefix == null) {
            log.warn("Kafka send operation is transactional but x-transactional-id-prefix is missing. Skipping transactional id ACL.");
            return;
        }
        addKafkaAcl("TRANSACTIONAL_ID", transactionalIdPrefix, "PREFIXED", principal, "Write", intent);
    }

    private void addKafkaAcl(String resourceType, String kafkaResourceName, String patternType, String principal, String operation, AsyncAPIOpsIntent intent) {
        String principalResourceName = toTerraformId(principal);
        intent.addPrincipal(principal, principalResourceName);

        AsyncAPIOpsIntent.AclIntent acl = new AsyncAPIOpsIntent.AclIntent();
        acl.resourceType = resourceType;
        acl.kafkaResourceType = kafkaResourceType(resourceType);
        acl.patternType = patternType;
        acl.kafkaPatternType = kafkaPatternType(patternType);
        acl.kafkaResourceName = kafkaResourceName;
        acl.topicName = "TOPIC".equals(resourceType) ? kafkaResourceName : null;
        acl.principal = principal;
        acl.principalResourceName = principalResourceName;
        acl.operation = operation;
        acl.resourceName = toTerraformId(resourceType + "_" + kafkaResourceName + "_" + principal + "_" + operation + "_" + patternType);
        intent.addAcl(acl);
    }

    private String kafkaResourceType(String resourceType) {
        return switch (resourceType) {
            case "GROUP" -> "Group";
            case "TRANSACTIONAL_ID" -> "TransactionalID";
            default -> "Topic";
        };
    }

    private String kafkaPatternType(String patternType) {
        return "PREFIXED".equals(patternType) ? "Prefixed" : "Literal";
    }

    private void addSchemaRegistryReadBindings(Map operationChannel, String principal, AsyncAPIOpsIntent intent) {
        String topicAddress = (String) operationChannel.get("address");
        Map<String, Map> messages = JSONPath.get(operationChannel, "$.messages", Collections.emptyMap());
        for (Map.Entry<String, Map> entry : messages.entrySet()) {
            Map message = entry.getValue();
            String schemaFormat = JSONPath.get(message, "$.payload.schemaFormat");
            if (schemaFormat == null || !schemaFormat.toLowerCase().contains("avro")) {
                continue;
            }
            String subject = topicAddress + "-" + entry.getKey() + "-value";
            addSchemaRegistryReadBinding(subject, principal, intent);
        }
    }

    private void addSchemaRegistryReadBinding(String subject, String principal, AsyncAPIOpsIntent intent) {
        String principalResourceName = toTerraformId(principal);
        intent.addPrincipal(principal, principalResourceName);

        AsyncAPIOpsIntent.RoleBindingIntent roleBinding = new AsyncAPIOpsIntent.RoleBindingIntent();
        roleBinding.principal = principal;
        roleBinding.principalResourceName = principalResourceName;
        roleBinding.roleName = "DeveloperRead";
        roleBinding.crnPattern = "${var.schema_registry_crn}/subject=" + subject;
        roleBinding.resourceName = toTerraformId("schema_registry_" + subject + "_" + principal + "_DeveloperRead");
        intent.addRoleBinding(roleBinding);
    }

    private String expandTemplate(String template, String groupId, String channelAddress, String suffix) {
        return template
                .replace("${groupId}", groupId)
                .replace("${channel.address}", channelAddress)
                .replace("${suffix}", suffix);
    }

    // -------------------------------------------------------------------------
    // Binding helpers
    // -------------------------------------------------------------------------

    /**
     * Merges {@code x-env-server-overrides[server]} or {@code env-server-overrides[server]}
     * into the channel kafka binding.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeChannelServerOverrides(Map<String, Object> kafkaBinding) {
        if (server == null || kafkaBinding.isEmpty()) {
            return kafkaBinding;
        }
        Map<String, Object> overrides = getServerOverrides(kafkaBinding);
        if (overrides == null || overrides.isEmpty()) {
            return kafkaBinding;
        }
        return Maps.deepMerge(Maps.copy(kafkaBinding), overrides);
    }

    /**
     * Merges {@code x-env-server-overrides[server]} or {@code env-server-overrides[server]}
     * into the error topic (retry/dlq) config.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeErrorTopicServerOverrides(Map<String, Object> config) {
        if (server == null || config.isEmpty()) {
            return config;
        }
        Map<String, Object> overrides = getServerOverrides(config);
        if (overrides == null || overrides.isEmpty()) {
            return config;
        }
        return Maps.deepMerge(Maps.copy(config), overrides);
    }

    private Map getErrorTopicsConfig(Map operation) {
        return JSONPath.getFirst(operation, "$.bindings.kafka.x-error-topics", "$.bindings.kafka.error-topics");
    }

    private Map<String, Object> getServerOverrides(Map<String, Object> config) {
        return JSONPath.getFirst(config,
                "$.x-env-server-overrides." + server,
                "$.env-server-overrides." + server);
    }

    private String getStringFirst(Map map, String... keys) {
        for (String key : keys) {
            String value = getString(map.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String getString(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private String firstString(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(s -> !s.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Map<String, String> buildTopicConfig(Map<String, Object> binding) {
        Map<String, Object> topicConfig = JSONPath.get(binding, "$.topicConfiguration", Collections.emptyMap());
        Map<String, String> config = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : topicConfig.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                config.put(entry.getKey(), list.stream().map(Object::toString).collect(Collectors.joining(",")));
            } else {
                config.put(entry.getKey(), String.valueOf(value));
            }
        }
        return config.isEmpty() ? null : config;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Converts a Kafka topic address or subject to a valid Terraform resource identifier.
     * Dots, dashes, colons and slashes are replaced with underscores; runs are collapsed.
     */
    private String toTerraformId(String name) {
        return name.replace(".", "_").replace("-", "_").replace(":", "_").replace("/", "_")
                .replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private int intValue(Map map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Integer integerValue(Map map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Map<Model, String> createApiNamespaces(List<Model> apis) {
        Map<Model, String> namespaces = new HashMap<>();
        Map<String, Integer> collisions = new HashMap<>();
        for (Model api : apis) {
            String base = sanitizeApiBasename(api.getUri());
            int count = collisions.merge(base, 1, Integer::sum);
            namespaces.put(api, count == 1 ? base : base + "_" + count);
        }
        return namespaces;
    }

    private String sanitizeApiBasename(java.net.URI uri) {
        String source = Objects.toString(uri, "api");
        String fileName = source;
        int lastSlash = Math.max(source.lastIndexOf('/'), source.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash + 1 < source.length()) {
            fileName = source.substring(lastSlash + 1);
        }
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }
        fileName = fileName.replace(" ", "_").replaceAll("[^A-Za-z0-9._-]", "_");
        fileName = fileName.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return fileName.isBlank() ? "api" : fileName;
    }

    private java.net.URI resolveAgainstApi(java.net.URI apiUri, String schemaRef) {
        String path = stripFragment(schemaRef);
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            return java.net.URI.create(path);
        }
        if ("classpath".equalsIgnoreCase(apiUri.getScheme())) {
            String basePath = apiUri.getSchemeSpecificPart();
            String baseDir = basePath.contains("/") ? basePath.substring(0, basePath.lastIndexOf('/') + 1) : "";
            return java.net.URI.create("classpath:" + normalizeRelativePath(baseDir + path));
        }
        return apiUri.resolve(path);
    }

    private String toTargetRelativePath(String schemaRef, String schemaName) {
        String path = stripFragment(schemaRef);
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.isBlank() ? "avro/" + schemaName + ".avsc" : fileName;
        }
        String normalized = normalizeRelativePath(path);
        while (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        normalized = normalized.replaceAll("^/+", "");
        return normalized.isBlank() ? "avro/" + schemaName + ".avsc" : normalized;
    }

    private String stripFragment(String schemaRef) {
        int fragmentIndex = schemaRef.indexOf('#');
        return fragmentIndex >= 0 ? schemaRef.substring(0, fragmentIndex) : schemaRef;
    }

    private String normalizeRelativePath(String path) {
        List<String> normalized = new ArrayList<>();
        for (String segment : path.replace("\\", "/").split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!normalized.isEmpty()) {
                    normalized.remove(normalized.size() - 1);
                }
                continue;
            }
            normalized.add(segment);
        }
        return String.join("/", normalized);
    }
}
