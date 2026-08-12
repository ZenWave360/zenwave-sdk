package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.templating.TemplateOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogFileWriterTest {

    @TempDir
    Path outputFolder;

    @Test
    void archivesChangedVersionsForAllEventCatalogResourceCollections() throws Exception {
        List<String> resources = List.of(
                "domains/sales/index.mdx",
                "domains/sales/subdomains/sales.orders/index.mdx",
                "domains/sales/subdomains/sales.orders/services/order-service/index.mdx",
                "domains/sales/subdomains/sales.orders/channels/orders/index.mdx",
                "domains/sales/subdomains/sales.orders/services/order-service/events/order-created/index.mdx",
                "domains/sales/subdomains/sales.orders/services/order-service/commands/create-order/index.mdx",
                "domains/sales/subdomains/sales.orders/services/order-service/queries/get-order/index.mdx",
                "domains/sales/subdomains/sales.orders/services/order-service/entities/order/index.mdx",
                "systems/order-system/index.mdx",
                "agents/order-agent/index.mdx",
                "adrs/version-all-resources/index.mdx",
                "flows/order-flow/index.mdx",
                "containers/orders-db/index.mdx",
                "data-products/order-metrics/index.mdx",
                "diagrams/order-landscape/index.mdx");

        for (String resource : resources) {
            write(resource, page("1.0.0", "old " + resource));
        }

        String event = "domains/sales/subdomains/sales.orders/services/order-service/events/order-created/index.mdx";
        Path eventDir = outputFolder.resolve(event).getParent();
        write(eventDir.resolve("schema.json"), "{\"title\":\"OrderCreated v1\"}");
        write(eventDir.resolve("docs/details.mdx"), "Historical event details");
        write(event, page("1.0.0", "https://schemas.example.test/order-created selector=OrderCreated"));

        List<TemplateOutput> generated = new ArrayList<>();
        for (String resource : resources) {
            generated.add(new TemplateOutput(resource, page("2.0.0", "new " + resource)));
        }
        writer().write(generated);

        for (String resource : resources) {
            Path current = outputFolder.resolve(resource);
            Path archive = current.getParent().resolve("versioned/1.0.0/index.mdx");
            assertTrue(Files.exists(archive), "Missing archive for " + resource);
            assertTrue(Files.readString(archive).contains("version: \"1.0.0\""));
            assertTrue(Files.readString(current).contains("version: \"2.0.0\""));
        }

        Path archivedEvent = eventDir.resolve("versioned/1.0.0");
        assertEquals("{\"title\":\"OrderCreated v1\"}", Files.readString(archivedEvent.resolve("schema.json")));
        assertEquals("Historical event details", Files.readString(archivedEvent.resolve("docs/details.mdx")));
        assertTrue(Files.readString(archivedEvent.resolve("index.mdx"))
                .contains("https://schemas.example.test/order-created selector=OrderCreated"));
        assertFalse(Files.exists(eventDir.resolve("schema.json")), "Current local assets are replaced with generated output");

        assertFalse(Files.exists(outputFolder.resolve("domains/sales/versioned/1.0.0/subdomains")),
                "A domain archive must not absorb nested resources");
        assertFalse(Files.exists(outputFolder.resolve(
                        "domains/sales/subdomains/sales.orders/services/order-service/versioned/1.0.0/events")),
                "A service archive must not absorb nested resources");
    }

    @Test
    void archivesResourcesThatAreRemovedFromGeneratedOutput() throws Exception {
        String resource = "services/order-service/events/order-cancelled/index.md";
        Path resourceDir = outputFolder.resolve(resource).getParent();
        write(resource, page("3.1.0", "removed event"));
        write(resourceDir.resolve("schema.avsc"), "removed schema");

        writer().write(List.of());

        assertFalse(Files.exists(outputFolder.resolve(resource)));
        assertEquals("removed event", body(resourceDir.resolve("versioned/3.1.0/index.md")));
        assertEquals("removed schema", Files.readString(resourceDir.resolve("versioned/3.1.0/schema.avsc")));
    }

    @Test
    void overwritesAnExistingArchiveForTheSameVersion() throws Exception {
        String resource = "services/order-service/commands/place-order/index.mdx";
        Path resourceDir = outputFolder.resolve(resource).getParent();
        write(resource, page("1.0.0", "authoritative old command"));
        write(resourceDir.resolve("versioned/1.0.0/index.mdx"), page("1.0.0", "stale archive"));
        write(resourceDir.resolve("versioned/1.0.0/stale.txt"), "must be removed");

        writer().write(List.of(new TemplateOutput(resource, page("2.0.0", "current command"))));

        assertEquals("authoritative old command", body(resourceDir.resolve("versioned/1.0.0/index.mdx")));
        assertFalse(Files.exists(resourceDir.resolve("versioned/1.0.0/stale.txt")));
        assertEquals("current command", body(outputFolder.resolve(resource)));
    }

    @Test
    void doesNotCreateAnArchiveWhenTheVersionIsUnchanged() throws Exception {
        String resource = "services/order-service/queries/get-order/index.mdx";
        write(resource, page("1.0.0", "old generated content"));

        writer().write(List.of(new TemplateOutput(resource, page("1.0.0", "new generated content"))));

        assertFalse(Files.exists(outputFolder.resolve(
                "services/order-service/queries/get-order/versioned")));
        assertEquals("new generated content", body(outputFolder.resolve(resource)));
    }

    private EventCatalogFileWriter writer() {
        EventCatalogFileWriter writer = new EventCatalogFileWriter();
        writer.setTargetFolder(outputFolder.toFile());
        return writer;
    }

    private void write(String relativePath, String content) throws IOException {
        write(outputFolder.resolve(relativePath), content);
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private String page(String version, String body) {
        return "---\nid: resource\nversion: \"" + version + "\"\n---\n" + body;
    }

    private String body(Path page) throws IOException {
        String content = Files.readString(page);
        return content.substring(content.indexOf("\n---\n") + 5);
    }
}
