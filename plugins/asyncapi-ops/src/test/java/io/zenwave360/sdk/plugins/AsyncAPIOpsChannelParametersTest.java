package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel address parameter provisioning: enumerable parameters expand into one resource set per
 * value, {@code parameterValues} restricts that expansion per invocation, and channels with an
 * unbounded (no {@code enum}) parameter are skipped entirely.
 */
public class AsyncAPIOpsChannelParametersTest {

    static final String ASYNCAPI_PARAMETERS = "classpath:parameters/asyncapi-parameters.yml";

    // -------------------------------------------------------------------------
    // Requirement 1 — enumerable parameter expansion
    // -------------------------------------------------------------------------

    @Test
    public void test_enum_parameter_expands_to_one_topic_per_value_without_cli_input() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        Assertions.assertEquals(
                List.of("orders.apac.created", "orders.eu.created", "orders.us.created"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".created")).sorted().toList());

        // enum declaration order is preserved, not sorted
        Assertions.assertEquals(
                List.of("orders.eu.created", "orders.us.created", "orders.apac.created"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".created")).toList());

        // default: eu does not restrict anything — the full enum is the source of truth
        Assertions.assertEquals(12, ownedTopicNames(intent).size(),
                "3 (orderCreated) + 2 (userSignedUp) + 6 (orderShipped) + 1 (orderCancelled)");

        // channels without parameters are unaffected
        Assertions.assertTrue(ownedTopicNames(intent).contains("orders.cancelled"));
    }

    @Test
    public void test_resource_names_incorporate_the_resolved_parameter_value() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        List<String> resourceNames = intent.topics.stream().map(t -> t.resourceName).toList();
        Assertions.assertEquals(resourceNames.size(), resourceNames.stream().distinct().count(),
                "every expansion must get a unique Terraform resource name");
        Assertions.assertTrue(resourceNames.contains("orders_eu_created"));
        Assertions.assertTrue(resourceNames.contains("orders_us_created"));
        Assertions.assertTrue(resourceNames.contains("orders_apac_created"));
        resourceNames.forEach(name -> {
            Assertions.assertFalse(name.contains("."), "resource name must not contain dots: " + name);
            Assertions.assertFalse(name.contains("{"), "resource name must not contain unresolved expressions: " + name);
        });
    }

    @Test
    public void test_expansion_applies_to_schemas_acls_role_bindings_and_error_topics() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        // one Schema Registry subject per resolved address, all reusing the same bundled file
        Assertions.assertEquals(
                List.of("orders.apac.created-OrderCreated-value",
                        "orders.eu.created-OrderCreated-value",
                        "orders.us.created-OrderCreated-value"),
                intent.schemas.stream().map(s -> s.subject).sorted().toList());
        Assertions.assertEquals(1, intent.schemas.stream().map(s -> s.schemaFile).distinct().count());

        // topic ACLs per resolved address
        Assertions.assertEquals(3, intent.acls.stream()
                .filter(a -> "Read".equals(a.operation) && a.topicName != null && a.topicName.endsWith(".created"))
                .count());

        // role bindings per resolved subject
        Assertions.assertEquals(3, intent.roleBindings.stream()
                .filter(r -> r.crnPattern.contains("-OrderCreated-value")).count());

        // ${channel.address} in the error topic template resolves per address: 3 x (1 retry + 1 dlq)
        Assertions.assertEquals(6, intent.topics.stream().filter(t -> t.isRetryOrDlq).count());
        Assertions.assertTrue(intent.topics.stream().anyMatch(t ->
                "sales.orders.__.orders.eu.created.dlq".equals(t.topicName)));
        Assertions.assertTrue(intent.topics.stream().anyMatch(t ->
                "sales.orders.__.orders.apac.created.retry-0".equals(t.topicName)));
    }

    @Test
    public void test_address_independent_acls_are_emitted_once_per_operation() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        Assertions.assertEquals(1, intent.acls.stream()
                .filter(a -> "GROUP".equals(a.resourceType) && "sales.orders".equals(a.kafkaResourceName))
                .count(), "the consumer group is not address-scoped");
    }

    @Test
    public void test_multiple_enumerable_parameters_expand_as_a_cross_product() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        Assertions.assertEquals(
                List.of("orders.apac.acme.shipped", "orders.apac.globex.shipped",
                        "orders.eu.acme.shipped", "orders.eu.globex.shipped",
                        "orders.us.acme.shipped", "orders.us.globex.shipped"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".shipped")).sorted().toList(),
                "a $ref'd parameter and an inlined one expand together");
    }

    // -------------------------------------------------------------------------
    // Requirement 2 — CLI override
    // -------------------------------------------------------------------------

    @Test
    public void test_bare_parameter_values_restricts_every_channel_using_that_name() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of(
                "region", List.of("eu", "us"),
                "userSignedUp", Map.of("region", "dev")));

        Assertions.assertEquals(List.of("orders.eu.created", "orders.us.created"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".created")).toList());
        Assertions.assertEquals(4, ownedTopicNames(intent).stream().filter(name -> name.endsWith(".shipped")).count(),
                "the bare form also restricts the cross product on orderShipped");
        Assertions.assertEquals(List.of("users.dev.signedup"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".signedup")).toList());
    }

    @Test
    public void test_channel_scoped_value_overrides_the_bare_form_for_that_channel_only() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of(
                "region", "eu",
                "orderCreated", Map.of("region", List.of("us", "apac")),
                "userSignedUp", Map.of("region", "staging")));

        Assertions.assertEquals(List.of("orders.us.created", "orders.apac.created"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".created")).toList(),
                "the scoped form wins for orderCreated");
        Assertions.assertEquals(List.of("orders.eu.acme.shipped", "orders.eu.globex.shipped"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".shipped")).toList(),
                "orderShipped still uses the bare form");
    }

    @Test
    public void test_selected_values_are_emitted_in_enum_declaration_order() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of(
                "orderCreated", Map.of("region", List.of("apac", "eu"))));

        Assertions.assertEquals(List.of("orders.eu.created", "orders.apac.created"),
                ownedTopicNames(intent).stream().filter(name -> name.endsWith(".created")).toList(),
                "output order follows the spec enum, not the order values were typed on the CLI");
    }

    @Test
    public void test_value_outside_a_channel_enum_fails_with_a_channel_specific_message() throws Exception {
        IllegalArgumentException bare = Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildIntent(Map.of("region", "eu")));
        Assertions.assertEquals(
                "parameterValues.region=eu is not valid for channel userSignedUp (allowed: dev, staging)",
                bare.getMessage(),
                "validation is per-channel: the same parameter name means different things per channel");

        IllegalArgumentException scoped = Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildIntent(Map.of("userSignedUp", Map.of("region", "production"))));
        Assertions.assertEquals(
                "parameterValues.userSignedUp.region=production is not valid for channel userSignedUp (allowed: dev, staging)",
                scoped.getMessage());
    }

    @Test
    public void test_empty_value_list_fails_instead_of_silently_generating_nothing() throws Exception {
        IllegalArgumentException empty = Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildIntent(Map.of("orderCreated", Map.of("region", ""))));
        Assertions.assertTrue(empty.getMessage().contains("parameterValues.orderCreated.region is empty"),
                empty.getMessage());
    }

    @Test
    public void test_parameter_values_are_parsed_from_the_dotted_cli_convention() {
        AsyncAPIOpsGeneratorPlugin plugin = new AsyncAPIOpsGeneratorPlugin();
        plugin.withOption("parameterValues.region", "eu,us");
        plugin.withOption("parameterValues.orderCreated.region", "eu");

        Assertions.assertEquals(
                Map.of("region", List.of("eu", "us"), "orderCreated", Map.of("region", "eu")),
                plugin.getOptions().get("parameterValues"),
                "the bare form nests one level, the channel-scoped form two");
    }

    // -------------------------------------------------------------------------
    // Requirement 3 — unbounded parameters
    // -------------------------------------------------------------------------

    @Test
    public void test_unbounded_parameter_skips_topic_schema_and_acls_without_failing_the_run() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        Assertions.assertTrue(intent.topics.stream().noneMatch(t -> t.topicName.contains("streetlight")),
                "no topic can be provisioned for an address that is only known at runtime");
        Assertions.assertTrue(intent.schemas.stream().noneMatch(s -> s.subject.contains("streetlight")));
        Assertions.assertTrue(intent.acls.stream().noneMatch(a -> "smartylighting.streetlights".equals(a.principal)),
                "no ACL either: a PREFIXED grant could leak a sibling channel's topics");
        Assertions.assertTrue(intent.principals.stream().noneMatch(p -> "smartylighting.streetlights".equals(p.name)));

        // the rest of the run still produced resources
        Assertions.assertFalse(intent.topics.isEmpty());

        Assertions.assertEquals(1, intent.skippedChannels.size());
        AsyncAPIOpsIntent.SkippedChannelIntent skipped = intent.skippedChannels.get(0);
        Assertions.assertEquals("streetlightMeasured", skipped.channelKey);
        Assertions.assertEquals("streetlight.{streetlightId}.measured", skipped.address);
        Assertions.assertEquals("streetlightId", skipped.parameterName);
    }

    @Test
    public void test_channel_scoped_values_make_an_unbounded_parameter_concrete() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of(
                "streetlightMeasured", Map.of("streetlightId", List.of("1", "2"))));

        Assertions.assertEquals(
                List.of("streetlight.1.measured", "streetlight.2.measured"),
                ownedTopicNames(intent).stream().filter(name -> name.startsWith("streetlight.")).toList(),
                "naming the channel explicitly supplies the values the spec does not declare");
        Assertions.assertTrue(intent.skippedChannels.isEmpty(), "the channel is no longer skipped");

        // the channel now participates fully: ACLs and its principal come back
        Assertions.assertEquals(List.of("streetlight.1.measured", "streetlight.2.measured"),
                intent.acls.stream()
                        .filter(a -> "Write".equals(a.operation) && "smartylighting.streetlights".equals(a.principal))
                        .map(a -> a.topicName).toList());
        Assertions.assertTrue(intent.principals.stream().anyMatch(p -> "smartylighting.streetlights".equals(p.name)));
    }

    @Test
    public void test_unbounded_values_keep_the_order_they_were_supplied_in() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of(
                "streetlightMeasured", Map.of("streetlightId", List.of("b", "a"))));

        Assertions.assertEquals(
                List.of("streetlight.b.measured", "streetlight.a.measured"),
                ownedTopicNames(intent).stream().filter(name -> name.startsWith("streetlight.")).toList(),
                "with no enum there is no declaration order to fall back to");
    }

    @Test
    public void test_bare_values_are_not_applied_to_an_unbounded_parameter() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of("streetlightId", List.of("1", "2")));

        Assertions.assertTrue(intent.topics.stream().noneMatch(t -> t.topicName.contains("streetlight")),
                "the bare form would reach every channel using that name across every spec, "
                        + "with no enum to catch a mistake");
        Assertions.assertEquals(1, intent.skippedChannels.size());
        Assertions.assertEquals("streetlightMeasured", intent.skippedChannels.get(0).channelKey);
    }

    @Test
    public void test_empty_scoped_value_for_an_unbounded_parameter_fails() throws Exception {
        IllegalArgumentException empty = Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildIntent(Map.of("streetlightMeasured", Map.of("streetlightId", ""))));
        Assertions.assertEquals(
                "parameterValues.streetlightMeasured.streetlightId is empty for channel streetlightMeasured"
                        + " — supply at least one value",
                empty.getMessage(),
                "no enum means no 'allowed' set to report");
    }

    // -------------------------------------------------------------------------
    // env / server= is unaffected
    // -------------------------------------------------------------------------

    @Test
    public void test_server_overrides_still_apply_to_every_expansion() throws Exception {
        AsyncAPIOpsIntent intent = buildIntent(Map.of());

        intent.topics.stream()
                .filter(t -> !t.isRetryOrDlq && t.topicName.endsWith(".created"))
                .forEach(t -> {
                    Assertions.assertEquals(2, t.partitions, "staging x-env-server-overrides for " + t.topicName);
                    Assertions.assertEquals(3, t.replicationFactor);
                });
    }

    // -------------------------------------------------------------------------
    // End to end generation
    // -------------------------------------------------------------------------

    @Test
    public void test_terraform_generation_expands_topics_and_bundles_each_schema_file_once() throws Exception {
        String targetFolder = "target/out/test_channel_parameters_generation";
        new io.zenwave360.sdk.MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                .withApiFile(ASYNCAPI_PARAMETERS)
                .withOption("server", "staging")
                .withOption("templates", "TerraformKafka")
                .withOption("parameterValues.region", "eu,us")
                .withOption("parameterValues.userSignedUp.region", "dev")
                .withTargetFolder(targetFolder)
                .withOption("skipFormatting", true));

        String topics = java.nio.file.Files.readString(java.nio.file.Path.of(targetFolder + "/topics.tf"));
        Assertions.assertTrue(topics.contains("resource \"kafka_topic\" \"orders_eu_created\""));
        Assertions.assertTrue(topics.contains("resource \"kafka_topic\" \"orders_us_created\""));
        Assertions.assertFalse(topics.contains("orders_apac_created"), "apac was excluded by parameterValues.region");
        Assertions.assertTrue(topics.contains("resource \"kafka_topic\" \"users_dev_signedup\""));
        Assertions.assertFalse(topics.contains("users_staging_signedup"), "the channel-scoped override wins for userSignedUp");
        Assertions.assertFalse(topics.contains("streetlight"), "unbounded parameter channel is not provisioned");
        Assertions.assertFalse(java.util.regex.Pattern.compile("name\\s+=\\s+\"[^\"]*\\{").matcher(topics).find(),
                "no unresolved parameter expression may reach a topic name");

        // one subject per resolved address, all pointing at the same bundled schema file
        String schemas = java.nio.file.Files.readString(java.nio.file.Path.of(targetFolder + "/schemas.tf"));
        Assertions.assertTrue(schemas.contains("subject             = \"orders.eu.created-OrderCreated-value\""));
        Assertions.assertTrue(schemas.contains("subject             = \"orders.us.created-OrderCreated-value\""));
        Assertions.assertEquals(2, schemas.split("asyncapi-parameters/avro/OrderCreated\\.avsc", -1).length - 1);
        Assertions.assertTrue(new java.io.File(targetFolder + "/asyncapi-parameters/avro/OrderCreated.avsc").exists());

        String acls = java.nio.file.Files.readString(java.nio.file.Path.of(targetFolder + "/acls.tf"));
        Assertions.assertTrue(acls.contains("resource_name       = \"orders.eu.created\""));
        Assertions.assertTrue(acls.contains("resource_name       = \"orders.us.created\""));
        Assertions.assertFalse(acls.contains("smartylighting"), "no ACL for a channel with an unbounded parameter");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AsyncAPIOpsIntent buildIntent(Map<String, Object> parameterValues) throws Exception {
        AsyncAPIOpsSpecLoader loader = new AsyncAPIOpsSpecLoader();
        loader.apiFiles = List.of(URI.create(ASYNCAPI_PARAMETERS));
        Map<String, Object> context = loader.process(new LinkedHashMap<>());

        AsyncAPIOpsIntentProcessor processor = new AsyncAPIOpsIntentProcessor();
        processor.server = "staging";
        processor.parameterValues = new LinkedHashMap<>(parameterValues);
        return (AsyncAPIOpsIntent) processor.process(context).get("intent");
    }

    private List<String> ownedTopicNames(AsyncAPIOpsIntent intent) {
        return intent.topics.stream().filter(t -> !t.isRetryOrDlq).map(t -> t.topicName).toList();
    }
}
