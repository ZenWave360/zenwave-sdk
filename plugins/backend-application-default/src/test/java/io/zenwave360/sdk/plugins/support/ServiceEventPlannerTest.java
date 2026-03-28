package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ServiceEventPlannerTest {

    private Map<String, Object> loadZdl(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void classifiesAsyncApiAndDomainEventInfrastructureNeeds() throws Exception {
        var externalZdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var localZdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var externalService = (Map<String, Object>) JSONPath.get(externalZdl, "$.services.CustomerService");
        var localService = (Map<String, Object>) JSONPath.get(localZdl, "$.services.CustomerService");

        Assertions.assertTrue(ServiceEventPlanner.needsEventsProducer(externalZdl, externalService));
        Assertions.assertFalse(ServiceEventPlanner.needsEventBus(externalZdl, externalService));
        Assertions.assertFalse(ServiceEventPlanner.needsEventsProducer(localZdl, localService));
        Assertions.assertTrue(ServiceEventPlanner.needsEventBus(localZdl, localService));
    }

    @Test
    void plansAggregateProducedEventsForServiceMethod() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var method = (Map<String, Object>) JSONPath.get(zdl, "$.services.OrdersService.methods.updateKitchenStatus");

        var publications = ServiceEventPlanner.serviceMethodEventPublications(zdl, method).stream()
                .map(publication -> publication.get("eventName") + ":" + publication.get("producedByAggregate") + ":" + publication.get("isAsyncApi"))
                .toList();

        Assertions.assertEquals(List.of(
                "OrderEvent:true:true",
                "OrderStatusUpdated:true:true"
        ), publications);
    }

    @Test
    void returnsEmptyPlanForMethodsWithoutEvents() {
        var zdl = Map.<String, Object>of();
        var method = Map.<String, Object>of("name", "noop");

        Assertions.assertTrue(ServiceEventPlanner.serviceMethodEventPublications(zdl, method).isEmpty());
    }
}
