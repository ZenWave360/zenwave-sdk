package io.zenwave360.generator.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.MainGenerator;

public class AsyncApiJsonSchema2PojoGeneratorTest {

    @BeforeEach
    public void setup() throws IOException {
        FileUtils.deleteDirectory(new File("target/zenwave630/out"));
    }

    @Test
    public void test_generator_for_asyncapi_schemas() throws Exception {
        Configuration configuration = new AsyncApiJsonSchema2PojoConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630")
                .withOption("modelPackage", "io.example.integration.test.with_schemas.model");

        new MainGenerator().generate(configuration);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/integration/test/with_schemas/model/ColorRelatedMsg.java").exists());
    }

    @Test
    public void test_generator_for_json_schemas() throws Exception {
        var url = getClass().getClassLoader().getResource("/io/zenwave360/generator/resources/asyncapi/json-schemas/asyncapi.yml");
        Configuration configuration = new AsyncApiJsonSchema2PojoConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/json-schemas/asyncapi.yml")
                .withTargetFolder("target/zenwave630");

        new MainGenerator().generate(configuration);

        Assertions.assertTrue(new File("target/zenwave630/src/main/java/io/example/transport/schema/TransportNotificationEvent.java").exists());
    }
}
