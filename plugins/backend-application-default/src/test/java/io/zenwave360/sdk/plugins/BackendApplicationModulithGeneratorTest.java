package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

public class BackendApplicationModulithGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void test_generator_modulith_customer_address() throws Exception {
        String targetFolder = "target/zdl/test_generator_modulith_customer_address";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("layout.commonPackage", "io.zenwave360.example.common")
                .withOption("layout.moduleBasePackage", "io.zenwave360.example.customer")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("useSpringModulith", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        Path moduleInfo = Path.of(targetFolder, "src/main/java/io/zenwave360/example/customer/package-info.java");
        Assertions.assertTrue(Files.exists(moduleInfo));
        Assertions.assertTrue(Files.readString(moduleInfo).contains("@org.springframework.modulith.ApplicationModule"));
    }

    @Test
    public void generatesRawInternalEventAndGeneratedOnceInternalListeners() throws Exception {
        String targetFolder = tempDir.resolve("internal-events-and-listeners").toString();
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:zdl/internal-events-and-listeners.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("layout.domainEventsPackage", "io.zenwave360.example.domain.signals")
                .withOption("layout.adaptersEventsPackage", "io.zenwave360.example.adapters.internal")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "orders")
                .withOption("useSpringModulith", true)
                .withOption("implementEventListeners", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        Path rawEvent = Path.of(targetFolder, "src/main/java/io/zenwave360/example/domain/signals/OrderCreated.java");
        Path listener = Path.of(targetFolder, "src/main/java/io/zenwave360/example/adapters/internal/OrdersServiceEventsListener.java");
        Path service = Path.of(targetFolder, "src/main/java/io/zenwave360/example/core/application/OrdersServiceImpl.java");
        Path mapper = Path.of(targetFolder, "src/main/java/io/zenwave360/example/core/application/mappers/EventsMapper.java");
        Assertions.assertTrue(Files.exists(rawEvent));
        String listenerSource = Files.readString(listener);
        Assertions.assertTrue(listenerSource.contains("import io.zenwave360.example.domain.signals.*;"));
        Assertions.assertTrue(listenerSource.contains("@ApplicationModuleListener"));
        Assertions.assertTrue(listenerSource.contains("public void handleOrderCreatedWithMapping(OrderCreated event)"));
        Assertions.assertTrue(listenerSource.contains("ordersService.handleOrderCreatedWithMapping(mapper.asOrderCreatedInput(event));"));

        Path listenerMapper = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/adapters/internal/mappers/OrdersServiceEventsListenerMapper.java");
        Path listenerMapStructMapper = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/adapters/internal/mappers/OrdersServiceEventsListenerMapStructMapper.java");
        String listenerMapperSource = Files.readString(listenerMapper).replaceAll("\\s+", " ");
        Assertions.assertTrue(listenerMapperSource.contains("OrderCreatedInput asOrderCreatedInput(OrderCreated event)"));
        Assertions.assertTrue(Files.readString(listenerMapStructMapper).contains("@Mapper(componentModel = \"spring\")"));

        String serviceSource = Files.readString(service);
        Assertions.assertTrue(serviceSource.contains("eventPublisher.onOrderCreated(orderCreated);"));
        Assertions.assertFalse(serviceSource.contains("eventsProducer"));

        String mapperSource = Files.readString(mapper).replaceAll("\\s+", " ");
        Assertions.assertTrue(mapperSource.contains(
                "io.zenwave360.example.domain.signals.OrderCreated asOrderCreated(Order order)"));

        Files.writeString(listener, "// developer-owned listener\n");
        new MainGenerator().generate(plugin);
        Assertions.assertEquals("// developer-owned listener\n", Files.readString(listener));
    }

    @Test
    public void generatesCrossModuleListenersFromReferencedZdl() throws Exception {
        String targetFolder = tempDir.resolve("cross-module-listeners").toString();

        Plugin paymentsPlugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:zdl/payments-processing.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example.payments")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useSpringModulith", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);
        new MainGenerator().generate(paymentsPlugin);

