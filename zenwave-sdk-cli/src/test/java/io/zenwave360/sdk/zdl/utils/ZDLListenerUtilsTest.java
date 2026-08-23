package io.zenwave360.sdk.zdl.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;

class ZDLListenerUtilsTest {

    @TempDir
    Path tempDir;

    private static final String PAYMENTS_ZDL = """
            config {
                basePackage "io.example.payments"
            }

            event PaymentAuthorized {
                paymentId String
            }

            event PaymentDeclined {
                paymentId String
            }
            """;

    private Map<String, Object> parse(String ordersZdl) throws IOException {
        return parse(ordersZdl, PAYMENTS_ZDL);
    }

    private Map<String, Object> parse(String ordersZdl, String paymentsZdl) throws IOException {
        Files.writeString(tempDir.resolve("payments.zdl"), paymentsZdl, StandardCharsets.UTF_8);
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ordersZdl, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        return (Map<String, Object>) new ZDLProcessor().process(contextModel).get("zdl");
    }

    @Test
    void resolvesCrossModuleEventTypeFromReferencedLayout() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);

        var groups = ZDLListenerUtils.listenerGroups(zdl);
        Assertions.assertEquals(1, groups.size());
        var group = groups.get(0);
        Assertions.assertEquals("zdl", group.get("type"));
        Assertions.assertEquals("Payments", group.get("groupName"));
        Assertions.assertEquals("PaymentsEventsListener", group.get("className"));

        var binding = ((List<Map<String, Object>>) group.get("bindings")).get(0);
        Assertions.assertEquals("io.example.payments.core.domain.events.PaymentAuthorized", binding.get("eventType"));
        Assertions.assertEquals("onPaymentAuthorized", binding.get("listenerMethodName"));
        Assertions.assertEquals(ZDLListenerUtils.MODE_MAPPER, binding.get("mode"));
        Assertions.assertEquals("asConfirmOrderInput", binding.get("mapperMethodName"));
    }

    @Test
    void honorsExplicitDomainEventsPackageOverride() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis {
                    zdl client PaymentsZdl "payments.zdl" {
                        domainEventsPackage "com.acme.custom.events"
                    }
                }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);

        var binding = ((List<Map<String, Object>>) ZDLListenerUtils.listenerGroups(zdl).get(0).get("bindings")).get(0);
        Assertions.assertEquals("com.acme.custom.events.PaymentAuthorized", binding.get("eventType"));
    }

    @Test
    void resolvesReferencedShortLayoutName() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }
                apis { zdl client PaymentsZdl "payments.zdl" }
                input ConfirmOrderInput { orderId String }
                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    confirmOrder(ConfirmOrderInput)
                }
                entity Order { orderId String }
                """, """
                config {
                    basePackage "io.example.payments"
                    layout SimpleDomainProjectLayout
                }
                event PaymentAuthorized { paymentId String }
                """);

        var binding = ((List<Map<String, Object>>) ZDLListenerUtils.listenerGroups(zdl).get(0).get("bindings")).get(0);
        Assertions.assertEquals("io.example.payments.domain.events.PaymentAuthorized", binding.get("eventType"));
    }

    @Test
    void resolvesReferencedModuleLayoutWithProjectOptionsAndReferencedModuleConfig() throws IOException {
        var zdl = parse("""
                config {
                    moduleBasePackage "com.example.clinical.modules.clinical"
                    layout CleanHexagonalProjectLayout
                }
                apis { zdl client DocumentsModule "payments.zdl" }
                input DocumentSignatureRequestedInput { documentInfoId Long }
                service PatientsService for (Patient) {
                    @listener(zdl: DocumentsModule, event: PaymentAuthorized)
                    associateDocumentWithPatient(DocumentSignatureRequestedInput)
                }
                entity Patient { patientId Long }
                """, """
                config {
                    moduleBasePackage "com.example.clinical.modules.documents"
                    layout SimpleDomainProjectLayout
                }
                event PaymentAuthorized { documentInfoId Long }
                """);

        var generatorOptions = Map.<String, Object>of(
                "basePackage", "com.example.clinical",
                "moduleBasePackage", "com.example.clinical.modules.clinical");
        var binding = ((List<Map<String, Object>>) ZDLListenerUtils
                .listenerGroups(zdl, null, generatorOptions).get(0).get("bindings")).get(0);

        Assertions.assertEquals(
                "com.example.clinical.modules.documents.domain.events.PaymentAuthorized",
                binding.get("eventType"));
    }

    @Test
    void failsClearlyWhenReferencedLayoutCannotBeLoaded() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }
                apis { zdl client PaymentsZdl "payments.zdl" }
                input ConfirmOrderInput { orderId String }
                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    confirmOrder(ConfirmOrderInput)
                }
                entity Order { orderId String }
                """, """
                config {
                    basePackage "io.example.payments"
                    layout MissingProjectLayout
                }
                event PaymentAuthorized { paymentId String }
                """);

        var error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ZDLListenerUtils.listenerGroups(zdl));
        Assertions.assertTrue(error.getMessage().contains("unable to load layout 'MissingProjectLayout'"));
        Assertions.assertTrue(error.getMessage().contains("configure domainEventsPackage explicitly"));
    }

    @Test
    void sameNamedThirdModuleEventAndLocalInputAlwaysUseMapper() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                input PaymentAuthorized { paymentId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    handlePaymentAuthorized(PaymentAuthorized)
                }

                entity Order { orderId String }
                """);

        var binding = ((List<Map<String, Object>>) ZDLListenerUtils.listenerGroups(zdl).get(0).get("bindings")).get(0);
        Assertions.assertEquals(ZDLListenerUtils.MODE_MAPPER, binding.get("mode"));
        Assertions.assertEquals("PaymentAuthorized", binding.get("inputType"));
        Assertions.assertEquals("io.example.payments.core.domain.events.PaymentAuthorized", binding.get("eventType"));
        Assertions.assertEquals("asPaymentAuthorized", binding.get("mapperMethodName"));
    }

    @Test
    void createsOneBindingPerRepeatedListenerOccurrence() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                input CancelOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    @listener(zdl: PaymentsZdl, event: PaymentDeclined)
                    cancelOrder(CancelOrderInput)
                }

                entity Order { orderId String }
                """);

        var group = ZDLListenerUtils.listenerGroups(zdl).get(0);
        var bindings = (List<Map<String, Object>>) group.get("bindings");
        Assertions.assertEquals(2, bindings.size(), "each stacked @listener is an independent binding");
        Assertions.assertEquals(List.of("onPaymentAuthorized", "onPaymentDeclined"),
                bindings.stream().map(b -> b.get("listenerMethodName")).toList());
        Assertions.assertEquals(2, ((List<?>) group.get("mapperBindings")).size());
    }

    @Test
    void classifiesUnsupportedSignaturesAsCustomRequired() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized)
                    confirmOrder(id)
                }

                entity Order { orderId String }
                """);

        var binding = ((List<Map<String, Object>>) ZDLListenerUtils.listenerGroups(zdl).get(0).get("bindings")).get(0);
        Assertions.assertEquals(ZDLListenerUtils.MODE_CUSTOM, binding.get("mode"));
    }

    @Test
    void groupsSameModuleListenersByService() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                input OrderCreatedInput { orderId String }

                service OrdersService for (Order) {
                    createOrder(Order) Order withEvents OrderCreated

                    @listener({event: OrderCreated})
                    handleOrderCreated(OrderCreatedInput)
                }

                entity Order { orderId String }

                event OrderCreated { orderId String }
                """);

        var groups = ZDLListenerUtils.listenerGroups(zdl);
        Assertions.assertEquals(1, groups.size());
        var group = groups.get(0);
        Assertions.assertEquals("local", group.get("type"));
        Assertions.assertEquals("OrdersServiceEventsListener", group.get("className"));

        var binding = ((List<Map<String, Object>>) group.get("bindings")).get(0);
        Assertions.assertEquals("OrderCreated", binding.get("eventType"), "same-module events are not qualified");
        Assertions.assertEquals("handleOrderCreated", binding.get("listenerMethodName"));
    }

    @Test
    void failsOnUndeclaredOrMistypedApiAndUnknownEvent() throws IOException {
        var undeclaredApi = parse("""
                config { basePackage "io.example.orders" }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: MissingZdl, event: PaymentAuthorized)
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);
        var undeclaredApiError = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ZDLListenerUtils.listenerGroups(undeclaredApi));
        Assertions.assertTrue(undeclaredApiError.getMessage().contains("undeclared zdl api"));

        var unknownEvent = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: NotAnEvent)
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);
        var unknownEventError = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ZDLListenerUtils.listenerGroups(unknownEvent));
        Assertions.assertTrue(unknownEventError.getMessage().contains("not found in zdl api"));
    }

    @Test
    void rejectsChannelOrTopicOnListener() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                apis { zdl client PaymentsZdl "payments.zdl" }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    @listener(zdl: PaymentsZdl, event: PaymentAuthorized, channel: "payments")
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);

        var error = Assertions.assertThrows(IllegalArgumentException.class, () -> ZDLListenerUtils.listenerGroups(zdl));
        Assertions.assertTrue(error.getMessage().contains("must not restate channel or topic"));
    }

    @Test
    void returnsNoGroupsWhenNoListenersDeclared() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                input ConfirmOrderInput { orderId String }

                service OrdersService for (Order) {
                    confirmOrder(ConfirmOrderInput)
                }

                entity Order { orderId String }
                """);
        Assertions.assertTrue(ZDLListenerUtils.listenerGroups(zdl).isEmpty());
        Assertions.assertNotNull(JSONPath.get(zdl, "$.services.OrdersService"));
    }
}
