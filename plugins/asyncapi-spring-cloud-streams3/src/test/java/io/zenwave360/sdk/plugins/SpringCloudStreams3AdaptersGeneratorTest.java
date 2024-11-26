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
    public void test_generate_imperative_adapters_events() throws Exception {
        Plugin plugin = new SpringCloudStreams3AdaptersPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml")
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
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/EventsMapper.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/OnProductCreatedConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/adapters/events/orders/OnProductCreated2ConsumerService.java"));
    }


    @Test
    public void test_generate_imperative_adapters_orders_relational() throws Exception {
        Plugin plugin = new SpringCloudStreams3AdaptersPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-orders-relational.yml")
                .withTargetFolder("target/zenwave630/out/v2")
                .withOption("apiId", "provider")
                .withOption("basePackage", "io.example.orders.relational")
                .withOption("consumerApiPackage", "io.example.orders.provider.api")
                .withOption("modelPackage", "io.example.orders.relational.core.domain.model.events")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/EventsMapper.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoCustomerRequestConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoShippingDetailsRequestConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoPaymentDetailsRequestConsumerService.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/orders/relational/adapters/events/provider/DoCustomerOrderRequestConsumerService.java"));
    }
}
