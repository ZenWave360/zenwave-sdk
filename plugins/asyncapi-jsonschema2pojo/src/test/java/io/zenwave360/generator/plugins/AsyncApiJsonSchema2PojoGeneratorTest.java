package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.Main;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AsyncApiJsonSchema2PojoGeneratorTest {

    private static LogCaptor logCaptor = LogCaptor.forRoot();

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forRoot();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @Test
    @Disabled
    public void test_generator_for_asyncapi_schemas() throws Exception {
        Configuration configuration = new AsyncApiJsonSchema2PojoConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_events")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                ;

        new Main().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/IDefaultServiceEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/DefaultServiceEventsProducer.java"));
    }


    @Test
    @Disabled
    public void test_generator_for_json_schemas() throws Exception {
        Configuration configuration = new AsyncApiJsonSchema2PojoConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/json-schemas/asyncapi.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_events")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                ;

        new Main().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/IDefaultServiceEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/DefaultServiceEventsProducer.java"));
    }
}
