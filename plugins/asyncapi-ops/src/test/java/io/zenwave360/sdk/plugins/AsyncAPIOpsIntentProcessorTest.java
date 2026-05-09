package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class AsyncAPIOpsIntentProcessorTest {

    static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    static final String ASYNCAPI_CLIENT   = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-client.yml";
    static final String ASYNCAPI_STOCK    = "classpath:retail-domain-catalog/merchandising/inventory/stock-replenishment/asyncapi.yml";
    static final String ASYNCAPI_COLLISION_ALPHA = "classpath:collision/alpha/asyncapi.yml";
    static final String ASYNCAPI_COLLISION_BETA  = "classpath:collision/beta/asyncapi.yml";

    @Test
    public void test_provider_intent_generation() throws Exception {
        Map<String, Object> context = loadAndBuildIntent("staging", ASYNCAPI_PROVIDER);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        Assertions.assertNotNull(intent);
        Assertions.assertEquals("staging", intent.server);

        // 3 owned topics, staging partitions = 3
        long ownedTopics = intent.topics.stream().filter(t -> !t.isRetryOrDlq).count();
        Assertions.assertEquals(3, ownedTopics, "3 owned topics from asyncapi.yml");
        intent.topics.stream().filter(t -> !t.isRetryOrDlq).forEach(t ->
                Assertions.assertEquals(3, t.partitions, "Staging partitions=3 for " + t.topicName));

        // Resource names derived from full topic address
        intent.topics.stream().filter(t -> !t.isRetryOrDlq).forEach(t -> {
            Assertions.assertFalse(t.resourceName.contains("-"), "Resource name must not contain dashes: " + t.resourceName);
            Assertions.assertFalse(t.resourceName.contains("."), "Resource name must not contain dots: " + t.resourceName);
            Assertions.assertTrue(t.resourceName.contains("merchandising"), "Resource name should include full address: " + t.resourceName);
        });

        // doReserveStockCommand: retryTopics=3 + dlq → 4 error topics
        long retryDlqTopics = intent.topics.stream().filter(t -> t.isRetryOrDlq).count();
        Assertions.assertEquals(4, retryDlqTopics, "4 error topics from doReserveStockCommand");

        // Error topic names use .__.  separator
        intent.topics.stream().filter(t -> t.isRetryOrDlq).forEach(t ->
                Assertions.assertTrue(t.topicName.contains(".__.")));

        // Error topics are fully configured (partitions, replicationFactor, config)
        intent.topics.stream().filter(t -> t.isRetryOrDlq && t.topicName.contains("retry")).forEach(t -> {
            Assertions.assertEquals(1, t.partitions);
            Assertions.assertNotNull(t.config, "Retry topic should have config from silver preset");
            Assertions.assertTrue(t.config.containsKey("retention.ms"));
        });
        intent.topics.stream().filter(t -> t.isRetryOrDlq && t.topicName.contains("dlq")).forEach(t -> {
            Assertions.assertNotNull(t.config, "DLQ topic should have config from standard preset");
            Assertions.assertTrue(t.config.containsKey("cleanup.policy"));
        });

        // 3 schemas (owned channels, Avro messages)
        Assertions.assertEquals(3, intent.schemas.size());
        intent.schemas.forEach(s -> {
            Assertions.assertTrue(s.subject.endsWith("-value"));
            Assertions.assertNotNull(s.schemaFile);
            Assertions.assertTrue(s.schemaFile.startsWith("asyncapi/avro/"));
            Assertions.assertNotNull(s.sourceSchemaUri);
            Assertions.assertFalse(s.resourceName.contains("."), "Schema resourceName must not contain dots");
        });

        // ACLs from provider operations + error topic ACLs
        Assertions.assertFalse(intent.acls.isEmpty());
        intent.acls.forEach(acl -> {
            Assertions.assertTrue(acl.principal.startsWith("User:"));
            Assertions.assertTrue(acl.operation.equals("Read") || acl.operation.equals("Write") || acl.operation.equals("Describe"));
        });
        long mainTopicDescribeAcls = intent.acls.stream()
                .filter(a -> "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(a.topicName))
                .filter(a -> "Describe".equals(a.operation))
                .count();
        Assertions.assertEquals(1, mainTopicDescribeAcls, "Receive operations should get Describe on the main topic");

        long sendTopicDescribeAcls = intent.acls.stream()
                .filter(a -> "merchandising.inventory.inventory-adjustment.reserve-stock.response.avro.v0".equals(a.topicName))
                .filter(a -> "Describe".equals(a.operation))
                .count();
        Assertions.assertEquals(1, sendTopicDescribeAcls, "Send operations should get Describe on the main topic");

        long errorTopicReadAcls = intent.acls.stream()
                .filter(a -> a.topicName.contains(".__.")  && "Read".equals(a.operation))
                .count();
        long errorTopicWriteAcls = intent.acls.stream()
                .filter(a -> a.topicName.contains(".__.")  && "Write".equals(a.operation))
                .count();
        long errorTopicDescribeAcls = intent.acls.stream()
                .filter(a -> a.topicName.contains(".__.")  && "Describe".equals(a.operation))
                .count();
        Assertions.assertEquals(4, errorTopicReadAcls, "4 Read ACLs for error topics");
        Assertions.assertEquals(4, errorTopicWriteAcls, "4 Write ACLs for error topics");
        Assertions.assertEquals(4, errorTopicDescribeAcls, "4 Describe ACLs for error topics");
    }

    @Test
    public void test_client_intent_generation() throws Exception {
        Map<String, Object> context = loadAndBuildIntent("staging", ASYNCAPI_CLIENT);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        // No owned topics (all channels carry x--original-$ref)
        long ownedTopics = intent.topics.stream().filter(t -> !t.isRetryOrDlq).count();
        Assertions.assertEquals(0, ownedTopics, "Client spec has no owned topics");

        // No schemas
        Assertions.assertEquals(0, intent.schemas.size(), "Client spec has no schemas");

        // Retry/DLQ from receive operations (onReplenishStockResponse, onStockReplenishedEvent,
        // onRecalculatePriceResponse, onPriceChangedEvent — 4 × (3 retry + 1 dlq) = 16)
        long retryDlqTopics = intent.topics.stream().filter(t -> t.isRetryOrDlq).count();
        Assertions.assertEquals(16, retryDlqTopics, "16 retry/dlq topics from client receive operations");

        // ACLs from all client operations
        Assertions.assertFalse(intent.acls.isEmpty());

        // x-groupId fallback: all error topics use the value from x-groupId (not groupId)
        // verify topic names contain the expected group prefix derived via x-groupId
        long xGroupIdErrorTopics = intent.topics.stream()
                .filter(t -> t.isRetryOrDlq && t.topicName.startsWith("merchandising.inventory.inventory-adjustment.__."))
                .count();
        Assertions.assertTrue(xGroupIdErrorTopics > 0, "x-groupId fallback must produce error topics with the correct group prefix");
    }

    @Test
    public void test_provider_and_client_intent_generation() throws Exception {
        Map<String, Object> context = loadAndBuildIntent("staging", ASYNCAPI_PROVIDER, ASYNCAPI_CLIENT);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        // 3 owned topics (from provider), 4 retry/dlq (provider) + 16 retry/dlq (client) = 23 total
        long ownedTopics = intent.topics.stream().filter(t -> !t.isRetryOrDlq).count();
        Assertions.assertEquals(3, ownedTopics);

        long retryDlqTopics = intent.topics.stream().filter(t -> t.isRetryOrDlq).count();
        Assertions.assertEquals(20, retryDlqTopics, "4 (provider) + 16 (client) retry/dlq topics");

        // 3 schemas (owned channels only)
        Assertions.assertEquals(3, intent.schemas.size());

        // ACLs from both specs, deduplicated
        Assertions.assertFalse(intent.acls.isEmpty());
        // Verify no duplicate (topicName, principal, operation) triples
        long distinctAcls = intent.acls.stream()
                .map(a -> a.topicName + "|" + a.principal + "|" + a.operation)
                .distinct().count();
        Assertions.assertEquals(intent.acls.size(), distinctAcls, "ACLs must be deduplicated");

        // groupId (provider asyncapi.yml) produces 4 error topics (3 retry + 1 dlq)
        long providerErrorTopics = intent.topics.stream()
                .filter(t -> t.isRetryOrDlq && t.topicName.contains("reserve-stock.command"))
                .count();
        Assertions.assertEquals(4, providerErrorTopics, "groupId on provider spec must produce 4 error topics");

        // x-groupId (client asyncapi-client.yml) also produces error topics
        long clientErrorTopics = intent.topics.stream()
                .filter(t -> t.isRetryOrDlq && t.topicName.contains("replenish-stock"))
                .count();
        Assertions.assertTrue(clientErrorTopics > 0, "x-groupId on client spec must produce error topics");
    }

    @Test
    public void test_loader_prepends_apiFile_and_deduplicates_apiFiles() throws Exception {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();
        loader.apiFile = URI.create(ASYNCAPI_PROVIDER);
        loader.apiFiles = List.of(URI.create(ASYNCAPI_PROVIDER), URI.create(ASYNCAPI_CLIENT));

        Map<String, Object> context = loader.process(new LinkedHashMap<>());
        List<Model> apis = getApis(context);

        Assertions.assertEquals(2, apis.size(), "provider must not be loaded twice when present in apiFile and apiFiles");
        Assertions.assertEquals(URI.create(ASYNCAPI_PROVIDER), apis.get(0).getUri(), "apiFile must be loaded first");
        Assertions.assertEquals(URI.create(ASYNCAPI_CLIENT), apis.get(1).getUri());

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        Assertions.assertEquals(3, intent.topics.stream().filter(t -> !t.isRetryOrDlq).count());
        Assertions.assertEquals(20, intent.topics.stream().filter(t -> t.isRetryOrDlq).count());
    }


    @Test
    public void test_multi_provider_intent_generation() throws Exception {
        Map<String, Object> context = loadAndBuildIntent("staging", ASYNCAPI_PROVIDER, ASYNCAPI_STOCK);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        // 3 owned topics from inventory-adjustment + 3 from stock-replenishment = 6
        long ownedTopics = intent.topics.stream().filter(t -> !t.isRetryOrDlq).count();
        Assertions.assertEquals(6, ownedTopics, "6 owned topics across two provider specs");

        // 6 schemas total
        Assertions.assertEquals(6, intent.schemas.size());

        // No duplicate resource names (full address guarantees uniqueness)
        long distinctResourceNames = intent.topics.stream()
                .map(t -> t.resourceName).distinct().count();
        Assertions.assertEquals(intent.topics.size(), distinctResourceNames, "Resource names must be unique");
    }

    @Test
    public void test_colliding_api_basenames_get_distinct_schema_target_folders() throws Exception {
        Map<String, Object> context = loadAndBuildIntent(null, ASYNCAPI_COLLISION_ALPHA, ASYNCAPI_COLLISION_BETA);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        Assertions.assertEquals(2, intent.schemas.size());
        Assertions.assertEquals(
                List.of("asyncapi/avro/CustomerCreated.avsc", "asyncapi_2/avro/CustomerUpdated.avsc"),
                intent.schemas.stream().map(s -> s.schemaFile).sorted().toList());
    }

    @Test
    public void test_unprefixed_extensions_are_supported() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> channelBinding = JSONPath.get(api, "$.channels['reserve-stock-command'].bindings.kafka");
        channelBinding.put("env-server-overrides", channelBinding.remove("x-env-server-overrides"));

        Map<String, Object> operationBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        operationBinding.put("error-topics", operationBinding.remove("x-error-topics"));

        context = buildIntent("staging", context);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        long ownedTopics = intent.topics.stream().filter(t -> !t.isRetryOrDlq).count();
        Assertions.assertEquals(3, ownedTopics);
        intent.topics.stream().filter(t -> !t.isRetryOrDlq).forEach(t -> Assertions.assertEquals(3, t.partitions));

        long retryDlqTopics = intent.topics.stream().filter(t -> t.isRetryOrDlq).count();
        Assertions.assertEquals(4, retryDlqTopics, "Unprefixed error-topics must still generate retry/DLQ topics");
    }

    @Test
    public void test_defaults_are_sourced_from_base_config_when_overrides_are_absent() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> channelBinding = JSONPath.get(api, "$.channels['reserve-stock-command'].bindings.kafka");
        Map<String, Object> baseChannelBinding = new LinkedHashMap<>(channelBinding);
        baseChannelBinding.remove("x-env-server-overrides");
        baseChannelBinding.remove("env-server-overrides");
        replaceAll(channelBinding, baseChannelBinding);

        Map<String, Object> operationBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        Map<String, Object> errorTopics = JSONPath.get(operationBinding, "$.x-error-topics");
        Map<String, Object> retryConfig = JSONPath.get(errorTopics, "$.retry");
        Map<String, Object> retryBaseConfig = new LinkedHashMap<>(retryConfig);
        retryBaseConfig.remove("x-env-server-overrides");
        retryBaseConfig.remove("env-server-overrides");
        replaceAll(retryConfig, retryBaseConfig);

        context = buildIntent("staging", context);

        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        AsyncAPIOpsIntent.TopicIntent ownedTopic = intent.topics.stream()
                .filter(t -> !t.isRetryOrDlq && "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(t.topicName))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(20, ownedTopic.partitions, "Without env overrides the base channel partitions must be used");
        Assertions.assertEquals(3, ownedTopic.replicationFactor, "Without env overrides the base channel replicas must be used");

        AsyncAPIOpsIntent.TopicIntent retryTopic = intent.topics.stream()
                .filter(t -> t.isRetryOrDlq && t.topicName.endsWith(".retry-0"))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(1, retryTopic.partitions, "Retry topic should fall back to the base retry config");
        Assertions.assertEquals(2, retryTopic.replicationFactor, "Retry topic should fall back to the base retry config");
        Assertions.assertEquals("259200000", retryTopic.config.get("retention.ms"), "Retry topic should keep base topicConfiguration values");
    }

    @Test
    public void test_missing_error_topic_template_keeps_main_acls_but_skips_retry_and_dlq_topics() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> operationBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        Map<String, Object> errorTopics = JSONPath.get(operationBinding, "$.x-error-topics");
        errorTopics.remove("addressTemplate");

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertEquals(0, intent.topics.stream().filter(t -> t.isRetryOrDlq).count(), "retry/dlq topics require addressTemplate");
        Assertions.assertTrue(intent.acls.stream().anyMatch(a ->
                "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(a.topicName)
                        && "Read".equals(a.operation)));
        Assertions.assertTrue(intent.acls.stream().anyMatch(a ->
                "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(a.topicName)
                        && "Describe".equals(a.operation)));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Map<String, Object> loadAndBuildIntent(String server, String... specPaths) throws Exception {
        return buildIntent(server, loadContext(specPaths));
    }

    private Map<String, Object> loadContext(String... specPaths) throws Exception {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();
        loader.apiFiles = java.util.Arrays.stream(specPaths).map(URI::create).toList();

        Map<String, Object> context = new java.util.LinkedHashMap<>();
        return loader.process(context);
    }

    private Map<String, Object> buildIntent(String server, Map<String, Object> context) {
        AsyncAPIOpsIntentProcessor intentProcessor = new AsyncAPIOpsIntentProcessor();
        intentProcessor.server = server;
        return intentProcessor.process(context);
    }

    @SuppressWarnings("unchecked")
    private List<Model> getApis(Map<String, Object> context) {
        return (List<Model>) context.get("apis");
    }

    private void replaceAll(Map<String, Object> target, Map<String, Object> replacement) {
        target.clear();
        target.putAll(replacement);
    }
}
