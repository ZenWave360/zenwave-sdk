package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AsyncAPIOpsIntentProcessorTest {

    static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    static final String ASYNCAPI_CLIENT   = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-client.yml";
    static final String ASYNCAPI_STOCK    = "classpath:retail-domain-catalog/merchandising/inventory/stock-replenishment/asyncapi.yml";
    static final String ASYNCAPI_COLLISION_ALPHA = "classpath:collision/alpha/asyncapi.yml";
    static final String ASYNCAPI_COLLISION_BETA  = "classpath:collision/beta/asyncapi.yml";
    static final String ASYNCAPI_STREAMS = "classpath:kafka-streams/asyncapi-streams.yml";
    static final String ASYNCAPI_STREAMS_SECOND = "classpath:kafka-streams/asyncapi-streams-second.yml";
    static final String STREAMS_APPLICATION_ID = "merchandising.inventory.inventory-adjustment.streams-app";

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
            Assertions.assertFalse(acl.principal.startsWith("User:"));
            Assertions.assertEquals(acl.principal.replace('.', '_').replace('-', '_'), acl.principalResourceName);
            Assertions.assertFalse(acl.resourceName.contains("_User_"));
            Assertions.assertTrue(acl.operation.equals("Read") || acl.operation.equals("Write") || acl.operation.equals("Describe"));
        });
        Assertions.assertEquals(1, intent.principals.size(), "Provider operations share one logical principal");
        Assertions.assertEquals("merchandising.inventory.inventory-adjustment", intent.principals.get(0).name);
        Assertions.assertEquals("merchandising_inventory_inventory_adjustment", intent.principals.get(0).resourceName);
        intent.roleBindings.forEach(roleBinding -> {
            Assertions.assertEquals("merchandising.inventory.inventory-adjustment", roleBinding.principal);
            Assertions.assertEquals("merchandising_inventory_inventory_adjustment", roleBinding.principalResourceName);
            Assertions.assertFalse(roleBinding.resourceName.contains("_User_"));
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
                .filter(a -> a.topicName != null && a.topicName.contains(".__.")  && "Read".equals(a.operation))
                .count();
        long errorTopicWriteAcls = intent.acls.stream()
                .filter(a -> a.topicName != null && a.topicName.contains(".__.")  && "Write".equals(a.operation))
                .count();
        long errorTopicDescribeAcls = intent.acls.stream()
                .filter(a -> a.topicName != null && a.topicName.contains(".__.")  && "Describe".equals(a.operation))
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
                .map(a -> a.resourceType + "|" + a.kafkaResourceName + "|" + a.patternType + "|" + a.principal + "|" + a.operation)
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
    public void test_loader_applies_overlays_before_processing() throws Exception {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();
        loader.apiFile = URI.create(ASYNCAPI_PROVIDER);
        loader.apiOverlayFiles = List.of("classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi-overlay.yml");

        Map<String, Object> context = loader.process(new LinkedHashMap<>());
        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertTrue(intent.topics.stream()
                .anyMatch(t -> "merchandising.inventory.inventory-adjustment.reserve-stock.command.overlay.avro.v0".equals(t.topicName)));
    }

    @Test
    public void test_loader_with_no_input_files_produces_empty_apis_and_empty_intent() {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();

        Map<String, Object> context = loader.process(new LinkedHashMap<>());
        Assertions.assertEquals(List.of(), context.get("apis"));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        Assertions.assertTrue(intent.topics.isEmpty());
        Assertions.assertTrue(intent.schemas.isEmpty());
        Assertions.assertTrue(intent.acls.isEmpty());
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
    public void test_x_prefixed_values_take_precedence_over_standard_binding_values() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> channelBinding = JSONPath.get(api, "$.channels['reserve-stock-command'].bindings.kafka");
        Map<String, Object> overrides = JSONPath.get(channelBinding, "$.x-env-server-overrides.staging");
        overrides.put("partitions", 7);
        channelBinding.put("env-server-overrides", Map.of("staging", Map.of("partitions", 11)));

        Map<String, Object> operationBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        operationBinding.put("principal", "standard.principal");
        operationBinding.put("x-principal", "extension.principal");
        operationBinding.put("x-groupId", "extension.group");
        operationBinding.put("groupId", Map.of("type", "string", "enum", List.of("schema.group")));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        AsyncAPIOpsIntent.TopicIntent ownedTopic = intent.topics.stream()
                .filter(t -> !t.isRetryOrDlq && "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(t.topicName))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(7, ownedTopic.partitions, "x-env-server-overrides must win over env-server-overrides");

        Assertions.assertTrue(intent.acls.stream().anyMatch(a -> "extension.principal".equals(a.principal)));
        Assertions.assertFalse(intent.acls.stream().anyMatch(a -> "standard.principal".equals(a.principal)));
        Assertions.assertTrue(intent.topics.stream().anyMatch(t -> t.isRetryOrDlq && t.topicName.startsWith("extension.group.__.")),
                "x-groupId must win over schema groupId");
    }

    @Test
    public void test_group_id_schema_shapes_and_transactional_prefix_acls() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> receiveBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        receiveBinding.remove("x-groupId");
        receiveBinding.put("groupId", Map.of("type", "string", "const", List.of("const-array.group", "ignored.group")));

        Map<String, Object> sendBinding = JSONPath.get(api, "$.operations['onReserveStockResponse'].bindings.kafka");
        sendBinding.put("transactional", true);
        sendBinding.put("x-transactional-id-prefix", "inventory-adjustment-tx-");

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertTrue(intent.acls.stream().anyMatch(a ->
                "GROUP".equals(a.resourceType)
                        && "const-array.group".equals(a.kafkaResourceName)
                        && "Read".equals(a.operation)));
        Assertions.assertTrue(intent.topics.stream().anyMatch(t -> t.isRetryOrDlq && t.topicName.startsWith("const-array.group.__.")));
        Assertions.assertTrue(intent.acls.stream().anyMatch(a ->
                "TRANSACTIONAL_ID".equals(a.resourceType)
                        && "PREFIXED".equals(a.patternType)
                        && "inventory-adjustment-tx-".equals(a.kafkaResourceName)
                        && "Write".equals(a.operation)));
        Assertions.assertTrue(intent.roleBindings.stream().anyMatch(r ->
                "DeveloperRead".equals(r.roleName)
                        && r.crnPattern.contains("/subject=merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0-ReserveStockCommand-value")));
    }

    @Test
    public void test_principals_are_deduplicated_and_sanitized_name_collisions_fail_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> receiveBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        Map<String, Object> sendBinding = JSONPath.get(api, "$.operations['onReserveStockResponse'].bindings.kafka");
        receiveBinding.put("x-principal", "sales.orders");
        sendBinding.put("x-principal", "sales_orders");

        IllegalArgumentException collision = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> buildIntent("staging", context));
        Assertions.assertTrue(collision.getMessage().contains("sales.orders"));
        Assertions.assertTrue(collision.getMessage().contains("sales_orders"));
        Assertions.assertTrue(collision.getMessage().contains("both sanitize to 'sales_orders'"));
    }

    @Test
    public void test_unsupported_group_id_schema_skips_group_scoped_resources() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> receiveBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        receiveBinding.remove("x-groupId");
        receiveBinding.put("groupId", Map.of("type", "object", "properties", Map.of()));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertFalse(intent.acls.stream().anyMatch(a -> "GROUP".equals(a.resourceType)));
        Assertions.assertEquals(0, intent.topics.stream().filter(t -> t.isRetryOrDlq).count(), "unsupported groupId schema must skip retry/dlq expansion");
    }

    @Test
    public void test_retry_suffixes_override_numbered_suffixes() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> errorTopics = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka.x-error-topics");
        errorTopics.put("retrySuffixes", List.of("retry-5s", "retry-30s", "retry-5m"));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");
        List<String> retryTopicNames = intent.topics.stream()
                .filter(t -> t.isRetryOrDlq && !t.topicName.endsWith(".dlq"))
                .map(t -> t.topicName)
                .toList();

        Assertions.assertEquals(3, retryTopicNames.size());
        Assertions.assertTrue(retryTopicNames.stream().anyMatch(name -> name.endsWith(".retry-5s")));
        Assertions.assertTrue(retryTopicNames.stream().anyMatch(name -> name.endsWith(".retry-30s")));
        Assertions.assertTrue(retryTopicNames.stream().anyMatch(name -> name.endsWith(".retry-5m")));
        Assertions.assertFalse(retryTopicNames.stream().anyMatch(name -> name.matches(".*\\.retry-\\d+$")));
    }

    @Test
    public void test_invalid_retry_suffixes_fail_generation() throws Exception {
        IllegalArgumentException invalidType = assertInvalidRetrySuffixes("retry-5s", 3, false);
        Assertions.assertTrue(invalidType.getMessage().contains("array of strings"));

        IllegalArgumentException wrongLengthWithoutRetry = assertInvalidRetrySuffixes(List.of("retry-5s"), 3, true);
        Assertions.assertTrue(wrongLengthWithoutRetry.getMessage().contains("length must equal retryTopics"));

        IllegalArgumentException nonString = assertInvalidRetrySuffixes(List.of("retry-5s", 30, "retry-5m"), 3, false);
        Assertions.assertTrue(nonString.getMessage().contains("only strings"));

        IllegalArgumentException blank = assertInvalidRetrySuffixes(List.of("retry-5s", " ", "retry-5m"), 3, false);
        Assertions.assertTrue(blank.getMessage().contains("blank values"));

        IllegalArgumentException duplicate = assertInvalidRetrySuffixes(List.of("retry-5s", "retry-5s", "retry-5m"), 3, false);
        Assertions.assertTrue(duplicate.getMessage().contains("values must be unique"));
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

    @Test
    public void test_invalid_numeric_values_and_missing_group_or_principal_follow_fallback_paths() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> channelBinding = JSONPath.get(api, "$.channels['reserve-stock-command'].bindings.kafka");
        channelBinding.put("partitions", "not-a-number");
        channelBinding.put("replicas", "NaN");

        Map<String, Object> receiveBinding = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka");
        receiveBinding.remove("groupId");
        receiveBinding.put("retryTopics", "not-used");

        Map<String, Object> receiveErrorTopics = JSONPath.get(receiveBinding, "$.x-error-topics");
        receiveErrorTopics.put("retryTopics", "invalid");

        Map<String, Object> sendBinding = JSONPath.get(api, "$.operations['onReserveStockResponse'].bindings.kafka");
        sendBinding.remove("x-principal");

        context = buildIntent(null, context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        AsyncAPIOpsIntent.TopicIntent ownedTopic = intent.topics.stream()
                .filter(t -> "merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0".equals(t.topicName))
                .findFirst()
                .orElseThrow();
        Assertions.assertNull(ownedTopic.partitions);
        Assertions.assertNull(ownedTopic.replicationFactor);

        Assertions.assertEquals(0, intent.topics.stream().filter(t -> t.isRetryOrDlq).count(), "missing groupId must skip retry/dlq expansion");
        Assertions.assertFalse(intent.acls.stream().anyMatch(a ->
                "merchandising.inventory.inventory-adjustment.reserve-stock.response.avro.v0".equals(a.topicName)
                        && "Write".equals(a.operation)), "send operation without principal must not create ACLs");
    }

    @Test
    public void test_schema_resolution_fallbacks_support_inline_names_absolute_refs_and_missing_schema_objects() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);

        Map<String, Object> reserveStockCommand = JSONPath.get(api, "$.channels['reserve-stock-command'].messages['ReserveStockCommand']");
        reserveStockCommand.put("payload", new HashMap<>(Map.of(
                "schemaFormat", "application/vnd.apache.avro+json;version=1.9.0",
                "schema", new LinkedHashMap<>(Map.of("name", "InlineOnly"))
        )));

        Map<String, Object> reserveStockResponse = JSONPath.get(api, "$.channels['reserve-stock-response'].messages['ReserveStockResponse']");
        reserveStockResponse.put("payload", new HashMap<>(Map.of(
                "schemaFormat", "application/vnd.apache.avro+json;version=1.9.0",
                "schema", new LinkedHashMap<>(Map.of("x--original-$ref", "https://schemas.example.com/shared/ReserveStockResponse.avsc#/components/schemas/ReserveStockResponse"))
        )));

        Map<String, Object> inventoryAdjusted = JSONPath.get(api, "$.channels['inventory-adjusted'].messages['InventoryAdjustedEvent']");
        inventoryAdjusted.put("payload", new HashMap<>(Map.of(
                "schemaFormat", "application/vnd.apache.avro+json;version=1.9.0"
        )));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        AsyncAPIOpsIntent.SchemaIntent inlineSchema = intent.schemas.stream()
                .filter(s -> s.subject.endsWith("ReserveStockCommand-value"))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/avro/InlineOnly.avsc", inlineSchema.sourceSchemaUri);
        Assertions.assertEquals("asyncapi/avro/InlineOnly.avsc", inlineSchema.schemaFile);

        AsyncAPIOpsIntent.SchemaIntent absoluteRefSchema = intent.schemas.stream()
                .filter(s -> s.subject.endsWith("ReserveStockResponse-value"))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("https://schemas.example.com/shared/ReserveStockResponse.avsc", absoluteRefSchema.sourceSchemaUri);
        Assertions.assertEquals("asyncapi/ReserveStockResponse.avsc", absoluteRefSchema.schemaFile);

        AsyncAPIOpsIntent.SchemaIntent missingSchema = intent.schemas.stream()
                .filter(s -> s.subject.endsWith("InventoryAdjustedEvent-value"))
                .findFirst()
                .orElseThrow();
        Assertions.assertNull(missingSchema.sourceSchemaUri);
        Assertions.assertNull(missingSchema.schemaFile);
    }

    // -------------------------------------------------------------------------
    // x-kafka-streams-applications
    // -------------------------------------------------------------------------

    @Test
    public void test_streams_application_grants_prefixed_internal_topic_group_and_transactional_acls() throws Exception {
        Map<String, Object> context = loadAndBuildIntent(null, ASYNCAPI_STREAMS);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        List<AsyncAPIOpsIntent.AclIntent> internalTopicAcls = intent.acls.stream()
                .filter(a -> "TOPIC".equals(a.resourceType) && "PREFIXED".equals(a.patternType))
                .toList();

        Assertions.assertEquals(7, internalTopicAcls.size(), "7 fixed operations on the application's own namespace");
        Assertions.assertEquals(
                Set.of("Create", "Delete", "Alter", "AlterConfigs", "Describe", "Read", "Write"),
                internalTopicAcls.stream().map(a -> a.operation).collect(Collectors.toSet()));
        internalTopicAcls.forEach(a -> {
            Assertions.assertEquals(STREAMS_APPLICATION_ID + "-", a.kafkaResourceName, "grant is scoped to <application.id>-");
            Assertions.assertEquals("merchandising.inventory.inventory-adjustment", a.principal);
            Assertions.assertEquals("Topic", a.kafkaResourceType);
            Assertions.assertEquals("Prefixed", a.kafkaPatternType);
        });

        // Confluent spelling is derived from the Mongey spelling, including the multi-word case
        Assertions.assertEquals("ALTER_CONFIGS", internalTopicAcls.stream()
                .filter(a -> "AlterConfigs".equals(a.operation)).findFirst().orElseThrow().confluentOperation);
        Assertions.assertEquals("READ", internalTopicAcls.stream()
                .filter(a -> "Read".equals(a.operation)).findFirst().orElseThrow().confluentOperation);

        Assertions.assertTrue(intent.acls.stream().anyMatch(a ->
                "GROUP".equals(a.resourceType)
                        && "LITERAL".equals(a.patternType)
                        && STREAMS_APPLICATION_ID.equals(a.kafkaResourceName)
                        && "Read".equals(a.operation)));

        List<String> transactionalOperations = intent.acls.stream()
                .filter(a -> "TRANSACTIONAL_ID".equals(a.resourceType))
                .peek(a -> {
                    Assertions.assertEquals("PREFIXED", a.patternType);
                    Assertions.assertEquals(STREAMS_APPLICATION_ID + "-", a.kafkaResourceName);
                })
                .map(a -> a.operation).sorted().toList();
        Assertions.assertEquals(List.of("Describe", "Write"), transactionalOperations);

        Assertions.assertEquals(1, intent.principals.size(), "streams principal is deduplicated with the operation principal");
        // Authorization only — the extension declares no internal topics
        Assertions.assertEquals(1, intent.topics.size(), "only the source channel topic, no internal topics");
        Assertions.assertEquals("merchandising.inventory.stock-movement.event.avro.v0", intent.topics.get(0).topicName);
    }

    @Test
    public void test_streams_topics_mode_replaces_the_prefixed_grant_with_one_literal_grant_per_topic() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        streamsApplication(context, STREAMS_APPLICATION_ID).put("topics", List.of(
                STREAMS_APPLICATION_ID + "-by-sku-store-changelog",
                STREAMS_APPLICATION_ID + "-by-sku-repartition"));

        context = buildIntent(null, context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertFalse(intent.acls.stream().anyMatch(a -> "TOPIC".equals(a.resourceType) && "PREFIXED".equals(a.patternType)),
                "topics mode is an alternative to the prefix grant, not an addition");

        List<AsyncAPIOpsIntent.AclIntent> literalAcls = intent.acls.stream()
                .filter(a -> "TOPIC".equals(a.resourceType) && a.kafkaResourceName.startsWith(STREAMS_APPLICATION_ID + "-"))
                .toList();
        Assertions.assertEquals(14, literalAcls.size(), "7 operations × 2 explicitly listed topics");
        literalAcls.forEach(a -> Assertions.assertEquals("LITERAL", a.patternType));
        Assertions.assertEquals(
                Set.of(STREAMS_APPLICATION_ID + "-by-sku-store-changelog", STREAMS_APPLICATION_ID + "-by-sku-repartition"),
                literalAcls.stream().map(a -> a.kafkaResourceName).collect(Collectors.toSet()));

        Assertions.assertTrue(intent.acls.stream().anyMatch(a -> "GROUP".equals(a.resourceType)), "group grant is unaffected");
    }

    @Test
    public void test_streams_empty_topics_list_suppresses_topic_acls_but_keeps_group_and_transactional_acls() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        streamsApplication(context, STREAMS_APPLICATION_ID).put("topics", List.of());

        context = buildIntent(null, context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertFalse(intent.acls.stream().anyMatch(a ->
                        "TOPIC".equals(a.resourceType) && a.kafkaResourceName.startsWith(STREAMS_APPLICATION_ID)),
                "an explicit empty list generates no TOPIC ACL at all");
        Assertions.assertTrue(intent.acls.stream().anyMatch(a -> "GROUP".equals(a.resourceType)));
        Assertions.assertTrue(intent.acls.stream().anyMatch(a -> "TRANSACTIONAL_ID".equals(a.resourceType)));
    }

    @Test
    public void test_streams_application_without_matching_receive_group_fails_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Map<String, Object> receiveBinding = JSONPath.get(getApis(context).get(0), "$.operations['processStockEvents'].bindings.kafka");
        receiveBinding.put("x-groupId", "merchandising.inventory.inventory-adjustment.some-other-app");

        IllegalArgumentException orphaned = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(orphaned.getMessage().contains("does not match the groupId/x-groupId of any receive operation"));
        Assertions.assertTrue(orphaned.getMessage().contains(STREAMS_APPLICATION_ID));
    }

    @Test
    public void test_invalid_streams_application_declarations_fail_generation() throws Exception {
        IllegalArgumentException missingPrincipal = assertInvalidStreamsApplication(app -> app.remove("x-principal"));
        Assertions.assertTrue(missingPrincipal.getMessage().contains("missing a non-blank x-principal"));

        IllegalArgumentException blankPrefix = assertInvalidStreamsApplication(
                app -> app.put("x-transactionalIdPrefix", "  "));
        Assertions.assertTrue(blankPrefix.getMessage().contains("must be a non-blank string when present"));

        IllegalArgumentException topicsNotAList = assertInvalidStreamsApplication(
                app -> app.put("topics", STREAMS_APPLICATION_ID + "-by-sku-changelog"));
        Assertions.assertTrue(topicsNotAList.getMessage().contains("must be an array of strings"));

        IllegalArgumentException blankTopic = assertInvalidStreamsApplication(app -> app.put("topics", List.of("  ")));
        Assertions.assertTrue(blankTopic.getMessage().contains("only non-blank strings"));

        // Kafka Streams always prefixes internal topics with application.id plus a separator
        IllegalArgumentException outsideNamespace = assertInvalidStreamsApplication(
                app -> app.put("topics", List.of("merchandising.inventory.stock-movement.event.avro.v0")));
        Assertions.assertTrue(outsideNamespace.getMessage().contains("is outside the application namespace"));
    }

    @Test
    public void test_streams_topic_listed_under_two_applications_fails_generation() throws Exception {
        String nestedApplicationId = STREAMS_APPLICATION_ID + "-sub";
        String sharedTopic = nestedApplicationId + "-by-sku-changelog";

        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);

        // A nested application.id keeps the shared topic inside both namespaces, so the ownership
        // check is what rejects it rather than the namespace check.
        streamsApplication(context, STREAMS_APPLICATION_ID).put("topics", List.of(sharedTopic));
        streamsApplications(context).put(nestedApplicationId, new LinkedHashMap<>(Map.of(
                "x-principal", "merchandising.inventory.inventory-adjustment",
                "topics", List.of(sharedTopic))));
        addReceiveOperation(api, "processStockEventsNested", nestedApplicationId);

        IllegalArgumentException duplicateTopic = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(duplicateTopic.getMessage().contains("is listed under both"));
        Assertions.assertTrue(duplicateTopic.getMessage().contains(sharedTopic));
    }

    @Test
    public void test_streams_application_id_prefixing_a_channel_address_fails_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);

        // "merchandising.inventory.stock-" also matches the stock-movement channel address
        String collidingApplicationId = "merchandising.inventory.stock";
        streamsApplications(context).put(collidingApplicationId, new LinkedHashMap<>(Map.of(
                "x-principal", "merchandising.inventory.inventory-adjustment")));
        addReceiveOperation(api, "processStockEventsColliding", collidingApplicationId);

        IllegalArgumentException collision = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(collision.getMessage().contains("would grant a PREFIXED ACL on 'merchandising.inventory.stock-'"));
        Assertions.assertTrue(collision.getMessage().contains("merchandising.inventory.stock-movement.event.avro.v0"));
    }

    @Test
    public void test_streams_application_id_sharing_a_dot_separated_stem_with_channels_is_not_a_collision() throws Exception {
        // The realistic case: application.id equals the service prefix and every channel address
        // continues with '.', so the granted 'merchandising.inventory.inventory-adjustment-' prefix
        // matches none of them.
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);
        api.model().put("x-kafka-streams-applications", new LinkedHashMap<>(Map.of(
                "merchandising.inventory.inventory-adjustment", new LinkedHashMap<>(Map.of(
                        "x-principal", "merchandising.inventory.inventory-adjustment")))));

        context = buildIntent("staging", context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertEquals(7, intent.acls.stream()
                .filter(a -> "TOPIC".equals(a.resourceType) && "PREFIXED".equals(a.patternType))
                .peek(a -> Assertions.assertEquals("merchandising.inventory.inventory-adjustment-", a.kafkaResourceName))
                .count());
    }

    @Test
    public void test_streams_unprefixed_extension_is_supported() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);
        api.model().put("kafka-streams-applications", api.model().remove("x-kafka-streams-applications"));

        context = buildIntent(null, context);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertEquals(7, intent.acls.stream()
                .filter(a -> "TOPIC".equals(a.resourceType) && "PREFIXED".equals(a.patternType)).count());
    }

    @Test
    public void test_streams_overlapping_application_namespaces_fail_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);

        // 'merchandising...streams-app-' also matches every internal topic of the nested application,
        // so the first application could delete the second one's changelog topics.
        String nestedApplicationId = STREAMS_APPLICATION_ID + "-rebuild";
        streamsApplications(context).put(nestedApplicationId, new LinkedHashMap<>(Map.of(
                "x-principal", "merchandising.inventory.inventory-adjustment")));
        addReceiveOperation(api, "processStockEventsRebuild", nestedApplicationId);

        IllegalArgumentException overlap = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(overlap.getMessage().contains("internal topic namespace"));
        Assertions.assertTrue(overlap.getMessage().contains(nestedApplicationId + "-"));
        Assertions.assertTrue(overlap.getMessage().contains("Create/Alter/Delete"));
    }

    @Test
    public void test_streams_prefix_covering_a_generated_retry_topic_fails_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);

        // A '-' separator after ${groupId} puts the generated retry/DLQ topics inside the
        // application's own internal topic namespace, where Kafka Streams believes it owns every name.
        Map<String, Object> receiveBinding = JSONPath.get(api, "$.operations['processStockEvents'].bindings.kafka");
        receiveBinding.put("x-error-topics", new LinkedHashMap<>(Map.of(
                "addressTemplate", "${groupId}-${suffix}",
                "retryTopics", 1,
                "retry", new LinkedHashMap<>(Map.of("partitions", 1)),
                "dlq", new LinkedHashMap<>(Map.of("partitions", 1)))));

        IllegalArgumentException overlap = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(overlap.getMessage().contains("which also matches topic '"));
        Assertions.assertTrue(overlap.getMessage().contains(STREAMS_APPLICATION_ID + "-retry-0")
                        || overlap.getMessage().contains(STREAMS_APPLICATION_ID + "-dlq"),
                "a generated retry/DLQ topic must be reported: " + overlap.getMessage());
    }

    @Test
    public void test_streams_prefix_overlapping_an_unresolved_channel_template_fails_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        Model api = getApis(context).get(0);

        // Unbounded parameter: no concrete address is known, but 'merchandising.inventory.stock-{tenant}'
        // can expand to names inside a 'merchandising.inventory.stock-' namespace.
        Map<String, Object> channel = JSONPath.get(api, "$.channels['stock-movement-event']");
        channel.put("address", "merchandising.inventory.stock-{tenant}");
        channel.put("parameters", new LinkedHashMap<>(Map.of("tenant", new LinkedHashMap<>())));

        String collidingApplicationId = "merchandising.inventory.stock";
        streamsApplications(context).put(collidingApplicationId, new LinkedHashMap<>(Map.of(
                "x-principal", "merchandising.inventory.inventory-adjustment")));
        addReceiveOperation(api, "processStockEventsTenant", collidingApplicationId);

        IllegalArgumentException overlap = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(overlap.getMessage().contains("unresolved channel address"));
        Assertions.assertTrue(overlap.getMessage().contains("merchandising.inventory.stock-{tenant}"));
    }

    @Test
    public void test_streams_application_declared_identically_in_two_specs_generates_one_set_of_grants() throws Exception {
        Map<String, Object> context = loadAndBuildIntent(null, ASYNCAPI_STREAMS, ASYNCAPI_STREAMS_SECOND);
        AsyncAPIOpsIntent intent = (AsyncAPIOpsIntent) context.get("intent");

        Assertions.assertEquals(2, intent.topics.size(), "each spec contributes its own source channel topic");
        Assertions.assertEquals(7, intent.acls.stream()
                        .filter(a -> "TOPIC".equals(a.resourceType) && "PREFIXED".equals(a.patternType)).count(),
                "the same application declared in both specs yields one set of internal topic grants");
        Assertions.assertEquals(1, intent.principals.size());
    }

    @Test
    public void test_streams_application_declared_with_conflicting_values_across_specs_fails_generation() throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS, ASYNCAPI_STREAMS_SECOND);
        streamsApplication(context, 1, STREAMS_APPLICATION_ID).put("x-principal", "merchandising.inventory.someone-else");

        IllegalArgumentException conflict = Assertions.assertThrows(
                IllegalArgumentException.class, () -> buildIntent(null, context));
        Assertions.assertTrue(conflict.getMessage().contains("conflicting values in more than one spec"));
        Assertions.assertTrue(conflict.getMessage().contains(STREAMS_APPLICATION_ID));
    }

    private Map<String, Object> streamsApplications(Map<String, Object> context) {
        return streamsApplications(context, 0);
    }

    private Map<String, Object> streamsApplications(Map<String, Object> context, int specIndex) {
        return JSONPath.get(getApis(context).get(specIndex), "$.x-kafka-streams-applications");
    }

    private Map<String, Object> streamsApplication(Map<String, Object> context, String applicationId) {
        return streamsApplication(context, 0, applicationId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> streamsApplication(Map<String, Object> context, int specIndex, String applicationId) {
        return (Map<String, Object>) streamsApplications(context, specIndex).get(applicationId);
    }

    /** Applies an invalid mutation to the sole streams application and expects generation to fail. */
    private IllegalArgumentException assertInvalidStreamsApplication(Consumer<Map<String, Object>> invalidate) throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_STREAMS);
        invalidate.accept(streamsApplication(context, STREAMS_APPLICATION_ID));
        return Assertions.assertThrows(IllegalArgumentException.class, () -> buildIntent(null, context));
    }

    /** Adds a second receive operation on the existing channel, so {@code groupId} resolves in-scope. */
    @SuppressWarnings("unchecked")
    private void addReceiveOperation(Model api, String operationId, String groupId) {
        Map<String, Object> operations = JSONPath.get(api, "$.operations");
        Map<String, Object> channel = JSONPath.get(api, "$.channels['stock-movement-event']");
        operations.put(operationId, new LinkedHashMap<>(Map.of(
                "action", "receive",
                "channel", channel,
                "bindings", Map.of("kafka", Map.of(
                        "x-principal", "merchandising.inventory.inventory-adjustment",
                        "x-groupId", groupId)))));
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

    private IllegalArgumentException assertInvalidRetrySuffixes(Object retrySuffixes, int retryTopics, boolean removeRetry) throws Exception {
        Map<String, Object> context = loadContext(ASYNCAPI_PROVIDER);
        Model api = getApis(context).get(0);
        Map<String, Object> errorTopics = JSONPath.get(api, "$.operations['doReserveStockCommand'].bindings.kafka.x-error-topics");
        errorTopics.put("retryTopics", retryTopics);
        errorTopics.put("retrySuffixes", retrySuffixes);
        if (removeRetry) {
            errorTopics.remove("retry");
        }
        return Assertions.assertThrows(IllegalArgumentException.class, () -> buildIntent("staging", context));
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
