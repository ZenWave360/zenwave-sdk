package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EventCatalogGeneratorTest {

    private static final String ARCHITECTURE_CLASSPATH = "retail-domain-catalog/zenwave-architecture.yml";
    private static final String OUTPUT_FOLDER = "target/event-catalog-output-test";

    private static String architectureFilePath() {
        var resource = EventCatalogGeneratorTest.class.getClassLoader().getResource(ARCHITECTURE_CLASSPATH);
        if (resource != null && "file".equalsIgnoreCase(resource.getProtocol())) {
            return new File(resource.getFile()).getAbsolutePath();
        }
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

    @BeforeAll
    static void generateCatalogOnce() throws Exception {
        runGenerator(OUTPUT_FOLDER);
    }

    @Test
    void generatesDomainSubdomainAndServicePages() {
        // Domains
        assertMdxExists("domains/merchandising/index.mdx");
        assertMdxExists("domains/customer-relationship/index.mdx");

        // Subdomains (folder name = full subdomain id)
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/index.mdx");
        assertMdxExists("domains/merchandising/subdomains/merchandising.pricing/index.mdx");
        assertMdxExists("domains/customer-relationship/subdomains/customer-relationship.customer-management/index.mdx");

        // Services
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.stock-replenishment/index.mdx");
        assertMdxExists("domains/merchandising/subdomains/merchandising.pricing/services/merchandising.pricing.price-change/index.mdx");
        assertMdxExists("domains/customer-relationship/subdomains/customer-relationship.customer-management/services/customer-relationship.customer-management.customer-profile/index.mdx");
        assertMdxExists("domains/customer-relationship/subdomains/customer-relationship.customer-management/services/customer-relationship.customer-management.loyalty-management/index.mdx");
    }

    @Test
    void domainPageHasCorrectFrontmatter() throws Exception {
        String content = readMdx("domains/merchandising/index.mdx");
        assertTrue(content.startsWith("---\n"), "Must start with frontmatter delimiter");
        assertTrue(content.contains("\"merchandising\""), "Must contain domain id");
        assertTrue(content.contains("name:"), "Must contain name");
        assertTrue(content.contains("version:"), "Must contain version");
    }

    @Test
    void servicePageHasCorrectFrontmatter() throws Exception {
        String content = readMdx("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertTrue(content.contains("\"merchandising.inventory.inventory-adjustment\""));
        assertTrue(content.contains("name:"));
        assertTrue(content.contains("\"1.0.0\""));
    }

    @Test
    void generatesEventAndCommandPages() throws Exception {
        // Service pages have sends/receives populated from AsyncAPI
        String serviceContent = readMdx("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertTrue(serviceContent.contains("sends:") || serviceContent.contains("receives:"),
                "Service page must contain sends or receives");
        assertTrue(serviceContent.contains("<NodeGraph />"), "Service body must include the architecture visualiser");
        assertTrue(serviceContent.contains("<MessageTable"), "Service body must include the message table");
    }

    @Test
    void generatesQueryPages() throws Exception {
        // inventory-adjustment has an openapi.yml with GET operations
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.listInventoryItems/index.mdx");
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.getInventoryItem/index.mdx");

        String queryContent = readMdx("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.getInventoryItem/index.mdx");
        assertTrue(queryContent.contains("\"Get Inventory Item\""), "Query name must be present");
        assertTrue(queryContent.contains("\"1.0.0\""), "Query version must be present");
        assertTrue(queryContent.contains("<NodeGraph />"), "Query body must include the node graph");
    }

    @Test
    void generatesEntityPages() throws Exception {
        // inventory-adjustment has a domain-model.zdl with InventoryItem entity
        assertMdxExists("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/entities/merchandising.inventory.inventory-adjustment.inventory-item/index.mdx");

        String entityContent = readMdx("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/entities/merchandising.inventory.inventory-adjustment.inventory-item/index.mdx");
        assertTrue(entityContent.contains("\"InventoryItem\""), "Entity name must be present");
        assertTrue(entityContent.contains("aggregateRoot: true"), "Aggregate root flag must be present");
        assertTrue(entityContent.contains("<EntityPropertiesTable />"), "Entity body must include the properties table");
    }

    @Test
    void generatedPagesContainEventCatalogBodyComponents() throws Exception {
        String domainContent = readMdx("domains/merchandising/index.mdx");
        assertTrue(domainContent.contains("<NodeGraph />"), "Domain body must include the node graph");
        assertTrue(domainContent.contains("<MessageTable"), "Domain body must include the message table");

        String eventContent = readMdx("domains/merchandising/subdomains/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/events/merchandising.inventory.inventory-adjustment.inventory-adjusted/index.mdx");
        assertTrue(eventContent.contains("<RemoteSpecificationSchema"),
                "A remote AsyncAPI event body must include the remote schema viewer");
        assertTrue(eventContent.contains("channel=\"inventory-adjusted\""),
                "A remote AsyncAPI event must identify its channel");
        assertTrue(eventContent.contains("channelMessage=\"InventoryAdjustedEvent\""),
                "A remote AsyncAPI event must select the message from its channel");
        assertFalse(eventContent.contains("message=\"InventoryAdjustedEvent\""),
                "A remote AsyncAPI event must not use the ambiguous message selector");
        assertFalse(eventContent.contains("<SchemaViewer"),
                "A remote AsyncAPI event must have exactly one schema viewer usage");

        String channelContent = readMdx("domains/merchandising/subdomains/merchandising.inventory/channels/merchandising.inventory.inventory-adjustment.inventory-adjusted/index.mdx");
        assertTrue(channelContent.contains("<NodeGraph />"), "Channel body must include the node graph");
    }

    @Test
    void servicePageBodyCanBeOverriddenWithTheStandardTemplateFolder() throws Exception {
        Path override = Path.of(
                ".zenwave/templates/io/zenwave360/sdk/plugins/EventCatalogGenerator/service.mdx.hbs");
        byte[] previousContent = Files.exists(override) ? Files.readAllBytes(override) : null;
        String outputFolder = "target/event-catalog-template-override-test";
        try {
            Files.createDirectories(override.getParent());
            Files.writeString(override, "CUSTOM SERVICE TEMPLATE: {{service.id}}\n");

            runGenerator(outputFolder);

            Path servicePage = Path.of(
                    outputFolder,
                    "domains/merchandising/subdomains/merchandising.inventory/services/"
                            + "merchandising.inventory.inventory-adjustment/index.mdx");
            String content = Files.readString(servicePage);
            assertTrue(content.contains(
                    "CUSTOM SERVICE TEMPLATE: merchandising.inventory.inventory-adjustment"));
            assertFalse(content.contains("<NodeGraph />"));
        } finally {
            if (previousContent != null) {
                Files.write(override, previousContent);
            } else {
                Files.deleteIfExists(override);
            }
        }
    }

    private static void runGenerator(String outputFolder) throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("linkSource", "git")
                        .withOption("outputFolder", outputFolder));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertMdxExists(String relativePath) {
        File file = new File(OUTPUT_FOLDER, relativePath);
        assertTrue(file.exists(), "Expected MDX file not found: " + file.getAbsolutePath());
    }

    private String readMdx(String relativePath) throws Exception {
        File file = new File(OUTPUT_FOLDER, relativePath);
        assertTrue(file.exists(), "MDX file not found: " + file.getAbsolutePath());
        return java.nio.file.Files.readString(file.toPath());
    }
}
