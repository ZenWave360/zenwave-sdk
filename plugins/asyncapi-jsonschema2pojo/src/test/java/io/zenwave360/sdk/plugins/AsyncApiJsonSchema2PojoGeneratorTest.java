package io.zenwave360.sdk.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.zenwave360.sdk.Plugin;
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
    }

    @Test
    public void test_generator_for_asyncapi_v31_external_json_schema_defs_with_underscores() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withApiFile("classpath:asyncapi-v3-external-json-schema-name-underscores.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.v31.domain.events");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/AddressC.java").exists());
        Assertions.assertFalse(new File("target/zenwave630/src/main/java/io/example/v31/domain/events/Address.java").exists());
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
