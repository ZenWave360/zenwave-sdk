package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

public class SpringCloudStreams3GeneratorV3Test {

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
    public void test_generator_provider_for_events() throws Exception {
        Plugin plugin = new SpringCloudStreams3Plugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-order-asyncapi.yml")
                .withTargetFolder("target/zenwave630/out/v3")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_events")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/DoCustomerRequestConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/IDoCustomerRequestConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/DoCustomerOrderRequestConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/IDoCustomerOrderRequestConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/ICustomerEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/CustomerEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/api/provider_for_events/CustomerEventsProducerCaptor.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/ICustomerOrderEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/CustomerOrderEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/api/provider_for_events/CustomerOrderEventsProducerCaptor.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/test/java/io/example/integration/test/api/provider_for_events/ProducerInMemoryContext.java"));
    }

    @Test
    public void test_generator_client_with_operationId() throws Exception {
        Plugin plugin = new SpringCloudStreams3Plugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-order-asyncapi.yml")
                .withTargetFolder("target/zenwave630/out/v3")
                .withOption("apiPackage", "io.example.integration.test.api.client_with_operationId")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("useEnterpriseEnvelope", true)
                .withOption("operationIds", List.of("onCustomerEvent"))
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.imperative);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_with_operationId/OnCustomerEventConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_with_operationId/IOnCustomerEventConsumerService.java"));
    }


}
