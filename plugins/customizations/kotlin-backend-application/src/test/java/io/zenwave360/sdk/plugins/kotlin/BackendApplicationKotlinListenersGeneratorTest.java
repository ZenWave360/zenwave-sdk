package io.zenwave360.sdk.plugins.kotlin;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;

/** Verifies Kotlin templates for ZDL listeners and backend-owned AsyncAPI consumer adapters. */
class BackendApplicationKotlinListenersGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesSameAndCrossModuleKotlinListenersAndPreservesGeneratedOnceFiles() throws Exception {
        Path targetFolder = tempDir.resolve("cross-module-listeners");
        new MainGenerator().generate(kotlinPlugin(
                "classpath:zdl/kotlin-payments-processing.zdl",
                targetFolder,
                "io.zenwave360.example.payments",
                false));

        Plugin ordersPlugin = kotlinPlugin(
                "classpath:zdl/kotlin-orders-cross-module-listeners.zdl",
                targetFolder,
                "io.zenwave360.example.orders",
                true);
        new MainGenerator().generate(ordersPlugin);

        Path listener = targetFolder.resolve(
                "src/main/kotlin/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.kt");
        String listenerSource = normalized(listener);
        Assertions.assertTrue(listenerSource.contains("@ApplicationModuleListener"));
        Assertions.assertTrue(listenerSource.contains(
                "fun onPaymentAuthorized(event: io.zenwave360.example.payments.core.domain.events.PaymentAuthorized)"));
        Assertions.assertTrue(listenerSource.contains("fun onPaymentDeclined("));
        Assertions.assertTrue(listenerSource.contains("fun onPaymentVoided("));
        Assertions.assertTrue(listenerSource.contains(
                "ordersService.confirmOrder(mapper.asPaymentAuthorized(event))"));
        Assertions.assertTrue(listenerSource.contains(
                "ordersService.cancelOrder(mapper.asCancelOrderInput(event))"));

        Path mapper = targetFolder.resolve(
                "src/main/kotlin/io/zenwave360/example/orders/adapters/events/payments/mappers/PaymentsProcessingEventsListenerMapper.kt");
        String mapperSource = normalized(mapper);
        Assertions.assertTrue(mapperSource.contains("fun asPaymentAuthorized("));
        Assertions.assertTrue(mapperSource.contains(
                "event: io.zenwave360.example.payments.core.domain.events.PaymentAuthorized"));
        Assertions.assertTrue(mapperSource.contains(
                "io.zenwave360.example.orders.core.inbound.dtos.PaymentAuthorized"));
        Assertions.assertTrue(mapperSource.contains("fun asCancelOrderInput("));
        Assertions.assertTrue(mapperSource.contains(
                "event: io.zenwave360.example.payments.core.domain.events.PaymentDeclined"));
        Assertions.assertTrue(mapperSource.contains(
                "io.zenwave360.example.orders.core.inbound.dtos.CancelOrderInput"));
        Assertions.assertTrue(mapperSource.contains("@Mapper(uses = [BaseMapper::class])"));
        Assertions.assertTrue(mapperSource.contains(
                "Mappers.getMapper(PaymentsProcessingEventsListenerMapper::class.java)"));
        Assertions.assertFalse(mapperSource.contains("\"copy\""), mapperSource);
        Assertions.assertTrue(listenerSource.contains(
                "private val mapper: PaymentsProcessingEventsListenerMapper = PaymentsProcessingEventsListenerMapper.INSTANCE"));

        Path sameModuleListener = targetFolder.resolve(
                "src/main/kotlin/io/zenwave360/example/orders/adapters/events/orders/OrdersServiceEventsListener.kt");
        Assertions.assertTrue(normalized(sameModuleListener).contains(
                "fun handleOrderConfirmed(event: OrderConfirmed)"));

