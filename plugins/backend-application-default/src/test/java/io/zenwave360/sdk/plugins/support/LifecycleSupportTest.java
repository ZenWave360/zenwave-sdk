package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class LifecycleSupportTest {

    private Map<String, Object> loadZdl(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void resolvesAggregateLifecycleMetadataAndTransitionSignatures() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var aggregate = (Map<String, Object>) JSONPath.get(zdl, "$.aggregates.CustomerOrderAggregate");
        var command = (Map<String, Object>) JSONPath.get(zdl, "$.aggregates.CustomerOrderAggregate.commands.updateKitchenStatus");

        Assertions.assertTrue(LifecycleSupport.hasLifecycle(zdl, aggregate));
        Assertions.assertTrue(LifecycleSupport.hasStateTransitions(aggregate));
        Assertions.assertEquals("OrderStatus", LifecycleSupport.lifecycleFieldType(zdl, aggregate));
        Assertions.assertEquals("CustomerOrderAggregateTransitions", LifecycleSupport.aggregateTransitionsClassName(aggregate));
        Assertions.assertEquals("ensureCanUpdateKitchenStatus", LifecycleSupport.transitionMethodName(command));
        Assertions.assertEquals(
                "OrderStatus.RECEIVED, OrderStatus.KITCHEN_ACCEPTED, OrderStatus.KITCHEN_IN_PROGRESS",
                LifecycleSupport.commandFromStatesSignature(zdl, command, aggregate));

        var transitionMethods = LifecycleSupport.aggregateTransitionMethods(zdl, aggregate).stream()
                .map(method -> (String) method.get("name"))
                .toList();
        Assertions.assertTrue(transitionMethods.contains("updateKitchenStatus"));
    }

    @Test
    void resolvesEntityLifecycleMetadataAcrossServices() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-lifecycle-entity.zdl");
        var entity = (Map<String, Object>) JSONPath.get(zdl, "$.entities.Order");
        var service = (Map<String, Object>) JSONPath.get(zdl, "$.services.OrderService");
        var method = (Map<String, Object>) JSONPath.get(zdl, "$.services.OrderService.methods.confirmOrder");

        Assertions.assertTrue(LifecycleSupport.hasEntityLifecycle(entity));
        Assertions.assertEquals("OrderStatus", LifecycleSupport.entityLifecycleFieldType(entity));
        Assertions.assertEquals("OrderTransitions", LifecycleSupport.entityServiceTransitionsClassName(entity));
        Assertions.assertTrue(LifecycleSupport.serviceHasEntityStateTransitions(service));
        Assertions.assertEquals(
                "OrderStatus.PLACED, OrderStatus.CONFIRMED",
                LifecycleSupport.entityCommandFromStatesSignature(method, entity));

        var transitionMethods = LifecycleSupport.entityServiceTransitionMethods(zdl, entity).stream()
                .map(m -> (String) m.get("name"))
                .toList();
        Assertions.assertEquals(List.of("placeOrder", "confirmOrder", "cancelOrder"), transitionMethods);
    }

    @Test
    void handlesMissingLifecycleGracefully() {
        var aggregate = Map.<String, Object>of("aggregateRoot", "Customer");
        var entity = Map.<String, Object>of("name", "Customer");
        var zdl = Map.<String, Object>of();

        Assertions.assertFalse(LifecycleSupport.hasLifecycle(zdl, aggregate));
        Assertions.assertFalse(LifecycleSupport.hasStateTransitions(aggregate));
        Assertions.assertFalse(LifecycleSupport.hasEntityLifecycle(entity));
        Assertions.assertEquals("", LifecycleSupport.entityLifecycleFieldType(entity));
        Assertions.assertTrue(LifecycleSupport.aggregateTransitionMethods(zdl, aggregate).isEmpty());
        Assertions.assertTrue(LifecycleSupport.entityServiceTransitionMethods(zdl, entity).isEmpty());
    }
}
