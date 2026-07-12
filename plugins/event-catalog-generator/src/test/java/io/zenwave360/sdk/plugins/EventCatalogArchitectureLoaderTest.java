package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogArchitectureLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unchecked")
    void loadsDirectDomainAndSubdomainServicesIntoNestedAndFlattenedMaps() throws Exception {
        Path manifest = writeManifest("""
                domains:
                  orders:
                    services:
                      orders-api:
                  fulfillment:
                    subdomains:
                      shipping:
                        services:
                          shipping-api:
                """);

        Map<String, Object> architecture = loadArchitecture(manifest);

        Map<String, Object> domains = (Map<String, Object>) architecture.get("domains");
        Map<String, Object> orders = (Map<String, Object>) domains.get("orders");
        Map<String, Object> directServices = (Map<String, Object>) orders.get("services");
        assertNotNull(directServices.get("orders-api"));

        Map<String, Object> fulfillment = (Map<String, Object>) domains.get("fulfillment");
        Map<String, Object> subdomains = (Map<String, Object>) fulfillment.get("subdomains");
        Map<String, Object> shipping = (Map<String, Object>) subdomains.get("shipping");
        Map<String, Object> nestedServices = (Map<String, Object>) shipping.get("services");
        assertNotNull(nestedServices.get("shipping-api"));

        Map<String, Object> flattenedServices = (Map<String, Object>) architecture.get("services");
        assertTrue(flattenedServices.containsKey("orders.orders-api"));
        assertTrue(flattenedServices.containsKey("fulfillment.shipping.shipping-api"));

        Map<String, Object> ordersService = (Map<String, Object>) flattenedServices.get("orders.orders-api");
        assertEquals("orders/orders-api", ordersService.get("serviceRef"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolvesPathDocsArtifactsAndNormalizesConsumers() throws Exception {
        Path repos = tempDir.resolve("repos");
        Path ordersApi = repos.resolve("orders-api");
        Path paymentsApi = repos.resolve("payments-api");
        Path notificationsApi = repos.resolve("notifications-api");
        Files.createDirectories(ordersApi);
        Files.createDirectories(paymentsApi);
        Files.createDirectories(notificationsApi);
        Files.writeString(ordersApi.resolve("SUMMARY.md"), "# Orders API");
        Files.writeString(ordersApi.resolve("domain-model.zdl"), "entity Order");
        Files.writeString(ordersApi.resolve("asyncapi.yml"), "asyncapi: 3.0.0");

        Path manifest = writeManifest("""
                config:
                  contentResolution:
                    - workspace
                    - git
                  sources:
                    git:
                      provider: generic
                      server: https://raw.githubusercontent.com/acme/catalog/develop
                      contentUrlExpression: "${server}/${content.path}"
                domains:
                  orders:
                    id: orders
                    services:
                      orders-api:
                        id: orders.orders-api
                        docs:
                          summary: SUMMARY.md
                        artifacts:
                          - type: zdl
                            path: domain-model.zdl
                          - type: asyncapi
                            path: asyncapi.yml
                        consumers:
                          - service: notifications-api
                          - "#/domains/payments/services/payments-api"
                  payments:
                    id: payments
                    services:
                      payments-api:
                        id: payments.payments-api
                  notifications:
                    id: notifications
                    services:
                      notifications-api:
                        id: notifications.notifications-api
                """);

        Map<String, Object> architecture = loadArchitecture(manifest);
        Map<String, Object> services = (Map<String, Object>) architecture.get("services");
        Map<String, Object> ordersService = (Map<String, Object>) services.get("orders.orders-api");

        assertEquals("orders/orders-api", ordersService.get("serviceRef"));

        Map<String, Object> docs = (Map<String, Object>) ordersService.get("docs");
        assertEquals("SUMMARY.md", docs.get("summary"));

        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) ordersService.get("artifacts");
        assertEquals("domain-model.zdl", artifacts.get(0).get("path"));
        assertEquals("asyncapi.yml", artifacts.get(1).get("path"));

        List<String> consumers = (List<String>) ordersService.get("consumers");
        assertEquals(List.of("orders.notifications-api", "payments.payments-api"), consumers);
    }

    private Path writeManifest(String content) throws Exception {
        Path manifest = tempDir.resolve("zenwave-architecture.yml");
        Files.writeString(manifest, content);
        return manifest;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadArchitecture(Path manifest) {
        EventCatalogArchitectureLoader loader = new EventCatalogArchitectureLoader();
        loader.inputFile = manifest.toString();
        Map<String, Object> context = loader.process(new java.util.LinkedHashMap<>());
        return (Map<String, Object>) context.get("architecture");
    }
}