        Plugin ordersPlugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:zdl/orders-cross-module-listeners.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example.orders")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useSpringModulith", true)
                .withOption("implementEventListeners", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);
        new MainGenerator().generate(ordersPlugin);

        Path rawPaymentEvent = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/payments/core/domain/events/PaymentAuthorized.java");
        Assertions.assertTrue(Files.exists(rawPaymentEvent), "Producer module should generate internal domain event classes");

        Path listener = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.java");
        String listenerSource = Files.readString(listener).replaceAll("\\s+", " ");
        Assertions.assertTrue(listenerSource.contains("@ApplicationModuleListener"));
        Assertions.assertTrue(listenerSource.contains(
                "public void onPaymentAuthorized( io.zenwave360.example.payments.core.domain.events.PaymentAuthorized event)")
                || listenerSource.contains(
                "public void onPaymentAuthorized(io.zenwave360.example.payments.core.domain.events.PaymentAuthorized event)"));
        Assertions.assertTrue(listenerSource.contains("onPaymentDeclined("));
        Assertions.assertTrue(listenerSource.contains("onPaymentVoided("),
                "Every stacked @listener occurrence should produce its own listener method");
        Assertions.assertTrue(listenerSource.contains("ordersService.confirmOrder(mapper.asPaymentAuthorized(event));"));
        Assertions.assertTrue(listenerSource.contains("ordersService.cancelOrder(mapper.asCancelOrderInput(event));"));

        Path listenerMapper = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/payments/mappers/PaymentsProcessingEventsListenerMapper.java");
        String listenerMapperSource = Files.readString(listenerMapper).replaceAll("\\s+", " ");
        Assertions.assertTrue(listenerMapperSource.contains(
                "io.zenwave360.example.orders.core.inbound.dtos.PaymentAuthorized asPaymentAuthorized("));
        Assertions.assertTrue(listenerMapperSource.contains(
                "io.zenwave360.example.payments.core.domain.events.PaymentAuthorized event)"),
                "Same-named local inputs and third-module events must both use collision-safe FQCNs");
        Assertions.assertTrue(listenerMapperSource.contains(
                "io.zenwave360.example.orders.core.inbound.dtos.CancelOrderInput asCancelOrderInput("));
        Assertions.assertTrue(listenerMapperSource.contains(
                "io.zenwave360.example.payments.core.domain.events.PaymentDeclined event)"));
        Assertions.assertTrue(listenerMapperSource.contains(
                "io.zenwave360.example.payments.core.domain.events.PaymentVoided event)"));

        Path sameModuleListener = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/orders/OrdersServiceEventsListener.java");
        String sameModuleListenerSource = Files.readString(sameModuleListener);
        Assertions.assertTrue(sameModuleListenerSource.contains("import io.zenwave360.example.orders.core.domain.events.*;"));
        Assertions.assertTrue(sameModuleListenerSource.contains("@ApplicationModuleListener"));
        Assertions.assertTrue(sameModuleListenerSource.contains("public void handleOrderConfirmed(OrderConfirmed event)"));
        Assertions.assertTrue(sameModuleListenerSource.contains(
                "ordersService.handleOrderConfirmed(mapper.asOrderConfirmedInput(event));"));

        Path sameModuleListenerMapper = Path.of(targetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/orders/mappers/OrdersServiceEventsListenerMapper.java");
        Assertions.assertTrue(Files.readString(sameModuleListenerMapper).replaceAll("\\s+", " ").contains(
                "OrderConfirmedInput asOrderConfirmedInput(OrderConfirmed event)"));

        Assertions.assertEquals(0, MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", targetFolder));

        Files.writeString(sameModuleListener, "// developer-owned listener\n");
        new MainGenerator().generate(ordersPlugin);
        Assertions.assertEquals("// developer-owned listener\n", Files.readString(sameModuleListener));
    }

    @Test
    public void implementEventListenersControlsGenerationIndependentlyFromUseSpringModulith() throws Exception {
        String disabledTargetFolder = tempDir.resolve("listeners-disabled").toString();
        Plugin disabledPlugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:zdl/orders-cross-module-listeners.zdl")
                .withTargetFolder(disabledTargetFolder)
                .withOption("basePackage", "io.zenwave360.example.orders")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useSpringModulith", false)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);
        new MainGenerator().generate(disabledPlugin);

        Path disabledListener = Path.of(disabledTargetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.java");
        Assertions.assertFalse(Files.exists(disabledListener));

        String enabledTargetFolder = tempDir.resolve("listeners-enabled").toString();
        Plugin enabledPlugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:zdl/orders-cross-module-listeners.zdl")
                .withTargetFolder(enabledTargetFolder)
                .withOption("basePackage", "io.zenwave360.example.orders")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useSpringModulith", false)
                .withOption("implementEventListeners", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);
        new MainGenerator().generate(enabledPlugin);

        Path listener = Path.of(enabledTargetFolder,
                "src/main/java/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.java");
        String listenerSource = Files.readString(listener);
        Assertions.assertTrue(listenerSource.contains("@ApplicationModuleListener"));
        Assertions.assertFalse(listenerSource.contains("org.springframework.context.event.EventListener"));
    }
}
