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

class EventCatalogFrontmatterTest {

    private static final String ARCHITECTURE_CLASSPATH = "retail-domain-catalog/zenwave-architecture.yml";
    private static final String OUTPUT_FOLDER = "target/event-catalog-frontmatter-test";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Test
    void generatesStructuredDomainAndServiceFrontmatter() throws Exception {
        runGenerator();

        Map<String, Object> domain = readFrontmatter("domains/merchandising/merchandising.inventory/index.mdx");
        assertEquals("merchandising.inventory", domain.get("id"));
        assertTrue(domain.containsKey("services"));
        assertTrue(domain.containsKey("entities"));

        List<Map<String, Object>> services = asListOfMaps(domain.get("services"));
        assertTrue(services.stream().anyMatch(service -> "merchandising.inventory.inventory-adjustment".equals(service.get("id"))));

        Map<String, Object> service = readFrontmatter("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertTrue(service.containsKey("repository"));
        assertTrue(service.containsKey("specifications"));
        assertTrue(service.containsKey("sends"));
        assertTrue(service.containsKey("receives"));

        Map<String, Object> repository = asMap(service.get("repository"));
        assertNotNull(repository.get("url"));

        List<Map<String, Object>> specifications = asListOfMaps(service.get("specifications"));
        assertTrue(specifications.stream().anyMatch(spec -> "asyncapi".equals(spec.get("type"))));
        assertTrue(specifications.stream().anyMatch(spec -> "openapi".equals(spec.get("type"))));
    }

    @Test
    void generatesEnrichedQueryEntityAndChannelFrontmatter() throws Exception {
        runGenerator();

        Map<String, Object> query = readFrontmatter("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.getInventoryItem/index.mdx");
        assertEquals("Returns a single inventory item by SKU", query.get("summary"));
        Map<String, Object> operation = asMap(query.get("operation"));
        assertEquals("GET", operation.get("method"));
        assertEquals("/inventory-items/{sku}", operation.get("path"));
        assertTrue(asStringList(operation.get("statusCodes")).contains("200"));

        Map<String, Object> entity = readFrontmatter("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/entities/merchandising.inventory.inventory-adjustment.inventory-item/index.mdx");
        assertEquals(Boolean.TRUE, entity.get("aggregateRoot"));
        List<Map<String, Object>> properties = asListOfMaps(entity.get("properties"));
        assertTrue(properties.stream().anyMatch(property -> "sku".equals(property.get("name"))));
        assertTrue(properties.stream().anyMatch(property -> "quantity".equals(property.get("name")) && Boolean.TRUE.equals(property.get("required"))));

        Map<String, Object> channel = readFrontmatter("domains/merchandising/merchandising.inventory/channels/merchandising.inventory.inventory-adjustment.inventory-adjusted/index.mdx");
        assertEquals("merchandising.inventory.inventory-adjustment.inventory-adjusted.event.avro.v0", channel.get("address"));
        assertTrue(asStringList(channel.get("protocols")).contains("kafka"));
        List<Map<String, Object>> messages = asListOfMaps(channel.get("messages"));
        assertTrue(messages.stream().anyMatch(message -> "events".equals(message.get("collection"))));
    }

    private void runGenerator() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));
    }

    private static String architectureFilePath() {
        String[] candidates = {
                ARCHITECTURE_CLASSPATH,
                "../zenwave-sdk-test-resources/src/main/resources/" + ARCHITECTURE_CLASSPATH,
                "../../zenwave-sdk-test-resources/src/main/resources/" + ARCHITECTURE_CLASSPATH,
                "zenwave-sdk-test-resources/src/main/resources/" + ARCHITECTURE_CLASSPATH,
        };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        throw new IllegalStateException("zenwave-architecture.yml not found. Tried classpath and filesystem fallbacks.");
    }

    private Map<String, Object> readFrontmatter(String relativePath) throws Exception {
        File file = new File(OUTPUT_FOLDER, relativePath);
        assertTrue(file.exists(), "Expected MDX file not found: " + file.getAbsolutePath());
        String content = Files.readString(file.toPath());
        int secondDelimiter = content.indexOf("---\n", 4);
        assertTrue(secondDelimiter > 4, "Expected closing frontmatter delimiter in " + file.getAbsolutePath());
        String yaml = content.substring(4, secondDelimiter);
        return YAML.readValue(yaml, MAP_TYPE);
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

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        assertInstanceOf(List.class, value);
        return (List<String>) value;
    }
}
