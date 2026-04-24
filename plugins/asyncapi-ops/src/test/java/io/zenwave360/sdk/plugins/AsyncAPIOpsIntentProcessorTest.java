package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

public class AsyncAPIOpsIntentProcessorTest {

    static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    static final String ASYNCAPI_CLIENT   = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-client.yml";
    static final String ASYNCAPI_STOCK    = "classpath:retail-domain-catalog/merchandising/inventory/stock-replenishment/asyncapi.yml";

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

        // ACLs from provider operations + error topic ACLs (same principal, Read)
        Assertions.assertFalse(intent.acls.isEmpty());
        intent.acls.forEach(acl -> {
            Assertions.assertTrue(acl.principal.startsWith("User:"));
            Assertions.assertTrue(acl.operation.equals("Read") || acl.operation.equals("Write"));
        });
        // Error topic ACLs: 4 Read entries for the error topics
        long errorTopicAcls = intent.acls.stream()
                .filter(a -> a.topicName.contains(".__.")  && "Read".equals(a.operation))
                .count();
        Assertions.assertEquals(4, errorTopicAcls, "4 Read ACLs for error topics");
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

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Map<String, Object> loadAndBuildIntent(String server, String... specPaths) throws Exception {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();
        loader.apiFiles = java.util.Arrays.stream(specPaths).map(URI::create).toList();

        Map<String, Object> context = new java.util.LinkedHashMap<>();
        context = loader.process(context);

        AsyncAPIOpsIntentProcessor intentProcessor = new AsyncAPIOpsIntentProcessor();
        intentProcessor.server = server;
        return intentProcessor.process(context);
    }
}
