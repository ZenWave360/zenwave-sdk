package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventCatalogImporterTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String EVENT_CATALOG_CONTENT = "src/test/resources/event-catalog-content";
    private static final String OUTPUT_FOLDER = "target/event-catalog-import-test";

    @TempDir
    Path tempDir;

    @Test
    void importsEventCatalogHierarchyIntoZenWaveManifest() throws Exception {
        runImporter(EVENT_CATALOG_CONTENT, OUTPUT_FOLDER + "/zenwave-architecture.yml");

        Map<String, Object> manifest = readManifest();
        Map<String, Object> config = asMap(manifest.get("config"));
        assertEquals(List.of("workspace"), config.get("contentResolution"));
        assertFalse(config.containsKey("sourcePriority"));
        assertFalse(config.containsKey("naming"));

        Map<String, Object> domains = asMap(manifest.get("domains"));
        assertTrue(domains.containsKey("merchandising"));

        Map<String, Object> merchandising = asMap(domains.get("merchandising"));
        assertFalse(merchandising.containsKey("docs"));
        Map<String, Object> subdomains = asMap(merchandising.get("subdomains"));
        Map<String, Object> inventory = asMap(subdomains.get("inventory"));
        assertFalse(inventory.containsKey("docs"));
        Map<String, Object> services = asMap(inventory.get("services"));
        Map<String, Object> service = asMap(services.get("inventory-adjustment"));

        assertFalse(service.containsKey("id"));
        assertFalse(service.containsKey("path"));
        assertEquals("EVENT_CATALOG.md", asMap(service.get("docs")).get("content"));

        List<Map<String, Object>> artifacts = asListOfMaps(service.get("artifacts"));
        assertTrue(artifacts.stream().anyMatch(artifact -> "asyncapi".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "openapi".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "zdl".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().allMatch(artifact -> artifact.keySet().stream()
                .allMatch(key -> List.of("name", "artifactId", "type", "path", "version").contains(key))));

        File serviceDoc = new File(OUTPUT_FOLDER, "merchandising/inventory/inventory-adjustment/EVENT_CATALOG.md");
        assertTrue(serviceDoc.exists());
        assertTrue(Files.readString(serviceDoc.toPath()).contains("Inventory Adjustment Service"));

        File zdl = new File(OUTPUT_FOLDER, "merchandising/inventory/inventory-adjustment/domain-model.zdl");
        assertTrue(zdl.exists());
        String zdlContent = Files.readString(zdl.toPath());
        assertTrue(zdlContent.contains("@aggregate"));
        assertTrue(zdlContent.contains("entity InventoryItem"));
        assertTrue(zdlContent.contains("sku String required"));
    }

    @Test
    void reportsMissingSourceSpecsWithoutEmittingInvalidPlaceholderArtifacts() throws Exception {
        File fixture = new File(OUTPUT_FOLDER, "minimal-catalog");
        File serviceDir = new File(fixture, "domains/sales/subdomains/sales.orders/services/sales.orders.order-service/events/sales.orders.order-service.order-created");
        assertTrue(serviceDir.mkdirs() || serviceDir.exists());
        Files.writeString(new File(fixture, "domains/sales/index.mdx").toPath(), """
                ---
                id: sales
                name: Sales
                ---
                Sales docs
                """);
        File subdomain = new File(fixture, "domains/sales/subdomains/sales.orders");
        assertTrue(subdomain.mkdirs() || subdomain.exists());
        Files.writeString(new File(subdomain, "index.mdx").toPath(), """
                ---
                id: sales.orders
                name: Orders
                ---
                Orders docs
                """);
        File service = new File(fixture, "domains/sales/subdomains/sales.orders/services/sales.orders.order-service");
        assertTrue(service.mkdirs() || service.exists());
        Files.writeString(new File(service, "index.mdx").toPath(), """
                ---
                id: sales.orders.order-service
                name: Order Service
                ---
                Service docs
                """);
        Files.writeString(new File(serviceDir, "index.mdx").toPath(), """
                ---
                id: sales.orders.order-service.order-created
                name: Order Created
                ---
                Event docs
                """);

        runImporter(fixture.getPath(), OUTPUT_FOLDER + "/minimal-output/zenwave-architecture.yml");

        Map<String, Object> manifest = YAML.readValue(new File(OUTPUT_FOLDER, "minimal-output/zenwave-architecture.yml"), MAP_TYPE);
        Map<String, Object> serviceMap = asMap(asMap(asMap(asMap(manifest.get("domains")).get("sales")).get("subdomains")).get("orders"));
        Map<String, Object> importedService = asMap(asMap(serviceMap.get("services")).get("order-service"));
        List<Map<String, Object>> artifacts = asListOfMaps(importedService.get("artifacts"));
        assertTrue(artifacts.isEmpty());
    }

    @Test
    void importsRichCatalogWithOwnedArtifactsAndMixedSpecificationLocations() throws Exception {
        Path catalog = tempDir.resolve("catalog");
        Path domain = catalog.resolve("domains/fulfillment");
        Path subdomain = domain.resolve("subdomains/fulfillment.shipping");
        Path service = subdomain.resolve("services/fulfillment.shipping.delivery");

        write(domain.resolve("index.mdx"), "---\r\nid: fulfillment\r\nname: Fulfillment\r\n---\r\nDomain docs\r\n");
        write(subdomain.resolve("index.mdx"), page("fulfillment.shipping", "Shipping", "Shipping docs"));
        write(service.resolve("index.mdx"), """
                ---
                id: fulfillment.shipping.delivery
                name: 'Delivery \\ "API"'
                version: 4.2.0
                summary: Delivery coordination
                specifications:
                  - type: asyncapi
                    path: specs/asyncapi.yml
                  - type: openapi
                    path: https://example.test/openapi.yml
                  - type: graphql
                    path: %s
                  - path: ignored-missing-type.yml
                  - type: openapi
                ---
                """.formatted(tempDir.resolve("absolute.graphql").toString().replace("\\", "/")));

        write(service.resolve("events/order-shipped/index.mdx"), page("fulfillment.shipping.delivery.order-shipped", "Order Shipped", "Event docs"));
        write(service.resolve("commands/ship-order/index.md"), page("fulfillment.shipping.delivery.ship-order", "Ship Order", "Command docs"));
        write(service.resolve("queries/find-shipment/index.mdx"), page("fulfillment.shipping.delivery.find-shipment", "Find Shipment", "Query docs"));
        write(service.resolve("flows/delivery-flow/index.mdx"), page("delivery-flow", "Delivery Flow", "Flow docs"));
        write(service.resolve("data-products/shipping-insights/index.mdx"), page("shipping-insights", "Shipping Insights", "Data product docs"));
        write(service.resolve("diagrams/context/index.mdx"), page("shipping-context", "Shipping Context", "Diagram docs"));
        write(service.resolve("unknown/ignored/index.mdx"), page("ignored", "Ignored", "Ignored docs"));
        write(service.resolve("entities/shipment/index.mdx"), """
                ---
                id: fulfillment.shipping.delivery.shipment
                name: Shipment
                summary: 'Shipment "aggregate"'
                aggregateRoot: true
                properties:
                  - name: trackingId
                    type: UUID
                    required: true
                  - name: amount
                    type: BigDecimal
                  - name: carrier-code
                    type: custom-code
                  - name: note
                  - ignored-non-map
                ---
                Entity docs
                """);
        write(service.resolve("entities/anonymous/index.mdx"), """
                ---
                properties:
                  - name: value
                    type: string
                ---
                """);

        Path idle = subdomain.resolve("services/idle-service/index.mdx");
        write(idle, "---\nid: idle-service\nname: Idle Service\n---");
        write(catalog.resolve("node_modules/domains/fake/index.mdx"), page("fake", "Fake", "Must be ignored"));
        write(catalog.resolve("README.md"), "No frontmatter");
        write(catalog.resolve("unfinished.mdx"), "---\nid: unfinished\n");
        write(catalog.resolve("ignored.txt"), page("ignored-text", "Ignored text", "Ignored"));
        write(service.resolve("specs/asyncapi.yml"), "asyncapi: 3.0.0\n");
        write(tempDir.resolve("absolute.graphql"), "type Query { shipment: String }\n");

        Path output = tempDir.resolve("output/zenwave-architecture.yml");
        runImporter(catalog.toString(), output.toString());

        Map<String, Object> manifest = YAML.readValue(output.toFile(), MAP_TYPE);
        Map<String, Object> fulfillment = asMap(asMap(manifest.get("domains")).get("fulfillment"));
        Map<String, Object> shipping = asMap(asMap(fulfillment.get("subdomains")).get("shipping"));
        Map<String, Object> services = asMap(shipping.get("services"));
        Map<String, Object> delivery = asMap(services.get("delivery"));
        assertFalse(delivery.containsKey("docs"), "A service with no body should not emit an empty docs entry");

        List<Map<String, Object>> artifacts = asListOfMaps(delivery.get("artifacts"));
        assertTrue(artifacts.stream().anyMatch(artifact -> "asyncapi".equals(artifact.get("type"))
                && artifact.get("path").toString().endsWith("catalog/domains/fulfillment/subdomains/fulfillment.shipping/services/fulfillment.shipping.delivery/specs/asyncapi.yml")));
        assertTrue(artifacts.stream().anyMatch(artifact -> "https://example.test/openapi.yml".equals(artifact.get("path"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.get("path").toString().endsWith("absolute.graphql")));
        assertTrue(artifacts.stream().anyMatch(artifact -> "flow".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "data-product".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "diagram".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "zdl".equals(artifact.get("type"))));

        Path zdl = tempDir.resolve("output/fulfillment/shipping/delivery/domain-model.zdl");
        String zdlContent = Files.readString(zdl);
        assertTrue(zdlContent.contains("@aggregate"));
        assertTrue(zdlContent.contains("trackingId UUID required"));
        assertTrue(zdlContent.contains("amount BigDecimal"));
        assertTrue(zdlContent.contains("carrier-code CustomCode"));
        assertTrue(zdlContent.contains("entity ImportedEntity"));
        assertEquals(2, services.size());
        assertTrue(services.containsKey("idle-service"));
        assertFalse(asMap(manifest.get("domains")).containsKey("fake"));
    }

    private void runImporter(String inputFolder, String outputFile) throws Exception {
        new MainGenerator().generate(new EventCatalogImporterPlugin()
                .withOption("inputFolder", inputFolder)
                .withOption("outputFile", outputFile));
    }

    private String page(String id, String name, String body) {
        return "---\nid: " + id + "\nname: " + name + "\n---\n" + body + "\n";
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private Map<String, Object> readManifest() throws Exception {
        return YAML.readValue(new File(OUTPUT_FOLDER, "zenwave-architecture.yml"), MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        assertInstanceOf(Map.class, value);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        assertInstanceOf(List.class, value);
        return (List<Map<String, Object>>) value;
    }
}