        Files.writeString(listener, "// developer-owned Kotlin listener\n");
        Files.writeString(mapper, Files.readString(mapper) + "\n// developer mapper customization\n");
        new MainGenerator().generate(ordersPlugin);
        Assertions.assertEquals("// developer-owned Kotlin listener\n", Files.readString(listener));
        Assertions.assertTrue(Files.readString(mapper).contains("// developer mapper customization"));
    }

    @Test
    void implementEventListenersControlsKotlinListenerGeneration() throws Exception {
        Path targetFolder = tempDir.resolve("listeners-disabled");
        new MainGenerator().generate(kotlinPlugin(
                "classpath:zdl/kotlin-orders-cross-module-listeners.zdl",
                targetFolder,
                "io.zenwave360.example.orders",
                false));

        Assertions.assertFalse(Files.exists(targetFolder.resolve(
                "src/main/kotlin/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.kt")));
    }

    @Test
    void generatesKotlinAsyncApiMapperAndConsumerAdapter() throws Exception {
        Path targetFolder = tempDir.resolve("asyncapi-adapter");
        Plugin plugin = kotlinPlugin(
                "classpath:zdl/kotlin-asyncapi-consumer-adapter.zdl",
                targetFolder,
                "io.example.customer",
                true);
        new MainGenerator().generate(plugin);

        Path packageFolder = targetFolder.resolve(
                "src/main/kotlin/io/example/customer/adapters/events/customer");
        Path mapper = packageFolder.resolve("EventsMapper.kt");
        Path adapter = packageFolder.resolve("CreateCustomerChannelConsumerService.kt");

        String mapperSource = normalized(mapper);
        Assertions.assertTrue(mapperSource.contains("fun createCustomerInput("));
        Assertions.assertTrue(mapperSource.contains(
                "event: io.example.customer.contract.model.Customer"));
        Assertions.assertTrue(mapperSource.contains(
                "io.example.customer.core.inbound.dtos.CustomerInput"));
        Assertions.assertTrue(mapperSource.contains(
                "Mappers.getMapper(EventsMapper::class.java)"));
        Assertions.assertTrue(mapperSource.contains("@Mapper(uses = [BaseMapper::class])"));
        Assertions.assertFalse(mapperSource.contains("\"copy\""), mapperSource);

        String adapterSource = normalized(adapter);
        Assertions.assertTrue(adapterSource.contains(
                "class CreateCustomerChannelConsumerService("));
        Assertions.assertTrue(adapterSource.contains(
                ") : io.example.customer.contract.consumer.ICreateCustomerChannelConsumerService"));
        Assertions.assertTrue(adapterSource.contains(
                "private val customerService: io.example.customer.core.inbound.CustomerService"));
        Assertions.assertTrue(adapterSource.contains("override fun doCreateCustomer("));
        Assertions.assertTrue(adapterSource.contains(
                "payload: io.example.customer.contract.model.Customer"));
        Assertions.assertTrue(adapterSource.contains(
                "headers: io.example.customer.contract.consumer.ICreateCustomerChannelConsumerService.CustomerHeaders"));
        Assertions.assertTrue(adapterSource.contains(
                "customerService.createCustomer(eventsMapper.createCustomerInput(payload))"));

        Files.writeString(mapper, Files.readString(mapper) + "\n// developer mapper customization\n");
        Files.writeString(adapter, Files.readString(adapter) + "\n// developer adapter customization\n");
        new MainGenerator().generate(plugin);
        Assertions.assertTrue(Files.readString(mapper).contains("// developer mapper customization"));
        Assertions.assertTrue(Files.readString(adapter).contains("// developer adapter customization"));
    }

    @Test
    void generatesCompilingKotlinTodoForCustomRequiredAsyncApiShape() throws Exception {
        Path targetFolder = tempDir.resolve("asyncapi-custom-adapter");
        new MainGenerator().generate(kotlinPlugin(
                "classpath:zdl/kotlin-asyncapi-consumer-adapter-custom.zdl",
                targetFolder,
                "io.example.customer",
                true));

        Path packageFolder = targetFolder.resolve(
                "src/main/kotlin/io/example/customer/adapters/events/customer");
        String adapterSource = normalized(
                packageFolder.resolve("CreateCustomerChannelConsumerService.kt"));
        Assertions.assertTrue(adapterSource.contains(
                "private val customerService: io.example.customer.core.inbound.CustomerService"));
        Assertions.assertTrue(adapterSource.contains("TODO CUSTOM_REQUIRED"));
        Assertions.assertTrue(adapterSource.contains("throw UnsupportedOperationException("));
        Assertions.assertFalse(Files.exists(packageFolder.resolve("EventsMapper.kt")));
    }

    // ---- spec sdk-listener-generation: id placeholder in Kotlin listeners ----

    private String generateKotlinIdListener(String name) throws Exception {
        Path targetFolder = tempDir.resolve(name);
        new MainGenerator().generate(kotlinPlugin(
                "classpath:zdl/kotlin-payments-processing.zdl", targetFolder, "io.zenwave360.example.payments", false));
        new MainGenerator().generate(kotlinPlugin(
                "classpath:zdl/kotlin-orders-cross-module-listeners.zdl", targetFolder,
                "io.zenwave360.example.orders", true));
        return normalized(targetFolder.resolve(
                "src/main/kotlin/io/zenwave360/example/orders/adapters/events/payments/PaymentsProcessingEventsListener.kt"));
    }

    @Test
    void kotlinListenerWithIdAndBeanInputDeclaresTodoIdPlaceholder() throws Exception {
        String listener = generateKotlinIdListener("kotlin-listener-id-input");
        Assertions.assertTrue(listener.contains(
                "val id: Long = TODO(\"CUSTOM_REQUIRED: resolve the id for PaymentDeclined\") "
                        + "ordersService.holdOrder(id, mapper.asCancelOrderInput(event))"), listener);
    }

    @Test
    void kotlinListenerWithIdOnlyDeclaresTodoIdPlaceholder() throws Exception {
        String listener = generateKotlinIdListener("kotlin-listener-id-only");
        Assertions.assertTrue(listener.contains(
                "val id: Long = TODO(\"CUSTOM_REQUIRED: resolve the id for PaymentVoided\") "
                        + "ordersService.archiveOrder(id)"), listener);
    }

    @Test
    void kotlinListenerUnsupportedShapesKeepThrowingCustomRequiredBody() throws Exception {
        String listener = generateKotlinIdListener("kotlin-listener-unsupported");
        Assertions.assertTrue(listener.contains("fun onPaymentAuthorizedSearchOrders("), listener);
        Assertions.assertTrue(listener.contains("fun onPaymentAuthorizedSearchOrdersById("), listener);
        Assertions.assertEquals(2, listener.split("throw UnsupportedOperationException\\(", -1).length - 1, listener);
    }

    @Test
    void kotlinListenerWithoutIdIsUnchanged() throws Exception {
        String listener = generateKotlinIdListener("kotlin-listener-no-id");
        Assertions.assertTrue(listener.contains(
                "fun onPaymentAuthorized(event: io.zenwave360.example.payments.core.domain.events.PaymentAuthorized) "
                        + "{ ordersService.confirmOrder(mapper.asPaymentAuthorized(event)) }"), listener);
    }

    private Plugin kotlinPlugin(String zdlFile, Path targetFolder, String basePackage,
            boolean implementEventListeners) throws Exception {
        // The backend plugin is provided-scoped for this customization module.
        Plugin plugin = (Plugin) Class.forName("io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin")
                .getConstructor()
                .newInstance();
        return plugin
                .withZdlFile(zdlFile)
                .withTargetFolder(targetFolder.toString())
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("implementEventListeners", implementEventListeners)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("skipFormatting", false)
                .withOption("haltOnFailFormatting", true);
    }

    private String normalized(Path file) throws Exception {
        return Files.readString(file).replaceAll("\\s+", " ");
    }
}
