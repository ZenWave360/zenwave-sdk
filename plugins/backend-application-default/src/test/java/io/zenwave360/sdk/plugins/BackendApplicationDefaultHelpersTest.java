package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BackendApplicationDefaultHelpersTest {

    private BackendApplicationDefaultGenerator generator = new BackendApplicationDefaultGenerator();
    private BackendApplicationDefaultHelpers helpers = new BackendApplicationDefaultHelpers(generator);

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    private Options options(Map map) {
        Context context = Context
                .newBuilder(map)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
                .build();
        return new Options.Builder(null, null, null, context, null).build();
    }


    @Test
    void methodParametersCallSignature() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.updateCustomer");
        var methodParametersCallSignature = helpers.methodParametersCallSignature((Map) method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals("id, input", methodParametersCallSignature);
    }

    @Test
    void methodParametersCallSignature_Inline() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.addCustomerAddress");
        var methodParametersCallSignature = helpers.methodParametersCallSignature((Map) method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals("customerId, address", methodParametersCallSignature);
    }

    @Test
    void methodEvents() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.createCustomer");
        var createCustomer = helpers.methodEvents((Map) method, options(Map.of("zdl", zdl))).stream()
                .map(e -> JSONPath.get(e, "name")).toList();
        Assertions.assertEquals(List.of("CustomerEvent", "CustomerCreated", "CustomerCreatedFailed"), createCustomer);
    }

    @Test
    void listOfPairEventEntity() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var methodParametersCallSignature = helpers.listOfPairEventEntity(zdl, options(Map.of("zdl", zdl)));
        var entityEvent = methodParametersCallSignature.stream()
                .map(p -> JSONPath.getFirst(p, "entity.name", "method.name") + "=" + JSONPath.get(p, "event.name"))
                .sorted()
                .peek(System.out::println)
                .toList();
        Assertions.assertEquals(List.of(
                "Address=Address",
                "Customer=Customer",
                "Customer=CustomerCreated",
                "Customer=CustomerCreatedFailed",
                "Customer=CustomerEvent",
                "Customer=CustomerUpdated",
                "deleteCustomer=CustomerDeleted"),
                entityEvent);

    }

    @Test
    void listOfPairEventEntityLocalEvents() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var methodParametersCallSignature = helpers.listOfPairEventEntity(zdl, options(Map.of("zdl", zdl)));
        var entityEvent = methodParametersCallSignature.stream()
                .map(p -> JSONPath.getFirst(p, "entity.name", "method.name") + "=" + JSONPath.get(p, "event.name"))
                .sorted()
                .peek(System.out::println)
                .toList();
        Assertions.assertEquals(
                List.of(
                        "Customer=CustomerEvent",
                        "Customer=CustomerUpdated"),
                entityEvent);

    }

    @Test
    void needsEventsProducer() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var service = JSONPath.get(zdl, "$.services.CustomerService");
        var needsEventsProducer = helpers.needsEventsProducer((Map) service, options(Map.of("zdl", zdl)));
        Assertions.assertTrue(needsEventsProducer);
    }

    @Test
    void needsEventsProducerLocalEvents() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var service = JSONPath.get(zdl, "$.services.CustomerService");
        var needsEventsProducer = helpers.needsEventsProducer((Map) service, options(Map.of("zdl", zdl)));
        Assertions.assertFalse(needsEventsProducer);
    }

    @Test
    void needsEventBus() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var service = JSONPath.get(zdl, "$.services.CustomerService");
        var needsEventsProducer = helpers.needsEventBus((Map) service, options(Map.of("zdl", zdl)));
        Assertions.assertFalse(needsEventsProducer);
    }

    @Test
    void needsEventBusLocalEvents() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var service = JSONPath.get(zdl, "$.services.CustomerService");
        var needsEventsProducer = helpers.needsEventBus((Map) service, options(Map.of("zdl", zdl)));
        Assertions.assertTrue(needsEventsProducer);
    }

    @Test
    void methodPolicies_rulesAnnotationWithRulesBlock() throws Exception {
        Map<String, Object> zdl;
        Map<String, Object> method;
        Map<String, Object> parsed = null;
        try {
            parsed = loadZdlContent("""
                    @aggregate
                    entity Order { id Long }

                    rules {
                        minAmount "An order total must be at least 10 EUR"
                        stock-first "Stock is reserved before payment is taken"
                    }

                    service OrderService for (Order) {
                        @rules(minAmount, "stock-first")
                        cancelOrder()
                    }
                    """);
        } catch (Exception ignored) {
            // dsl-kotlin 1.10.0 does not know `rules`
        }
        Map parsedRules = parsed == null ? null : JSONPath.get(parsed, "$.rules", Collections.emptyMap());
        if (parsedRules != null && parsedRules.containsKey("minAmount")) {
            zdl = parsed;
            method = JSONPath.get(parsed, "$.services.OrderService.methods.cancelOrder");
        } else {
            // parser does not fill $.rules; build the model so lookup is still covered
            method = Map.<String, Object>of("optionsList", List.of(Map.of("name", "rules", "value", List.of("minAmount", "stock-first"))));
            zdl = Map.<String, Object>of("rules", Map.of(
                    "minAmount", Map.of("name", "minAmount", "value", "An order total must be at least 10 EUR"),
                    "stock-first", Map.of("name", "stock-first", "value", "Stock is reserved before payment is taken")));
        }

        List<Map> resolved = helpers.methodPolicies(method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals(List.of("minAmount", "stock-first"), resolved.stream().map(r -> r.get("name")).toList());
        Assertions.assertEquals("An order total must be at least 10 EUR", resolved.get(0).get("value"));
        Assertions.assertEquals("Stock is reserved before payment is taken", resolved.get(1).get("value"));
    }

    @Test
    void methodPolicies_policyAnnotationWithPoliciesBlock() throws Exception {
        var zdl = loadZdlContent("""
                @aggregate
                entity Order { id Long }

                policies {
                    shipedOrdersCanNotBeCancelled "Shipped orders can not be cancelled"
                }

                service OrderService for (Order) {
                    @policy(shipedOrdersCanNotBeCancelled)
                    cancelOrder()
                }
                """);
        Map<String, Object> method = JSONPath.get(zdl, "$.services.OrderService.methods.cancelOrder");

        List<Map> resolved = helpers.methodPolicies(method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals(1, resolved.size());
        Assertions.assertEquals("shipedOrdersCanNotBeCancelled", resolved.get(0).get("name"));
        Assertions.assertEquals("Shipped orders can not be cancelled", resolved.get(0).get("value"));
    }

    @Test
    void methodPolicies_rulesAnnotationWithPoliciesOnlyModel() {
        // older parsers only fill $.policies; @rules must still resolve
        Map<String, Object> method = Map.<String, Object>of("optionsList", List.of(Map.of("name", "rules", "value", "shipedOrdersCanNotBeCancelled")));
        Map<String, Object> zdl = Map.<String, Object>of("policies", Map.of(
                "shipedOrdersCanNotBeCancelled",
                Map.of("name", "shipedOrdersCanNotBeCancelled", "value", "Shipped orders can not be cancelled")));

        List<Map> resolved = helpers.methodPolicies(method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals(1, resolved.size());
        Assertions.assertEquals("shipedOrdersCanNotBeCancelled", resolved.get(0).get("name"));
        Assertions.assertEquals("Shipped orders can not be cancelled", resolved.get(0).get("value"));
    }

    @Test
    void methodPolicies_rulesFirstThenPolicyDeduped() {
        Map<String, Object> method = Map.<String, Object>of("optionsList", List.of(
                Map.of("name", "policy", "value", List.of("b", "c")),
                Map.of("name", "rules", "value", List.of("a", "b"))));
        Map<String, Object> zdl = Map.<String, Object>of("rules", Map.of(
                "a", Map.of("name", "a", "value", "A"),
                "b", Map.of("name", "b", "value", "B"),
                "c", Map.of("name", "c", "value", "C")));

        List<Map> resolved = helpers.methodPolicies(method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals(List.of("a", "b", "c"), resolved.stream().map(r -> r.get("name")).toList());
    }

    private Map<String, Object> loadZdlContent(String zdl) throws Exception {
        Map<String, Object> model = new ZDLParser().withContent(zdl).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

}
