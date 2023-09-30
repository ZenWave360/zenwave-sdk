package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

public class SpringCloudStreams3AdaptersGeneratorTest {

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
    public void test_generate_imperative_adapters() throws Exception {
        Plugin plugin = new SpringCloudStreams3AdaptersPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out/v2")
                .withOption("apiId", "orders")
                .withOption("basePackage", "io.example.integration.test")
                .withOption("consumerApiPackage", "io.example.orders.provider.api")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/AdapterEventsMapper.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/OnProductCreatedConsumerServiceAdapter.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/OnProductCreated2ConsumerServiceAdapter.java"));
    }


    @Test
    public void test_generate_imperative_adapters_with_jdl() throws Exception {
        Plugin plugin = new SpringCloudStreams3AdaptersPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-orders-relational.yml")
                .withTargetFolder("target/zenwave630/out/v2")
                .withOption("apiId", "provider")
                .withOption("zdlFile", "classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl")
                .withOption("basePackage", "io.example.orders.relational")
                .withOption("consumerApiPackage", "io.example.orders.provider.api")
                .withOption("modelPackage", "io.example.orders.relational.core.domain.model.events")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/AdapterEventsMapper.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoCustomerRequestConsumerServiceAdapter.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoShippingDetailsRequestConsumerServiceAdapter.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoPaymentDetailsRequestConsumerServiceAdapter.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoCustomerOrderRequestConsumerServiceAdapter.java"));
    }
}
