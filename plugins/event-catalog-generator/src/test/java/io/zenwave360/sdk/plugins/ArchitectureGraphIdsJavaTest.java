package io.zenwave360.sdk.plugins;

import io.zenwave360.architecture.graph.ArchitectureGraphIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureGraphIdsJavaTest {

    @Test
    void exposesStableAsyncApiAndOpenApiResourceIdsToJavaConsumers() {
        assertEquals(
                "zw:orders/checkout/channel/orders%2F%7Bid%7D",
                ArchitectureGraphIds.channel("orders/checkout", "async api#1", "orders/{id}"));
        assertEquals(
                "zw:orders/checkout/api-operation/GET%20%2Forders%2F%7Bid%7D",
                ArchitectureGraphIds.apiOperation("orders/checkout", "open api#1", "GET /orders/{id}"));
    }
}
