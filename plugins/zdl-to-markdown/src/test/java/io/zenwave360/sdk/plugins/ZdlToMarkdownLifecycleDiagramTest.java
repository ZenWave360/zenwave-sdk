package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class ZdlToMarkdownLifecycleDiagramTest {

    @Test
    void buildAggregateLifecyclePlantUml_rendersAggregateTransitions() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var aggregate = map(JSONPath.get(zdl, "$.aggregates.CustomerOrderAggregate"));
        var rootEntity = map(JSONPath.get(zdl, "$.entities.CustomerOrder"));

        String plantUml = invokeString("buildAggregateLifecyclePlantUml", aggregate, rootEntity);

        Assertions.assertNotNull(plantUml);
        Assertions.assertTrue(plantUml.contains("title CustomerOrderAggregate lifecycle"));
        Assertions.assertTrue(plantUml.contains("[*] --> RECEIVED : initialState"));
        Assertions.assertTrue(plantUml.contains("KITCHEN_READY --> DELIVERED : updateDeliveryStatus(input)\\n/ OrderEvent, OrderStatusUpdated"));
        Assertions.assertTrue(plantUml.contains("state \"CANCELLED\" as CANCELLED <<terminal>>"));
        Assertions.assertTrue(plantUml.contains("Aggregate root: CustomerOrder"));
    }

    @Test
    void buildEntityServiceLifecyclePlantUml_rendersServiceTransitions() throws Exception {
        var service = Map.of(
                "name", "OrdersService",
                "methods", Map.of(
                        "updateOrder", Map.of(
                                "name", "updateOrder",
                                "entity", "CustomerOrder",
                                "paramId", "id",
                                "parameter", "CustomerOrderInput",
                                "from", List.of("RECEIVED"),
                                "to", "KITCHEN_IN_PROGRESS",
                                "withEvents", List.of("OrderEvent", "OrderStatusUpdated")
                        ),
                        "cancelOrder", Map.of(
                                "name", "cancelOrder",
                                "entity", "CustomerOrder",
                                "paramId", "id",
                                "parameter", "CancelOrderInput",
                                "transition", Map.of(
                                        "from", List.of("RECEIVED", "KITCHEN_READY"),
                                        "to", "CANCELLED"
                                )
                        )
                ));
        var entity = Map.of(
                "name", "CustomerOrder",
                "lifecycle", Map.of(
                        "field", "status",
                        "initial", "RECEIVED"
                ));

        String plantUml = invokeString("buildEntityServiceLifecyclePlantUml", service, entity);

        Assertions.assertNotNull(plantUml);
        Assertions.assertTrue(plantUml.contains("title CustomerOrder lifecycle (via OrdersService)"));
        Assertions.assertTrue(plantUml.contains("[*] --> RECEIVED : initialState"));
        Assertions.assertTrue(plantUml.contains("RECEIVED --> KITCHEN_IN_PROGRESS : updateOrder(id, input)\\n/ OrderEvent, OrderStatusUpdated"));
        Assertions.assertTrue(plantUml.contains("KITCHEN_READY --> CANCELLED : cancelOrder(id, input)"));
        Assertions.assertTrue(plantUml.contains("Entity: CustomerOrder"));
        Assertions.assertTrue(plantUml.contains("Service: OrdersService"));
    }

    @Test
    void buildEntityServiceLifecyclePlantUml_returnsNullWhenEntityHasNoLifecycle() throws Exception {
        var service = Map.of(
                "name", "Service",
                "methods", Map.of("doWork", Map.of(
                        "name", "doWork",
                        "entity", "PlainEntity",
                        "from", List.of("A"),
                        "to", "B"
                )));
        var entity = Map.of("name", "PlainEntity");

        String plantUml = invokeString("buildEntityServiceLifecyclePlantUml", service, entity);

        Assertions.assertNull(plantUml);
    }

    @Test
    void buildAggregateLifecyclePlantUml_usesRootEntityLifecycleWhenAggregateLifecycleMissing() throws Exception {
        var aggregate = Map.of(
                "name", "TestAggregate",
                "aggregateRoot", "TestRoot",
                "commands", List.of(Map.of(
                        "name", "advance",
                        "from", List.of("awaiting-review"),
                        "to", "done!"
                )));
        var rootEntity = Map.of(
                "name", "TestRoot",
                "lifecycle", Map.of(
                        "field", "state",
                        "initialState", "awaiting-review"
                ));

        String plantUml = invokeString("buildAggregateLifecyclePlantUml", aggregate, rootEntity);

        Assertions.assertNotNull(plantUml);
        Assertions.assertTrue(plantUml.contains("[*] --> awaiting_review : initialState"));
        Assertions.assertTrue(plantUml.contains("state \"done!\" as done_ <<terminal>>"));
        Assertions.assertTrue(plantUml.contains("Status field: state"));
    }

    @Test
    void sanitizeStateId_andTransitionLabel_coverEdgeCases() throws Exception {
        String sanitized = invokeString("sanitizeStateId", "123 done!");
        String emptyFallback = invokeString("sanitizeStateId", "!!!");
        String nullLabel = invokeString("transitionLabel", Map.of("name", "publish"));
        String complexLabel = invokeString("transitionLabel", Map.of(
                "name", "publish",
                "paramId", "id",
                "parameter", "PublishInput",
                "withEvents", List.of("Published", List.of("AuditLogged", "MetricsUpdated"))
        ));

        Assertions.assertEquals("S_123_done_", sanitized);
        Assertions.assertEquals("___", emptyFallback);
        Assertions.assertEquals("publish()", nullLabel);
        Assertions.assertEquals("publish(id, input)\\n/ Published, AuditLogged, MetricsUpdated", complexLabel);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadZdl(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        return (Map<String, Object>) model.get("zdl");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static String invokeString(String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = Map.class;
            if (!(args[i] instanceof Map)) {
                parameterTypes[i] = String.class;
            }
        }
        Method method = ZdlToMarkdownGenerator.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return (String) method.invoke(null, args);
    }
}
