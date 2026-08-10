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

    private Map<String, Object> overlay(String version, Map<String, Object>... actions) {
        return Map.of(
            "overlay", version,
            "info", Map.of(
                "title", "Test overlay",
                "version", "1.0.0",
                "description", "Overlay description"
            ),
            "actions", List.of(actions)
        );
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
            "overlay", "1.1.0",
            "info", Map.of(
                "title", "Update AsyncAPI",
                "version", "1.0.0"
            ),
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
              description: Verifies Overlay 1.1 metadata
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

    @Test
    public void testOverlay11UpdatesPrimitiveTargetsDirectly() {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("info", new LinkedHashMap<>(Map.of("title", "Internal API", "version", "1.0.0")));

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.3",
            Map.of("target", "$.info.title", "update", "Public API")
        ));

        Assertions.assertEquals("Public API", JSONPath.get(result, "$.info.title"));
        Assertions.assertEquals("Internal API", JSONPath.get(base, "$.info.title"));
    }

    @Test
    public void testOverlay11UpdatesPrimitiveTargetsToNull() {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("target", "$.info.description");
        update.put("update", null);

        Map<String, Object> base = Map.of(
            "info", new LinkedHashMap<>(Map.of("description", "Present"))
        );
        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay("1.1.0", update));

        Map<String, Object> info = JSONPath.get(result, "$.info");
        Assertions.assertTrue(info.containsKey("description"));
        Assertions.assertNull(info.get("description"));
    }

    @Test
    public void testOverlay11RemovesMultiplePrimitiveArrayElementsWithoutIndexShift() {
        Map<String, Object> base = Map.of(
            "tags", new ArrayList<>(List.of("public", "dummy", "dummy", "stable"))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of("target", "$.tags[?(@ == 'dummy')]", "remove", true)
        ));

        Assertions.assertEquals(List.of("public", "stable"), JSONPath.get(result, "$.tags"));
        Assertions.assertEquals(
            List.of("public", "dummy", "dummy", "stable"),
            JSONPath.get(base, "$.tags"),
            "Nested arrays in the original document must not be mutated"
        );
    }

    @Test
    public void testOverlay11CopiesObjectIntoExistingObject() {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("paths", new LinkedHashMap<>(Map.of(
            "/items", new LinkedHashMap<>(Map.of(
                "get", new LinkedHashMap<>(Map.of("description", "List items"))
            )),
            "/some-items", new LinkedHashMap<>(Map.of(
                "delete", new LinkedHashMap<>(Map.of("description", "Delete items"))
            ))
        )));

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of(
                "target", "$.paths['/some-items']",
                "copy", "$.paths['/items']"
            )
        ));

        Assertions.assertEquals("List items", JSONPath.get(result, "$.paths['/some-items'].get.description"));
        Assertions.assertEquals("Delete items", JSONPath.get(result, "$.paths['/some-items'].delete.description"));
        Assertions.assertNull(JSONPath.get(base, "$.paths['/some-items'].get"));
    }

    @Test
    public void testOverlay11CopiesThenRemovesToRenamePath() {
        Map<String, Object> base = Map.of(
            "paths", new LinkedHashMap<>(Map.of(
                "/items", new LinkedHashMap<>(Map.of(
                    "get", new LinkedHashMap<>(Map.of("description", "List items"))
                ))
            ))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of("target", "$.paths", "update", Map.of("/new-items", Map.of())),
            Map.of("target", "$.paths['/new-items']", "copy", "$.paths['/items']"),
            Map.of("target", "$.paths['/items']", "remove", true)
        ));

        Assertions.assertEquals("List items", JSONPath.get(result, "$.paths['/new-items'].get.description"));
        Assertions.assertNull(JSONPath.get(result, "$.paths['/items']"));
    }

    @Test
    public void testOverlay11CopySourceMustSelectExactlyOneNode() {
        Map<String, Object> base = Map.of(
            "source", Map.of("one", Map.of("value", 1), "two", Map.of("value", 2)),
            "target", new LinkedHashMap<>()
        );

        IllegalArgumentException noSource = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.1.0",
                Map.of("target", "$.target", "copy", "$.missing")
            ))
        );
        Assertions.assertTrue(noSource.getMessage().contains("selected 0"));

        IllegalArgumentException multipleSources = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.1.0",
                Map.of("target", "$.target", "copy", "$.source.*")
            ))
        );
        Assertions.assertTrue(multipleSources.getMessage().contains("selected 2"));
    }

    @Test
    public void testOverlay11RejectsMixedTargetCategories() {
        Map<String, Object> base = Map.of(
            "object", new LinkedHashMap<>(),
            "primitive", "value"
        );

        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.1.0",
                Map.of("target", "$['object','primitive']", "update", Map.of("added", true))
            ))
        );

        Assertions.assertTrue(
            exception.getMessage().contains("must all be objects, arrays, or primitives"),
            exception::getMessage
        );
    }

    @Test
    public void testOverlay11ZeroTargetsAreANoOp() {
        Map<String, Object> base = Map.of("info", Map.of("title", "API"));

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of("target", "$.missing", "update", Map.of("description", "Ignored"))
        ));

        Assertions.assertEquals(base, result);
        Assertions.assertNotSame(base, result);
    }

    @Test
    public void testOverlay11RecursivelyMergesObjectsAndConcatenatesArrays() {
        Map<String, Object> base = Map.of(
            "schema", new LinkedHashMap<>(Map.of(
                "type", "object",
                "required", new ArrayList<>(List.of("id")),
                "properties", new LinkedHashMap<>(Map.of(
                    "id", new LinkedHashMap<>(Map.of("type", "string"))
                ))
            ))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of(
                "target", "$.schema",
                "update", Map.of(
                    "required", List.of("name"),
                    "properties", Map.of(
                        "name", Map.of("type", "string")
                    )
                )
            )
        ));

        Assertions.assertEquals(List.of("id", "name"), JSONPath.get(result, "$.schema.required"));
        Assertions.assertEquals("string", JSONPath.get(result, "$.schema.properties.id.type"));
        Assertions.assertEquals("string", JSONPath.get(result, "$.schema.properties.name.type"));
    }

    @Test
    public void testOverlay11RejectsIncompatibleRecursiveMergeValues() {
        Map<String, Object> base = Map.of(
            "schema", new LinkedHashMap<>(Map.of("required", new ArrayList<>(List.of("id"))))
        );

        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.1.0",
                Map.of("target", "$.schema", "update", Map.of("required", "id"))
            ))
        );

        Assertions.assertTrue(exception.getMessage().contains("incompatible values for property 'required'"));
    }

    @Test
    public void testOverlay11AppendsOrConcatenatesArrayUpdates() {
        Map<String, Object> base = Map.of(
            "parameters", new ArrayList<>(List.of(Map.of("name", "existing")))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of("target", "$.parameters", "update", Map.of("name", "appended")),
            Map.of("target", "$.parameters", "update", List.of(
                Map.of("name", "concatenated-one"),
                Map.of("name", "concatenated-two")
            ))
        ));

        Assertions.assertEquals(
            List.of("existing", "appended", "concatenated-one", "concatenated-two"),
            JSONPath.get(result, "$.parameters[*].name")
        );
    }

    @Test
    public void testOverlayModifierPrecedenceIsRemoveThenUpdateThenCopy() {
        Map<String, Object> base = Map.of(
            "values", new LinkedHashMap<>(Map.of(
                "removeMe", "original",
                "updateMe", "original",
                "copySource", "copied"
            ))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.1.0",
            Map.of(
                "target", "$.values.removeMe",
                "remove", true,
                "update", "updated",
                "copy", "$.values.copySource"
            ),
            Map.of(
                "target", "$.values.updateMe",
                "update", "updated",
                "copy", "$.values.copySource"
            )
        ));

        Assertions.assertNull(JSONPath.get(result, "$.values.removeMe"));
        Assertions.assertEquals("updated", JSONPath.get(result, "$.values.updateMe"));
    }

    @Test
    public void testOverlay10PatchVersionsRemainSupportedWithLegacyArrayReplacement() {
        Map<String, Object> base = Map.of(
            "servers", new ArrayList<>(List.of(Map.of("url", "old")))
        );

        Map<String, Object> result = YamlOverlyMerger.applyOverlay(base, overlay(
            "1.0.9",
            Map.of("target", "$.servers", "update", List.of(Map.of("url", "new")))
        ));

        Assertions.assertEquals(List.of("new"), JSONPath.get(result, "$.servers[*].url"));
    }

    @Test
    public void testOverlay10RejectsCopyAndPrimitiveTargets() {
        Map<String, Object> base = Map.of(
            "info", new LinkedHashMap<>(Map.of("title", "API")),
            "copySource", Map.of("description", "Copied")
        );

        IllegalArgumentException copyException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.0.0",
                Map.of("target", "$.info", "copy", "$.copySource")
            ))
        );
        Assertions.assertTrue(copyException.getMessage().contains("requires Overlay 1.1"));

        IllegalArgumentException primitiveException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.0.0",
                Map.of("target", "$.info.title", "update", "Renamed")
            ))
        );
        Assertions.assertTrue(primitiveException.getMessage().contains("only objects or arrays"));
    }

    @Test
    public void testOverlayRejectsUnsupportedVersionsAndModifierlessActions() {
        Map<String, Object> base = Map.of("info", Map.of("title", "API"));

        IllegalArgumentException versionException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.2.0",
                Map.of("target", "$.info", "update", Map.of("description", "Ignored"))
            ))
        );
        Assertions.assertTrue(versionException.getMessage().contains("Unsupported Overlay feature set 1.2"));

        IllegalArgumentException modifierException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> YamlOverlyMerger.applyOverlay(base, overlay(
                "1.1.0",
                Map.of("target", "$.info")
            ))
        );
        Assertions.assertTrue(modifierException.getMessage().contains("must define update, copy, or remove"));
    }
}
