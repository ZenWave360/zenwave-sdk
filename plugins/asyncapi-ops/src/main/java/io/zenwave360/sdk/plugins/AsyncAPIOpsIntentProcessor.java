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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds an in-memory {@link AsyncAPIOpsIntent} from all parsed AsyncAPI models in context
 * and places it under the key {@code "intent"}.
 *
 * <p>Channel ownership is determined by the absence of {@code x--external-channel}: channels
 * declared inline in a spec file are owned (generate topic + schemas); channels resolved
 * from a cross-file {@code $ref} are external (contribute ACLs and error topics only).
 *
 * <p>Retry/DLQ provisioning is driven by the {@code x-error-topics} operation binding extension.
 */
public class AsyncAPIOpsIntentProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Used to merge env-server-overrides from channel and error-topic bindings.")
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

        for (Model apiModel : apis) {
            processModel(apiModel, intent);
        }

        contextModel.put("intent", intent);
        return contextModel;
    }

    private void processModel(Model apiModel, AsyncAPIOpsIntent intent) {
        Map<String, Map> channels = JSONPath.get(apiModel, "$.channels", Collections.emptyMap());
        Map<String, Map> operations = JSONPath.get(apiModel, "$.operations", Collections.emptyMap());

        for (Map.Entry<String, Map> entry : channels.entrySet()) {
            if (isOwnedChannel(entry.getValue())) {
                intent.topics.add(buildOwnedTopic(entry.getKey(), entry.getValue()));
                intent.schemas.addAll(buildSchemas(entry.getKey(), entry.getValue()));
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

        topic.partitions = intValue(kafkaBinding, "partitions", 1);
        topic.replicationFactor = intValue(kafkaBinding, "replicas", 1);
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
            topic.partitions = intValue(merged, "partitions", 1);
            topic.replicationFactor = intValue(merged, "replicas", 1);
            topic.config = buildTopicConfig(merged);
        }
        return topic;
    }

    // -------------------------------------------------------------------------
    // Schemas (owned channels only)
    // -------------------------------------------------------------------------

    private List<AsyncAPIOpsIntent.SchemaIntent> buildSchemas(String channelName, Map channel) {
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
            schema.schemaFile = resolveSchemaFile(message);

            if (schema.schemaFile == null) {
                log.warn("Could not resolve schema file path for message '{}' in channel '{}'", messageName, channelName);
            }
            result.add(schema);
        }
        return result;
    }

    private String resolveSchemaFile(Map message) {
        Map schema = JSONPath.get(message, "$.payload.schema");
        if (schema == null) {
            return null;
        }
        String originalRef = (String) schema.get("x--original-$ref");
        if (originalRef != null) {
            int avroIdx = originalRef.lastIndexOf("avro/");
            if (avroIdx >= 0) {
                return originalRef.substring(avroIdx);
            }
            String fileName = originalRef.substring(originalRef.lastIndexOf('/') + 1);
            return "avro/" + fileName;
        }
        String name = (String) schema.get("name");
        return name != null ? "avro/" + name + ".avsc" : null;
    }

    // -------------------------------------------------------------------------
    // Operations → ACLs + error topics
    // -------------------------------------------------------------------------

    private void processOperation(Map operation, AsyncAPIOpsIntent intent) {
        String action = (String) operation.get("action");
        String principal = JSONPath.get(operation, "$.bindings.kafka.x-principal");

        Map operationChannel = JSONPath.get(operation, "$.channel");
        if (operationChannel == null) {
            return;
        }

        String topicAddress = (String) operationChannel.get("address");
        if (topicAddress == null || principal == null) {
            return;
        }

        boolean isSend = "send".equals(action);

        // ACL for the main topic
        AsyncAPIOpsIntent.AclIntent acl = new AsyncAPIOpsIntent.AclIntent();
        acl.topicName = topicAddress;
        acl.principal = "User:" + principal;
        acl.operation = isSend ? "Write" : "Read";
        acl.resourceName = toTerraformId(topicAddress + "_User_" + principal + "_" + acl.operation);
        intent.addAcl(acl);

        // Error topics for receive operations
        if (!isSend) {
            String groupId = getGroupId(operation);
            Map errorTopics = JSONPath.get(operation, "$.bindings.kafka.x-error-topics");
            if (errorTopics != null && groupId != null) {
                expandErrorTopics(errorTopics, groupId, topicAddress, principal, intent);
            }
        }
    }

    private String getGroupId(Map operation) {
        // Standard Kafka binding field takes precedence; x-groupId is the legacy fallback
        String groupId = JSONPath.get(operation, "$.bindings.kafka.groupId");
        if (groupId == null) {
            groupId = JSONPath.get(operation, "$.bindings.kafka.x-groupId");
        }
        return groupId;
    }

    private void expandErrorTopics(Map errorTopics, String groupId, String topicAddress, String principal, AsyncAPIOpsIntent intent) {
        String addressTemplate = (String) errorTopics.get("addressTemplate");
        if (addressTemplate == null) {
            log.warn("x-error-topics.addressTemplate is missing for groupId='{}' topic='{}' — skipping", groupId, topicAddress);
            return;
        }

        int retryTopics = intValue(errorTopics, "retryTopics", 0);
        Map retryConfig = (Map) errorTopics.get("retry");
        Map dlqConfig = (Map) errorTopics.get("dlq");

        if (retryConfig != null) {
            for (int i = 0; i < retryTopics; i++) {
                String topicName = expandTemplate(addressTemplate, groupId, topicAddress, "retry-" + i);
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

    private void addErrorTopicAcl(String topicName, String principal, AsyncAPIOpsIntent intent) {
        AsyncAPIOpsIntent.AclIntent acl = new AsyncAPIOpsIntent.AclIntent();
        acl.topicName = topicName;
        acl.principal = "User:" + principal;
        acl.operation = "Read";
        acl.resourceName = toTerraformId(topicName + "_User_" + principal + "_Read");
        intent.addAcl(acl);
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
     * Merges {@code x-env-server-overrides[server]} into the channel kafka binding.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeChannelServerOverrides(Map<String, Object> kafkaBinding) {
        if (server == null || kafkaBinding.isEmpty()) {
            return kafkaBinding;
        }
        Map<String, Object> overrides = JSONPath.get(kafkaBinding, "$.x-env-server-overrides." + server);
        if (overrides == null || overrides.isEmpty()) {
            return kafkaBinding;
        }
        return Maps.deepMerge(Maps.copy(kafkaBinding), overrides);
    }

    /**
     * Merges {@code env-server-overrides[server]} into the error topic (retry/dlq) config.
     * Error topic configs use {@code env-server-overrides} (no {@code x-} prefix).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeErrorTopicServerOverrides(Map<String, Object> config) {
        if (server == null || config.isEmpty()) {
            return config;
        }
        Map<String, Object> overrides = JSONPath.get(config, "$.env-server-overrides." + server);
        if (overrides == null || overrides.isEmpty()) {
            return config;
        }
        return Maps.deepMerge(Maps.copy(config), overrides);
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
}
