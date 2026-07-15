package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogAdvancedFrontmatterTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @TempDir
    Path tempDir;

    @Test
    void mapsAdvancedFrontmatterAndFallbackDocsAtTheGeneratorBoundary() throws Exception {
        Files.writeString(tempDir.resolve("SERVICE.md"), "## Operational notes\n\nKeep orders moving.");
        Files.writeString(tempDir.resolve("schema.yml"), "type: object\n");

        Map<String, Object> domain = map(
                "id", "commerce",
                "name", "Commerce",
                "version", "9.0.0",
                "summary", "Commercial capabilities",
                "draft", true,
                "deprecated", true,
                "hidden", true,
                "visualiser", false,
                "badges", List.of(map("content", "beta", "backgroundColor", "orange", "textColor", "black", "icon", "flag", "url", "https://example.test/beta")),
                "owners", List.of("commerce-team", map("id", "architecture-team"), map("name", "ignored")),
                "attachments", List.of("https://example.test/runbook", map("url", "https://example.test/adr", "title", "ADR", "type", "text/markdown", "description", "Decision", "icon", "book")),
                "diagrams", List.of(map("id", "commerce-context", "version", "1.0.0"), map("id", "commerce-context"), map("name", "ignored")),
                "versions", List.of("8.0.0", map("id", "9.0.0"), 7),
                "latestVersion", "9.0.0",
                "agents", List.of("commerce-agent", map("id", "pricing-agent"), map("name", "ignored")),
                "data-products", List.of(map("id", "sales-insights"), map("id", "sales-insights")),
                "flows", List.of(map("id", "checkout-flow"), map("name", "ignored")));

        Map<String, Object> subdomain = map(
                "id", "commerce.orders",
                "name", "Orders",
                "version", "9.1.0",
                "draft", map("title", "Preview", "message", "Subject to change"),
                "deprecated", map("message", "Use fulfillment", "date", "2027-01-01"));
        domain.put("subdomains", map("orders", subdomain));

        Map<String, Object> event = map(
                "id", "commerce.orders.billing.invoice-issued",
                "name", "Invoice Issued",
                "version", "1.2.0",
                "summary", "Invoice issued",
                "schemaPath", "schemas/invoice&\"quoted\".json",
                "channelId", "commerce.orders.billing.invoices");
        Map<String, Object> fallbackEvent = map(
                "id", "commerce.orders.billing.fallback-event",
                "name", "Fallback Event",
                "version", "1.2.0");
        Map<String, Object> command = map(
                "id", "commerce.orders.billing.issue-invoice",
                "name", "Issue Invoice",
                "version", "1.2.0",
                "channelId", "commerce.orders.billing.invoices");
        Map<String, Object> query = map(
                "id", "commerce.orders.billing.findInvoice",
                "name", "Find Invoice",
                "version", "1.2.0",
                "operation", map("method", "GET", "path", "/invoices/{id}", "statusCodes", "200"));
        Map<String, Object> entity = map(
                "id", "commerce.orders.billing.invoice",
                "name", "Invoice",
                "version", "1.2.0",
                "aggregateRoot", false,
                "properties", List.of(
                        map("name", "lineItems", "type", "array", "required", true, "description", "Invoice lines", "references", "LineItem", "referencesIdentifier", "id", "relationType", "oneToMany", "items", map("type", "LineItem")),
                        map("name", "state", "type", "string", "enum", List.of("OPEN", map("id", "PAID"), 3))));

        Map<String, Object> billing = map(
                "id", "commerce.orders.billing",
                "name", "Billing",
                "domain", "commerce",
                "subdomain", "orders",
                "version", "1.2.0",
                "repository", tempDir.toString(),
                "docs", map("content", "SERVICE.md", "missing", "MISSING.md"),
                "artifacts", List.of(
                        map("type", "zdl", "path", "domain-model.zdl"),
                        map("type", "asyncapi", "path", "asyncapi.yml", "headers", map("X-API-Key", "secret", "ignored", null)),
                        map("type", "openapi"),
                        map("type", "graphql", "buildPath", tempDir.resolve("schema.yml").toString())),
                "_sends", List.of(event.get("id"), map("id", fallbackEvent.get("id")), 17),
                "_receives", command.get("id"),
                "_events", List.of(map("name", "Ignored missing id"), event, fallbackEvent),
                "_commands", List.of(map("name", "Ignored missing id"), command),
                "_queries", List.of(map("name", "Ignored missing id"), query),
                "_entities", List.of(map("name", "Ignored missing id"), entity),
                "_channels", List.of(
                        map("name", "Ignored missing id"),
                        map("id", "commerce.orders.billing.empty", "name", "Empty Channel", "version", "1.2.0"),
                        map("id", "commerce.orders.billing.invoices", "name", "Invoices", "version", "1.2.0", "summary", "Invoice traffic", "address", "commerce.invoices", "protocols", "kafka", "deliveryGuarantee", "at-least-once")));

        Map<String, Object> observer = map(
                "id", "commerce.orders.observer",
                "name", "Observer",
                "domain", "commerce",
                "subdomain", "orders",
                "_sends", List.of(event.get("id")),
                "_receives", List.of(command.get("id")));
        Map<String, Object> direct = map(
                "id", "commerce.support",
                "name", "Support",
                "domain", "commerce",
                "version", "1.0.0",
                "repository", "https://example.test/support");
        Map<String, Object> unknownDomain = map(
                "id", "external.raw-service",
                "name", "Raw Service",
                "domain", "external",
                "subdomain", "raw",
                "version", "1.0.0");

        Map<String, Object> architecture = map(
                "config", Map.of(),
                "domains", map("commerce-key", domain),
                "services", map(
                        "billing", billing,
                        "observer", observer,
                        "support", direct,
                        "external", unknownDomain));

        EventCatalogGenerator generator = new EventCatalogGenerator();
        Map<String, TemplateOutput> outputs = generator.generate(map("architecture", architecture)).singleFiles.stream()
                .collect(Collectors.toMap(TemplateOutput::getTargetFile, Function.identity()));

        String domainPage = content(outputs, "domains/commerce/index.mdx");
        Map<String, Object> domainFrontmatter = frontmatter(domainPage);
        assertTrue(domainFrontmatter.containsKey("draft"));
        assertTrue(domainFrontmatter.containsKey("deprecated"));
        assertEquals(2, maps(domainFrontmatter.get("owners")).size());
        assertEquals(2, maps(domainFrontmatter.get("attachments")).size());
        assertEquals(1, maps(domainFrontmatter.get("diagrams")).size());
        assertEquals(List.of("8.0.0", "9.0.0"), strings(domainFrontmatter.get("versions")));
        assertEquals(2, maps(domainFrontmatter.get("agents")).size());

        Map<String, Object> subdomainFrontmatter = frontmatter(content(outputs,
                "domains/commerce/subdomains/commerce.orders/index.mdx"));
        assertEquals("Preview", mapValue(subdomainFrontmatter.get("draft")).get("title"));
        assertEquals("Use fulfillment", mapValue(subdomainFrontmatter.get("deprecated")).get("message"));

        String servicePage = content(outputs,
                "domains/commerce/subdomains/commerce.orders/services/commerce.orders.billing/index.mdx");
        assertTrue(servicePage.contains("Keep orders moving."));
        Map<String, Object> serviceFrontmatter = frontmatter(servicePage);
        assertEquals(2, maps(serviceFrontmatter.get("specifications")).size());
        assertEquals("secret", mapValue(maps(serviceFrontmatter.get("specifications")).get(0).get("headers")).get("X-API-Key"));

        assertTrue(outputs.containsKey("domains/commerce/services/commerce.support/index.mdx"));
        assertTrue(outputs.containsKey("domains/external/subdomains/raw/services/external.raw-service/index.mdx"));
        assertFalse(outputs.keySet().stream().anyMatch(path -> path.contains("Ignored missing id")));

        String eventPage = content(outputs,
                "domains/commerce/subdomains/commerce.orders/services/commerce.orders.billing/events/commerce.orders.billing.invoice-issued/index.mdx");
        assertTrue(eventPage.contains("schemas/invoice&amp;&quot;quoted&quot;.json"));
        String fallbackEventPage = content(outputs,
                "domains/commerce/subdomains/commerce.orders/services/commerce.orders.billing/events/commerce.orders.billing.fallback-event/index.mdx");
        assertTrue(fallbackEventPage.contains("Generated event reference page."));

        String entityPage = content(outputs,
                "domains/commerce/subdomains/commerce.orders/services/commerce.orders.billing/entities/commerce.orders.billing.invoice/index.mdx");
        Map<String, Object> entityFrontmatter = frontmatter(entityPage);
        Map<String, Object> lineItems = maps(entityFrontmatter.get("properties")).get(0);
        assertEquals("LineItem", mapValue(lineItems.get("items")).get("type"));
        assertTrue(entityPage.contains("## Relationships"));
        assertEquals(List.of("commerce.orders-0.0.1"), strings(entityFrontmatter.get("domains")));

        String emptyChannel = content(outputs, "domains/commerce/subdomains/commerce.orders/channels/commerce.orders.billing.empty/index.mdx");
        assertFalse(emptyChannel.contains("## Messages"));
        String invoiceChannel = content(outputs, "domains/commerce/subdomains/commerce.orders/channels/commerce.orders.billing.invoices/index.mdx");
        assertTrue(invoiceChannel.contains("## Messages"));

        Map<String, Object> queryFrontmatter = frontmatter(content(outputs,
                "domains/commerce/subdomains/commerce.orders/services/commerce.orders.billing/queries/commerce.orders.billing.findInvoice/index.mdx"));
        assertEquals(List.of("200"), strings(mapValue(queryFrontmatter.get("operation")).get("statusCodes")));
        assertNull(frontmatter(content(outputs, "domains/commerce/services/commerce.support/index.mdx")).get("specifications"));
    }

    private String content(Map<String, TemplateOutput> outputs, String path) {
        TemplateOutput output = outputs.get(path);
        assertTrue(output != null, "Missing generated output " + path);
        return output.getContent();
    }

    private Map<String, Object> frontmatter(String content) throws Exception {
        int end = content.indexOf("---\n", 4);
        assertTrue(end > 4);
        return YAML.readValue(content.substring(4, end), MAP_TYPE);
    }

    private Map<String, Object> map(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(entries[i].toString(), entries[i + 1]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object value) {
        return (List<String>) value;
    }
}
