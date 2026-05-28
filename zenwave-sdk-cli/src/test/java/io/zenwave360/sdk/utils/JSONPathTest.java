package io.zenwave360.sdk.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JSONPathTest {

    @Test
    void testGetFirstReturnsFirstNonNullMatch() {
        Map<String, Object> document = Map.of(
                "customer", Map.of(
                        "id", "customer-1",
                        "name", "Ada"
                ),
                "order", Map.of(
                        "id", "order-42"
                )
        );

        String value = JSONPath.getFirst(document, "$.missing", "$.order.id", "$.customer.id");

        assertEquals("order-42", value);
    }

    @Test
    void testGetFirstReturnsNullWhenNoPathMatches() {
        Map<String, Object> document = Map.of("customer", Map.of("id", "customer-1"));

        String value = JSONPath.getFirst(document, "$.missing", "$.customer.missing");

        assertNull(value);
    }

    @Test
    void testGetFirstNonEmptyReturnsFirstNonEmptyCollection() {
        Map<String, Object> document = Map.of(
                "customer", Map.of(
                        "tags", List.of(),
                        "roles", List.of("admin", "billing")
                ),
                "order", Map.of(
                        "items", List.of("sku-1")
                )
        );

        List<String> value = JSONPath.getFirstNonEmpty(document, "$.customer.tags", "$.customer.roles", "$.order.items");

        assertEquals(List.of("admin", "billing"), value);
    }

    @Test
    void testGetFirstNonEmptyReturnsNullWhenOnlyNullOrEmptyCollectionsExist() {
        Map<String, Object> document = Map.of(
                "customer", Map.of(
                        "tags", List.of(),
                        "name", "Ada"
                )
        );

        List<String> value = JSONPath.getFirstNonEmpty(document, "$.missing", "$.customer.tags");

        assertNull(value);
    }
}
