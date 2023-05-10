package io.zenwave360.sdk.plugins;

import java.io.File;
import java.io.IOException;

import io.zenwave360.sdk.Plugin;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;

public class AsyncApiJsonSchema2PojoGeneratorTest {

    @BeforeEach
    public void setup() throws IOException {
        FileUtils.deleteDirectory(new File("target/zenwave630/out"));
    }

    @Test
    public void test_generator_for_asyncapi_schemas() throws Exception {
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.integration.test.with_schemas.model");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/ColorRelatedMsg.java").exists());
    }

    @Test
    public void test_generator_for_json_schemas() throws Exception {
        var url = getClass().getClassLoader().getResource("/io/zenwave360/sdk/resources/asyncapi/v2/v2/json-schemas/asyncapi.yml");
        Plugin plugin = new AsyncApiJsonSchema2PojoPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/json-schemas/asyncapi.yml")
                .withTargetFolder("target/zenwave630");

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/transport/schema/TransportNotificationEvent.java").exists());
    }
}