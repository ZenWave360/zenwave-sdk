package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventCatalogImporterTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String EVENT_CATALOG_CONTENT = "src/test/resources/event-catalog-content";
    private static final String OUTPUT_FOLDER = "target/event-catalog-import-test";

    @Test
    void importsEventCatalogHierarchyIntoZenWaveManifest() throws Exception {
        runImporter(EVENT_CATALOG_CONTENT, OUTPUT_FOLDER + "/zenwave-architecture.yml");

        Map<String, Object> manifest = readManifest();
        Map<String, Object> domains = asMap(manifest.get("domains"));
        assertTrue(domains.containsKey("merchandising"));

        Map<String, Object> merchandising = asMap(domains.get("merchandising"));
        Map<String, Object> subdomains = asMap(merchandising.get("subdomains"));
        Map<String, Object> inventory = asMap(subdomains.get("inventory"));
        Map<String, Object> services = asMap(inventory.get("services"));
        Map<String, Object> service = asMap(services.get("inventory-adjustment"));

        assertEquals("merchandising.inventory.inventory-adjustment", service.get("id"));
        assertEquals("merchandising/inventory/inventory-adjustment", service.get("path"));
        assertEquals("EVENT_CATALOG.md", asMap(service.get("docs")).get("content"));

        List<Map<String, Object>> artifacts = asListOfMaps(service.get("artifacts"));
        assertTrue(artifacts.stream().anyMatch(artifact -> "asyncapi".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "openapi".equals(artifact.get("type"))));
        assertTrue(artifacts.stream().anyMatch(artifact -> "zdl".equals(artifact.get("type"))));

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
    void marksMissingSourceSpecsAsUnresolvedArtifacts() throws Exception {
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
        assertTrue(artifacts.stream().anyMatch(artifact ->
                "asyncapi".equals(artifact.get("type"))
                        && Boolean.TRUE.equals(artifact.get("unresolved"))
                        && artifact.get("comment").toString().contains("source of truth")));
    }

    private void runImporter(String inputFolder, String outputFile) throws Exception {
        new MainGenerator().generate(new EventCatalogImporterPlugin()
                .withOption("inputFolder", inputFolder)
                .withOption("outputFile", outputFile));
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
