package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestDomain;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ManifestSubdomain;
import io.zenwave360.manifest.ZenWaveManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventCatalogArchitectureLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsDirectDomainAndSubdomainServicesAsTypedManifestModels() throws Exception {
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

        Map<String, Object> context = loadContext(manifest);
        ZenWaveManifest loaded = (ZenWaveManifest) context.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) context.get("eventCatalog");

        ManifestService orders = loaded.findService("orders/orders-api");
        ManifestService shipping = loaded.findService("fulfillment/shipping/shipping-api");
        assertNotNull(orders);
        assertNotNull(shipping);
        assertEquals("orders.orders-api", eventCatalog.catalogServiceId(orders));
        assertEquals("fulfillment.shipping.shipping-api", eventCatalog.catalogServiceId(shipping));

        ManifestDomain fulfillment = loaded.getDomains().stream()
                .filter(domain -> "fulfillment".equals(domain.getKey()))
                .findFirst()
                .orElseThrow();
        ManifestSubdomain shippingSubdomain = fulfillment.getSubdomains().get(0);
        assertEquals("fulfillment.shipping", eventCatalog.catalogSubdomainId(fulfillment, shippingSubdomain));
    }

    @Test
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

        ZenWaveManifest loaded = (ZenWaveManifest) loadContext(manifest).get("manifest");
        ManifestService ordersService = loaded.findService("orders/orders-api");
        assertNotNull(ordersService);

        assertEquals("orders/orders-api", ordersService.getServiceRef());
        assertEquals("SUMMARY.md", ordersService.getDocs().get("summary"));
        assertEquals("domain-model.zdl", ordersService.getArtifacts().get(0).getPath());
        assertEquals("asyncapi.yml", ordersService.getArtifacts().get(1).getPath());
        assertEquals(List.of("orders/notifications-api", "payments/payments-api"), ordersService.getConsumers());
    }

    private Path writeManifest(String content) throws Exception {
        Path manifest = tempDir.resolve("zenwave-architecture.yml");
        Files.writeString(manifest, content);
        return manifest;
    }

    private Map<String, Object> loadContext(Path manifest) {
        EventCatalogArchitectureLoader loader = new EventCatalogArchitectureLoader();
        loader.inputFile = manifest.toUri();
        return loader.process(new java.util.LinkedHashMap<>());
    }
}
