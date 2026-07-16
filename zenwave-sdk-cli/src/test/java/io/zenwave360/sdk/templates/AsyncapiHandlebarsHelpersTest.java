package io.zenwave360.sdk.templates;

import io.zenwave360.sdk.templating.AsyncapiHandlebarsHelpers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class AsyncapiHandlebarsHelpersTest {

    private final AsyncapiHandlebarsHelpers helpers = new AsyncapiHandlebarsHelpers(() -> null);

    @Test
    void readsAsyncapiModelProperties() {
        Map<String, Object> message = Map.of(
                "x--javaType", "com.example.OrderEvent",
                "x--javaTypeSimpleName", "OrderEvent");
        Map<String, Object> schema = Map.of("x--schema-name", "OrderPayload");

        Assertions.assertEquals("com.example.OrderEvent", helpers.javaType(message, null));
        Assertions.assertEquals("OrderEvent", helpers.javaTypeSimpleName(message, null));
        Assertions.assertEquals("OrderPayload", helpers.schemaName(schema, null));
    }

    @Test
    void returnsNullWhenThePropertyIsUnavailable() {
        Assertions.assertNull(helpers.javaType(null, null));
        Assertions.assertNull(helpers.javaTypeSimpleName("not-a-map", null));
        Assertions.assertNull(helpers.schemaName(Map.of(), null));
    }

    @Test
    void resolvesOperationMessagesFromTheCurrentApiModel() {
        Map<String, Object> message = Map.of("name", "OrderEvent");
        Map<String, Object> operation = Map.of("operationId", "publishOrder", "message", message);
        Map<String, Object> model = Map.of(
                "asyncapi", "2.6.0",
                "channels", Map.of("orders", Map.of("publish", operation)));
        AsyncapiHandlebarsHelpers helpers = new AsyncapiHandlebarsHelpers(() -> model);

        Assertions.assertEquals(List.of(message), helpers.operationMessages("publishOrder", null));
        Assertions.assertTrue(helpers.operationMessages(null, null).isEmpty());
    }
}
