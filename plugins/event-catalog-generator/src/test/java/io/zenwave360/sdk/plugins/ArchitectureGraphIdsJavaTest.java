package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.graph.ArchitectureGraphIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureGraphIdsJavaTest {

    @Test
    void exposesStableAsyncApiAndOpenApiResourceIdsToJavaConsumers() {
        assertEquals(
                "artifact/orders%002Fcheckout/async%0020api%00231/channel/channels.orders%002F%007Bid%007D",
                ArchitectureGraphIds.channel("orders/checkout", "async api#1", "orders/{id}"));
        assertEquals(
                "artifact/orders%002Fcheckout/open%0020api%00231/api_operation/operations.GET%0020%002Forders%002F%007Bid%007D",
                ArchitectureGraphIds.apiOperation("orders/checkout", "open api#1", "GET /orders/{id}"));
    }
}
