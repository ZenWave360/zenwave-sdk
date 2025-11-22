package io.zenwave360.sdk.plugins.kafka;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.sdk.plugins.AsyncAPIGeneratorPlugin;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.*;

import java.io.File;

public class ShoppingCartTest {
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

    String targetFolder = "target/out/shopping-cart";

    @Test
    public void test_generate_shopping_cart_1_provider() throws Exception {
        Plugin plugin = new AsyncAPIGeneratorPlugin()
                .withApiFile("classpath:shopping-cart-1/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("templates", "SpringKafka")
                .withOption("avroCompilerProperties.imports", "classpath:shopping-cart-1/avro")
                .withOption("producerApiPackage", "io.example.asyncapi.shoppingcart.client.events")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/asyncapi/shoppingcart/client/events/ShoppingCartEventsProducer.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/asyncapi/shoppingcart/client/events/DefaultShoppingCartEventsProducer.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/test/java/io/example/asyncapi/shoppingcart/client/events/InMemoryShoppingCartEventsProducer.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/test/java/io/example/asyncapi/shoppingcart/client/events/EventsProducerInMemoryContext.java").exists());
    }

    @Test
    public void test_generate_shopping_cart_1_client() throws Exception {
        Plugin plugin = new AsyncAPIGeneratorPlugin()
                .withApiFile("classpath:shopping-cart-1/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("templates", "SpringKafka")
                .withOption("avroCompilerProperties.imports", "classpath:shopping-cart-1/avro")
                .withOption("consumerApiPackage", "io.example.asyncapi.shoppingcart.client.events")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/asyncapi/shoppingcart/client/events/IShoppingCartChannelConsumerService.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/asyncapi/shoppingcart/client/events/ShoppingCartChannelConsumer.java").exists());
    }
}
