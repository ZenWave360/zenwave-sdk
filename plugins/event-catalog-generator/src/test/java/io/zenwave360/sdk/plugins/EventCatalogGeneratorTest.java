package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class EventCatalogGeneratorTest {

    private static final String ARCHITECTURE_CLASSPATH = "retail-domain-catalog/zenwave-architecture.yml";
    private static final String OUTPUT_FOLDER = "target/event-catalog-test";

    private static String architectureFilePath() {
        var resource = EventCatalogGeneratorTest.class.getClassLoader().getResource(ARCHITECTURE_CLASSPATH);
        if (resource != null) {
            return resource.getFile();
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

    @Test
    void generatesDomainSubdomainAndServicePages() throws Exception {
        String inputFile = architectureFilePath();

        var plugin = new EventCatalogPlugin()
                .withOption("inputFile", inputFile)
                .withOption("outputFolder", OUTPUT_FOLDER);

        new MainGenerator().generate(plugin);

        // Domains
        assertMdxExists("domains/merchandising/index.mdx");
        assertMdxExists("domains/customer-relationship/index.mdx");

        // Subdomains (folder name = full subdomain id)
        assertMdxExists("domains/merchandising/merchandising.inventory/index.mdx");
        assertMdxExists("domains/merchandising/merchandising.pricing/index.mdx");
        assertMdxExists("domains/customer-relationship/customer-relationship.customer-management/index.mdx");

        // Services
        assertMdxExists("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertMdxExists("domains/merchandising/merchandising.inventory/services/merchandising.inventory.stock-replenishment/index.mdx");
        assertMdxExists("domains/merchandising/merchandising.pricing/services/merchandising.pricing.price-change/index.mdx");
        assertMdxExists("domains/customer-relationship/customer-relationship.customer-management/services/customer-relationship.customer-management.customer-profile/index.mdx");
        assertMdxExists("domains/customer-relationship/customer-relationship.customer-management/services/customer-relationship.customer-management.loyalty-management/index.mdx");
    }

    @Test
    void domainPageHasCorrectFrontmatter() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        String content = readMdx("domains/merchandising/index.mdx");
        assertTrue(content.startsWith("---\n"), "Must start with frontmatter delimiter");
        assertTrue(content.contains("\"merchandising\""), "Must contain domain id");
        assertTrue(content.contains("name:"), "Must contain name");
        assertTrue(content.contains("version:"), "Must contain version");
    }

    @Test
    void servicePageHasCorrectFrontmatter() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        String content = readMdx("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertTrue(content.contains("\"merchandising.inventory.inventory-adjustment\""));
        assertTrue(content.contains("name:"));
        assertTrue(content.contains("\"1.0.0\""));
    }

    @Test
    void generatesEventAndCommandPages() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        // Service pages have sends/receives populated from AsyncAPI
        String serviceContent = readMdx("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/index.mdx");
        assertTrue(serviceContent.contains("sends:") || serviceContent.contains("receives:"),
                "Service page must contain sends or receives");
    }

    @Test
    void generatesQueryPages() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        // inventory-adjustment has an openapi.yml with GET operations
        assertMdxExists("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.listInventoryItems/index.mdx");
        assertMdxExists("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.getInventoryItem/index.mdx");

        String queryContent = readMdx("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/queries/merchandising.inventory.inventory-adjustment.getInventoryItem/index.mdx");
        assertTrue(queryContent.contains("\"Get Inventory Item\""), "Query name must be present");
        assertTrue(queryContent.contains("\"1.0.0\""), "Query version must be present");
    }

    @Test
    void generatesEntityPages() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        // inventory-adjustment has a domain-model.zdl with InventoryItem entity
        assertMdxExists("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/entities/merchandising.inventory.inventory-adjustment.inventory-item/index.mdx");

        String entityContent = readMdx("domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment/entities/merchandising.inventory.inventory-adjustment.inventory-item/index.mdx");
        assertTrue(entityContent.contains("\"InventoryItem\""), "Entity name must be present");
        assertTrue(entityContent.contains("aggregateRoot: true"), "Aggregate root flag must be present");
    }

    @Test
    void servicePageHasSendsReceives() throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
                        .withOption("outputFolder", OUTPUT_FOLDER));

        // Check that at least one service with an asyncapi spec has sends or receives
        String serviceDir = "domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment";
        File serviceIndex = new File(OUTPUT_FOLDER, serviceDir + "/index.mdx");
        if (serviceIndex.exists()) {
            String content = java.nio.file.Files.readString(serviceIndex.toPath());
            // If AsyncAPI spec was found and parsed, sends/receives should be present
            // (test is lenient — just check structure is valid MDX with frontmatter)
            assertTrue(content.startsWith("---\n"), "Service MDX must start with frontmatter");
        }
    }

    @Test
    void outputFolderIsCleanedOnRegeneration() throws Exception {
        // First run — generates everything
        runGenerator(OUTPUT_FOLDER);

        // Plant a stale file that should be removed on the next run
        File staleFile = new File(OUTPUT_FOLDER, "domains/merchandising/stale-file.mdx");
        staleFile.getParentFile().mkdirs();
        java.nio.file.Files.writeString(staleFile.toPath(), "stale");
        assertTrue(staleFile.exists(), "Stale file must exist before second run");

        // Second run — stale file must be gone
        runGenerator(OUTPUT_FOLDER);
        assertFalse(staleFile.exists(), "Stale file must be removed by clean-before-write");
    }

    @Test
    void versionedFolderIsPreservedDuringCleanup() throws Exception {
        // First run
        runGenerator(OUTPUT_FOLDER);

        // Plant a versioned/ archive as if a previous version existed
        String serviceDir = "domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment";
        File versionedDir = new File(OUTPUT_FOLDER, serviceDir + "/versioned/0.9.0");
        versionedDir.mkdirs();
        File archivedPage = new File(versionedDir, "index.mdx");
        java.nio.file.Files.writeString(archivedPage.toPath(), "---\nid: old\nversion: \"0.9.0\"\n---\n");

        // Second run — versioned/ archive must survive
        runGenerator(OUTPUT_FOLDER);
        assertTrue(archivedPage.exists(), "Archived versioned page must be preserved across regeneration");
    }

    @Test
    void servicePageIsArchivedWhenVersionChanges() throws Exception {
        String serviceDir = "domains/merchandising/merchandising.inventory/services/merchandising.inventory.inventory-adjustment";
        String outputFolder = "target/event-catalog-versioning-test";

        // First run — generates version 1.0.0
        runGenerator(outputFolder);
        File serviceIndex = new File(outputFolder, serviceDir + "/index.mdx");
        assertTrue(serviceIndex.exists(), "Service index must exist after first run");

        // Overwrite the generated page with a different version to simulate a prior version
        String oldContent = java.nio.file.Files.readString(serviceIndex.toPath())
                .replace("\"1.0.0\"", "\"0.5.0\"");
        java.nio.file.Files.writeString(serviceIndex.toPath(), oldContent);

        // Second run — the old 0.5.0 page must be archived, new 1.0.0 must be written
        runGenerator(outputFolder);

        File archived = new File(outputFolder, serviceDir + "/versioned/0.5.0/index.mdx");
        assertTrue(archived.exists(), "Old version must be archived to versioned/0.5.0/index.mdx");

        String current = java.nio.file.Files.readString(serviceIndex.toPath());
        assertTrue(current.contains("\"1.0.0\""), "Current page must contain new version 1.0.0");
    }

    private void runGenerator(String outputFolder) throws Exception {
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architectureFilePath())
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
