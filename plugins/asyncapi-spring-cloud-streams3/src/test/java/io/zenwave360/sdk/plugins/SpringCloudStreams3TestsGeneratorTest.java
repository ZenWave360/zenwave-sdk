package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

public class SpringCloudStreams3TestsGeneratorTest {

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
    public void test_generate_imperative_tests() throws Exception {
        Plugin plugin = new SpringCloudStreams3TestsPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out/v2")
                .withOption("apiPackage", "io.example.integration.test.consumer_tests_imperative")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.imperative);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/consumer_tests_imperative/BaseConsumerTest.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/consumer_tests_imperative/OnProductCreatedConsumerServiceIT.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/consumer_tests_imperative/OnProductCreated2ConsumerServiceIT.java"));
    }
}
