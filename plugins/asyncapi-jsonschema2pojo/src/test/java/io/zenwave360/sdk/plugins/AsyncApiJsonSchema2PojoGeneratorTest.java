package io.zenwave360.sdk.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.jsonrefparser.$Refs;
import io.zenwave360.jsonrefparser.parser.Parser;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.parsers.Model;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;

public class AsyncApiJsonSchema2PojoGeneratorTest {

    @BeforeEach
    public void setup() throws IOException {
        FileUtils.deleteDirectory(new File("target/zenwave630"));
    }

    @Test
    public void test_generator_for_asyncapi_v3() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("generatedAnnotationClass", "org.springframework.aot.generate.Generated")
                .withOption("modelPackage", "io.example.v3.domain.events");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v3/domain/events/CustomerInput.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/mypackage/CustomerCreated.java").exists());
        Assertions.assertFalse(new File("target/zenwave630/src/main/java/io/example/v3/domain/events/CustomerCreated.java").exists());
    }

    @Test
    public void test_generator_for_asyncapi_v3_with_overlays() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("apiOverlayFiles", List.of("classpath:asyncapi-v3-overlay.yml"))
                .withOption("modelPackage", "io.example.v3.domain.events");

        new MainGenerator().generate(plugin);

        File customerEvent = new File("target/zenwave630/src/main/java/io/example/v3/domain/events/CustomerEvent.java");
        Assertions.assertTrue(customerEvent.exists());
        Assertions.assertTrue(FileUtils.readFileToString(customerEvent, "UTF-8").contains("externalId"));
    }

    @Test
    public void test_generator_for_asyncapi_schemas_with_delimiters() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.integration.test.with_schemas.model");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/DepartmentV1.java").exists());
    }

    @Test
    public void test_generator_for_asyncapi_v31_schema_names_with_underscores() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-schema-name-underscores.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31.domain.events")
                .withOption("jsonschema2pojo.propertyWordDelimiters", "_-");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/AddressC.java").exists());
        Assertions.assertFalse(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/Address_c.java").exists());
        String samplePayload = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/SamplePayload.java"), "UTF-8");
        Assertions.assertEquals(3, countOccurrences(samplePayload, "@JsonProperty(\"address\")")); // field, getter and setter
    }

    @Test
    public void test_generator_for_asyncapi_v31_external_json_schema_defs_with_underscores() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-external-json-schema-name-underscores.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31_external.domain.events");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v31_external/domain/events/AddressC.java").exists());
        Assertions.assertFalse(new File("target/zenwave630/src/main/java/io/example/v31_external/domain/events/Address.java").exists());
        String samplePayload = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31_external/domain/events/SampleMessage.java"), "UTF-8");
        Assertions.assertEquals(3, countOccurrences(samplePayload, "@JsonProperty(\"address\")")); // field, getter and setter
    }

    @Test
    public void test_convert_to_json_uses_defs_original_ref_for_java_type() throws Exception {
        AsyncApiJsonSchema2PojoGenerator generator = new AsyncApiJsonSchema2PojoGenerator();
        JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(Map.of("propertyWordDelimiters", "_-"));
        Model apiModel = new Model(URI.create("file:///tmp/asyncapi.yml"), new $Refs(Parser.parse("{}")));

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("type", "object");
        address.put("x--original-$ref", "#/$defs/Address_c");
        properties.put("address", address);
        payload.put("type", "object");
        payload.put("properties", properties);

        String json = generator.convertToJson(apiModel, config, payload, "io.example.v31_external.domain.events");

        Assertions.assertTrue(json.contains("\"javaType\":\"io.example.v31_external.domain.events.AddressC\""));
        Assertions.assertEquals("#/$defs/Address_c", address.get("x--original-$ref"));
    }

    @Test
    public void test_generator_honors_initialize_collections_false() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-jsonschema2pojo-options.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31_options.domain.events")
                .withOption("messageNames", List.of("SampleMessage"))
                .withOption("jsonschema2pojo.initializeCollections", "false");

        new MainGenerator().generate(plugin);

        String samplePayload = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31_options/domain/events/SamplePayload.java"), "UTF-8");
        String address = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31_options/domain/events/Address.java"), "UTF-8");
        Assertions.assertFalse(samplePayload.contains("private List<Address> addresses = new ArrayList<Address>();"));
        Assertions.assertFalse(address.contains("private List<String> cities = new ArrayList<String>();"));
        Assertions.assertFalse(samplePayload.contains("new ArrayList"));
        Assertions.assertFalse(address.contains("new ArrayList"));
    }

    @Test
    public void test_generator_honors_use_joda_local_dates_false() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-jsonschema2pojo-options.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31_options.domain.events")
                .withOption("messageNames", List.of("SampleMessage"))
                .withOption("jsonschema2pojo.useJodaLocalDates", "false")
                .withOption("jsonschema2pojo.useJodaLocalTimes", "false");

        new MainGenerator().generate(plugin);

        String samplePayload = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31_options/domain/events/SamplePayload.java"), "UTF-8");
        String address = FileUtils.readFileToString(new File("target/zenwave630/src/main/java/io/example/v31_options/domain/events/Address.java"), "UTF-8");
        Assertions.assertTrue(samplePayload.contains("private String localDate;"));
        Assertions.assertTrue(samplePayload.contains("private String localTime;"));
        Assertions.assertTrue(address.contains("private String localDate;"));
        Assertions.assertTrue(address.contains("private String localTime;"));
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) != -1) {
            count++;
            index += token.length();
        }
        return count;
    }

    @Test
    public void test_generator_for_asyncapi_v31_schema_titles_and_prefix_suffix() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-schema-title-prefix-suffix.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31.domain.events")
                .withOption("jsonschema2pojo.useTitleAsClassname", "true")
                .withOption("jsonschema2pojo.classNamePrefix", "Api")
                .withOption("jsonschema2pojo.classNameSuffix", "Dto")
                .withOption("jsonschema2pojo.propertyWordDelimiters", "_-");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/ApiPostalAddressDto.java").exists());
        Assertions.assertFalse(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/ApiAddressCDto.java").exists());
    }

    @Test
    public void test_generator_for_asyncapi_v3_filter_messages() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("messageNames", List.of("CustomerInputMessage"))
                .withOption("modelPackage", "io.example.v3.domain.byMessage");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v3/domain/byMessage/CustomerInput.java").exists());
    }

    @Test
    public void test_generator_generates_only_payload_schemas_by_default() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-message-schemas.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.message.schemas.defaults");

        new MainGenerator().generate(plugin);

        String base = "target/zenwave630/src/main/java/io/example/message/schemas/defaults/";
        // Payloads are generated as before.
        Assertions.assertTrue(new File(base + "IncludedPayload.java").exists());
        Assertions.assertTrue(new File(base + "StandardKeyPayload.java").exists());
        Assertions.assertTrue(new File(base + "TraitHeadersPayload.java").exists());
        // Binding keys, protocol headers and application headers are all opt-in.
        Assertions.assertFalse(new File(base + "KafkaMessageKey.java").exists());
        Assertions.assertFalse(new File(base + "StandardKafkaKey.java").exists());
        Assertions.assertFalse(new File(base + "AvroMessageJsonKey.java").exists());
        Assertions.assertFalse(new File(base + "HttpProtocolHeaders.java").exists());
        Assertions.assertFalse(new File(base + "IncludedMessageJmsHeaders.java").exists());
        Assertions.assertFalse(new File(base + "TraitHeadersMessageHeaders.java").exists());
    }

    @Test
    public void test_generator_generates_message_binding_keys_when_enabled() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-message-schemas.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.message.schemas.keys")
                .withOption("generateMessageKeys", true);

        new MainGenerator().generate(plugin);

        String base = "target/zenwave630/src/main/java/io/example/message/schemas/keys/";
        Assertions.assertTrue(new File(base + "IncludedPayload.java").exists());
        Assertions.assertTrue(new File(base + "KafkaMessageKey.java").exists());
        Assertions.assertTrue(new File(base + "StandardKafkaKey.java").exists());
        // Issue #130: a JSON-schema key on a message with an Avro payload is still generated,
        // while the Avro payload itself is left to the Avro generator.
        Assertions.assertTrue(new File(base + "AvroMessageJsonKey.java").exists());
        Assertions.assertFalse(new File(base + "AvroPayload.java").exists());
        // Only keys are enabled here: protocol headers stay off.
        Assertions.assertFalse(new File(base + "HttpProtocolHeaders.java").exists());
        Assertions.assertFalse(new File(base + "IncludedMessageJmsHeaders.java").exists());
        // Messages not reachable from any operation are never generated.
        Assertions.assertFalse(new File(base + "UnusedKafkaKey.java").exists());
    }

    @Test
    public void test_generator_generates_protocol_binding_headers_when_enabled() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-message-schemas.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.message.schemas.bindings")
                .withOption("generateBindingHeaders", true);

        new MainGenerator().generate(plugin);

        String base = "target/zenwave630/src/main/java/io/example/message/schemas/bindings/";
        Assertions.assertTrue(new File(base + "HttpProtocolHeaders.java").exists());
        Assertions.assertTrue(new File(base + "IncludedMessageJmsHeaders.java").exists());
        // Scalar binding schemas (mqtt correlationData is a plain string) are not turned into POJOs.
        Assertions.assertFalse(new File(base + "IncludedMessageMqttCorrelationData.java").exists());
        // Kafka key generation is a separate switch.
        Assertions.assertFalse(new File(base + "KafkaMessageKey.java").exists());
    }

    @Test
    public void test_generator_generates_referenced_and_trait_message_headers_when_enabled() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-message-schemas.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.message.schemas.headers")
                .withOption("generateMessageHeaders", true);

        new MainGenerator().generate(plugin);

        // Issue #120: a header schema with an explicit javaType lands at that type...
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/custom/CustomMessageHeaders.java").exists());
        // ...and headers inherited from a message trait are generated too.
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/message/schemas/headers/TraitHeadersMessageHeaders.java").exists());
    }

    @Test
    public void test_generator_generates_message_schemas_for_asyncapi_v2() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v2-message-schemas.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.message.schemas.v2")
                .withOption("generateMessageKeys", true)
                .withOption("generateMessageHeaders", true);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/message/schemas/v2/V2Payload.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/message/schemas/v2/V2KafkaMessageKey.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/message/schemas/v2/V2ApplicationHeaders.java").exists());
    }

    @Test
    public void test_generator_for_asyncapi_repeated_enum() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.integration.test.with_schemas.model");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/Email.java").exists());
        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/UserSignedUp.java").exists());
    }

    @Test
    public void test_generator_for_asyncapi_schemas() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.integration.test.with_schemas.model");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/ColorRelatedMsg.java").exists());
    }

    @Test
    public void test_generator_for_json_schemas() throws Exception {
        var url = getClass().getClassLoader().getResource("/io/zenwave360/sdk/resources/asyncapi/v2/json-schemas/asyncapi.yml");
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/json-schemas/asyncapi.yml")
                .withTargetFolder("target/zenwave630");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/transport/schema/TransportNotificationEvent.java").exists());
    }
}
