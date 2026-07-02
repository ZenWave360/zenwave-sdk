package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class YamlOverlyMergerTest {

    private Map<String, Object> loadYamlFromResource(String resource) throws Exception {
        Map<String, Object> parsed = new DefaultYamlParser()
                .withApiFile(URI.create(resource))
                .parse();
        return (Map<String, Object>) parsed.get("api");
    }

    @Test
    public void testMergeCustomerAddressOpenAPIWithMerger() throws Exception {
        // Given
        String baseYaml = "classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml";
        String mergerYaml = "classpath:io/zenwave360/sdk/resources/openapi/openapi-merger.yml";

        Map<String, Object> base = loadYamlFromResource(baseYaml);
        Map<String, Object> merger = loadYamlFromResource(mergerYaml);

        // When
        Map<String, Object> result = YamlOverlyMerger.merge(base, merger);

        // Then
        // Verify original base is untouched
        Assertions.assertEquals(
            "Zenwave 360 Generated API",
            JSONPath.get(base, "$.info.title"),
            "Original base should remain unchanged"
        );

        // Verify merged values
        Assertions.assertNull(
            JSONPath.get(result, "$.security[0].basicAuth[0]"),
            "Security scheme should be merged"
        );

        // Verify original values are preserved when not overwritten
        Assertions.assertEquals(
            "0.0.1",
            JSONPath.get(base, "$.info.version"),
            "Original values should be preserved"
        );
        Assertions.assertEquals(
            "1.0.0",
            JSONPath.get(result, "$.info.version"),
            "New values should be merged"
        );

        // Verify servers section is merged correctly
        Assertions.assertEquals(
            "http://localhost:8080/api/webapp",
            JSONPath.get(result, "$.servers[0].url"),
            "First server URL should be merged correctly"
        );

        // Verify second server configuration
        Assertions.assertEquals(
            "{protocol}://{server}/{path}",
            JSONPath.get(result, "$.servers[1].url"),
            "Second server URL template should be merged correctly"
        );
        Assertions.assertEquals(
            "http",
            JSONPath.get(result, "$.servers[1].variables.protocol.default"),
            "Second server protocol variable should be merged correctly"
        );
        Assertions.assertEquals(
            "localhost:8080",
            JSONPath.get(result, "$.servers[1].variables.server.default"),
            "Second server hostname variable should be merged correctly"
        );
    }

    @Test
    public void testApplyCustomerAddressOpenAPIWithOverlay() throws Exception {
        // Given
        String baseYaml = "classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml";
        String overlayYaml = "classpath:io/zenwave360/sdk/resources/openapi/openapi-overlay.yml";

        Map<String, Object> base = loadYamlFromResource(baseYaml);
        Map<String, Object> overlay = loadYamlFromResource(overlayYaml);

        // When
        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay);

        // Then
        // Verify info section is updated
        Assertions.assertEquals(
            "My Organization (Overlayed) - WebApp API",
            JSONPath.get(result, "$.info.title"),
            "API title should be updated"
        );
        Assertions.assertEquals(
            "1.0.0",
            JSONPath.get(result, "$.info.version"),
            "API version should be updated"
        );
        Assertions.assertEquals(
            "me@email.com",
            JSONPath.get(result, "$.info.contact.email"),
            "Contact email should be updated"
        );

        // Verify servers section is updated
        Assertions.assertEquals(
            "http://localhost:8080/api",
            JSONPath.get(result, "$.servers[0].url"),
            "First server URL should be updated"
        );
        Assertions.assertEquals(
            "{protocol}://{server}/{path}",
            JSONPath.get(result, "$.servers[1].url"),
            "Second server URL template should be updated"
        );

        // Verify security schemes
        Assertions.assertNull(
            JSONPath.get(result, "$.components.securitySchemes.basicAuth"),
            "basicAuth security scheme should be removed"
        );
        Assertions.assertEquals(
            "bearer",
            JSONPath.get(result, "$.components.securitySchemes.bearerAuth.scheme"),
            "bearerAuth scheme should be present"
        );

        // Verify security requirements
        List<Map<String, Object>> security = JSONPath.get(result, "$.security");
        Assertions.assertEquals(1, security.size(), "Should have only one security requirement");
        Assertions.assertTrue(
            security.get(0).containsKey("bearerAuth"),
            "Security should contain bearerAuth"
        );
        Assertions.assertFalse(
            security.stream().anyMatch(s -> s.containsKey("basicAuth")),
            "Security should not contain basicAuth"
        );

        // Verify original base is untouched
        Assertions.assertEquals(
            "Zenwave 360 Generated API",
            JSONPath.get(base, "$.info.title"),
            "Original base should remain unchanged"
        );
    }

    @Test
    public void testApplyOverlayUpdateToRootTarget() {
        // Given
        Map<String, Object> inventoryChannel = new LinkedHashMap<>();
        inventoryChannel.put("address", "inventory");
        inventoryChannel.put("messages", Map.of("StockReleased", Map.of("$ref", "#/components/messages/StockReleased")));

        Map<String, Object> base = new LinkedHashMap<>();
        base.put("asyncapi", "3.0.0");
        base.put("info", Map.of("title", "Arcadia Editions - Catalog Inventory"));
        base.put("channels", Map.of("inventoryChannel", inventoryChannel));

        Map<String, Object> overlay = Map.of(
            "actions", List.of(
                Map.of(
                    "target", "$",
                    "update", Map.of("asyncapi", "3.1.0")
                ),
                Map.of(
                    "target", "$",
                    "update", Map.of(
                        "servers", Map.of(
                            "develop", Map.of("$ref", "https://example.com/asyncapi.yml#/servers/develop")
                        )
                    )
                ),
                Map.of(
                    "target", "$.channels.*",
                    "update", Map.of(
                        "bindings", Map.of(
                            "$ref", "https://example.com/asyncapi.yml#/components/channelBindings/kafka"
                        )
                    )
                )
            )
        );

        // When
        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay);

        // Then
        Assertions.assertEquals("3.1.0", JSONPath.get(result, "$.asyncapi"));
        Assertions.assertEquals(
            "Arcadia Editions - Catalog Inventory",
            JSONPath.get(result, "$.info.title"),
            "A root update should preserve unrelated document fields"
        );
        Assertions.assertEquals(
            "inventory",
            JSONPath.get(result, "$.channels.inventoryChannel.address"),
            "A root update should preserve unrelated nested fields"
        );
        Assertions.assertEquals(
            "#/components/messages/StockReleased",
            JSONPath.get(result, "$.channels.inventoryChannel.messages.StockReleased['$ref']"),
            "An object update selected by a wildcard should preserve existing properties"
        );
        Assertions.assertEquals(
            "https://example.com/asyncapi.yml#/components/channelBindings/kafka",
            JSONPath.get(result, "$.channels.inventoryChannel.bindings['$ref']")
        );
        Map<String, Object> updatedChannel = JSONPath.get(result, "$.channels.inventoryChannel");
        Assertions.assertEquals(
            List.of("address", "messages", "bindings"),
            new ArrayList<>(updatedChannel.keySet()),
            "Existing key order should be retained and new keys should be appended"
        );
        Assertions.assertEquals(
            "https://example.com/asyncapi.yml#/servers/develop",
            JSONPath.get(result, "$.servers.develop['$ref']"),
            "Multiple root updates should be applied in order"
        );
        Assertions.assertEquals(
            "3.0.0",
            JSONPath.get(base, "$.asyncapi"),
            "Original base should remain unchanged"
        );
    }

    @Test
    public void testMergeAndOverlayFormatsAsyncAPIRootElements() throws Exception {
        String asyncapi = """
            asyncapi: 3.0.0
            info:
              title: Catalog Inventory
              version: 1.0.0
            channels:
              inventory:
                address: inventory
            components:
              messages: {}
            """;
        String overlay = """
            overlay: 1.1.0
            info:
              title: Shared defaults
              version: 1.0.0
            actions:
              - target: $
                update:
                  asyncapi: 3.1.0
                  servers:
                    develop:
                      host: localhost:9092
            """;

        String result = YamlOverlyMerger.mergeAndOverlay(
            asyncapi,
            null,
            List.of("overlay"),
            ignored -> overlay
        );

        Assertions.assertTrue(result.startsWith("asyncapi: 3.1.0\n"));
        Assertions.assertFalse(result.startsWith("---"));
        Assertions.assertTrue(result.startsWith("asyncapi: 3.1.0\ninfo:"));
        Assertions.assertTrue(result.contains("\n\nservers:"));
        Assertions.assertTrue(result.contains("\n\nchannels:"));
        Assertions.assertTrue(result.contains("\n\ncomponents:"));
    }
}
