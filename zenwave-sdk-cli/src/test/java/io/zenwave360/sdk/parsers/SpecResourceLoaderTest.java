package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpecResourceLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesClasspathResources() throws IOException {
        Model model = new SpecResourceLoader().parse(
                "classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");

        Assertions.assertNotNull(JSONPath.get(model, "$.channels.createProductNotification.subscribe.message"));
    }

    @Test
    void parsesLocalResourcesWithRelativeRefs() throws IOException {
        Files.writeString(tempDir.resolve("schemas.yml"), """
                Customer:
                  type: object
                  properties:
                    name:
                      type: string
                """, StandardCharsets.UTF_8);
        Path apiFile = tempDir.resolve("api.yml");
        Files.writeString(apiFile, """
                openapi: 3.0.0
                info:
                  title: Local API
                  version: 1.0.0
                paths: {}
                components:
                  schemas:
                    Customer:
                      $ref: './schemas.yml#/Customer'
                """, StandardCharsets.UTF_8);

        Model model = new SpecResourceLoader().parse(apiFile.toUri());

        Assertions.assertEquals("string", JSONPath.get(model, "$.components.schemas.Customer.properties.name.type"));
    }

    @Test
    void parsesHttpResourcesWithAuthenticationAndRelativeRefs() throws IOException {
        AtomicInteger authenticatedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api.yml", exchange -> respond(exchange, authenticatedRequests, """
                openapi: 3.0.0
                info:
                  title: Remote API
                  version: 1.0.0
                paths: {}
                components:
                  schemas:
                    Customer:
                      $ref: './schemas.yml#/Customer'
                """));
        server.createContext("/schemas.yml", exchange -> respond(exchange, authenticatedRequests, """
                Customer:
                  type: object
                  properties:
                    id:
                      type: integer
                """));
        server.start();
        try {
            URI apiUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api.yml");
            AuthenticationValue authentication = new AuthenticationValue(
                    "Authorization",
                    "Bearer test-token",
                    AuthenticationValue.AuthenticationType.HEADER,
                    ignored -> true);

            Model model = new SpecResourceLoader()
                    .withAuthentication(List.of(authentication))
                    .parse(apiUri);

            Assertions.assertEquals("integer", JSONPath.get(model, "$.components.schemas.Customer.properties.id.type"));
            Assertions.assertEquals(2, authenticatedRequests.get());
        } finally {
            server.stop(0);
        }
    }

    // ── load ──────────────────────────────────────────────────────────────────

    @Test
    void loadsClasspathResourcesWithOrWithoutTheLeadingSlash() throws IOException {
        String withoutSlash = new SpecResourceLoader()
                .load("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        String withSlash = new SpecResourceLoader()
                .load("classpath:/io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");

        Assertions.assertTrue(withoutSlash.contains("asyncapi:"));
        Assertions.assertEquals(withSlash, withoutSlash);
    }

    @Test
    void loadingAMissingClasspathResourceNamesTheResource() {
        IOException exception = Assertions.assertThrows(IOException.class,
                () -> new SpecResourceLoader().load("classpath:does/not/exist.yml"));
        Assertions.assertTrue(exception.getMessage().contains("does/not/exist.yml"), exception.getMessage());
    }

    @Test
    void loadsClasspathResourcesFromTheProjectClassLoader() throws IOException {
        Files.writeString(tempDir.resolve("on-project-classpath.yml"), "key: value", StandardCharsets.UTF_8);
        try (var classLoader = new URLClassLoader(new URL[] { tempDir.toUri().toURL() }, null)) {
            String content = new SpecResourceLoader()
                    .withProjectClassLoader(classLoader)
                    .load("classpath:on-project-classpath.yml");
            Assertions.assertEquals("key: value", content);
        }
    }

    @Test
    void loadsLocalFiles() throws IOException {
        Path file = tempDir.resolve("local.yml");
        Files.writeString(file, "key: value", StandardCharsets.UTF_8);

        Assertions.assertEquals("key: value", new SpecResourceLoader().load(file.toString()));
        Assertions.assertEquals("key: value", new SpecResourceLoader().load(file.toUri()));
    }

    @Test
    void loadsHttpResourcesSendingTheConfiguredAuthenticationHeader() throws IOException {
        AtomicInteger authenticatedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/spec.yml", exchange -> respond(exchange, authenticatedRequests, "key: value"));
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/spec.yml";
            AuthenticationValue authentication = new AuthenticationValue(
                    "Authorization",
                    "Bearer test-token",
                    AuthenticationValue.AuthenticationType.HEADER,
                    ignored -> true);

            String content = new SpecResourceLoader()
                    .withAuthentication(List.of(authentication))
                    .load(url);

            Assertions.assertEquals("key: value", content);
            Assertions.assertEquals(1, authenticatedRequests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void authenticationValuesThatDoNotMatchTheUrlAreNotSent() throws IOException {
        AtomicInteger authenticatedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/spec.yml", exchange -> respond(exchange, authenticatedRequests, "key: value"));
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/spec.yml";
            AuthenticationValue nonMatching = new AuthenticationValue(
                    "Authorization", "Bearer test-token", AuthenticationValue.AuthenticationType.HEADER,
                    ignored -> false);
            AuthenticationValue notAHeader = new AuthenticationValue(
                    "token", "test-token", AuthenticationValue.AuthenticationType.QUERY, ignored -> true);

            String content = new SpecResourceLoader()
                    .withAuthentication(List.of(nonMatching, notAHeader))
                    .load(url);

            Assertions.assertEquals("key: value", content);
            Assertions.assertEquals(0, authenticatedRequests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nullAuthenticationIsTreatedAsNone() throws IOException {
        String content = new SpecResourceLoader()
                .withAuthentication(null)
                .load("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        Assertions.assertTrue(content.contains("asyncapi:"));
    }

    // ── toUri ─────────────────────────────────────────────────────────────────

    @Test
    void toUriKeepsAbsoluteSchemesAndNormalizesClasspath() {
        SpecResourceLoader loader = new SpecResourceLoader();

        Assertions.assertEquals(URI.create("classpath:/io/example/api.yml"), loader.toUri("classpath:io/example/api.yml"));
        Assertions.assertEquals(URI.create("http://example.org/api.yml"), loader.toUri("http://example.org/api.yml"));
        Assertions.assertEquals(URI.create("https://example.org/api.yml"), loader.toUri("https://example.org/api.yml"));
        Assertions.assertEquals(URI.create("file:/tmp/api.yml"), loader.toUri("file:/tmp/api.yml"));
    }

    @Test
    void toUriResolvesPlainPathsAgainstTheWorkingDirectory() {
        URI uri = new SpecResourceLoader().toUri("src/test/resources/api.yml");

        Assertions.assertEquals("file", uri.getScheme());
        Assertions.assertTrue(uri.getPath().endsWith("/src/test/resources/api.yml"), uri.toString());
    }

    @Test
    void toUriRejectsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpecResourceLoader().toUri(null));
    }

    // ── normalizeBaseUri ──────────────────────────────────────────────────────

    @Test
    void normalizeBaseUriAddsTheMissingSlashToClasspathUris() {
        SpecResourceLoader loader = new SpecResourceLoader();

        Assertions.assertEquals(URI.create("classpath:/io/example/api.yml"),
                loader.normalizeBaseUri(URI.create("classpath:io/example/api.yml")));
        Assertions.assertEquals(URI.create("classpath:/io/example/api.yml"),
                loader.normalizeBaseUri(URI.create("classpath:/io/example/api.yml")));
    }

    @Test
    void normalizeBaseUriTurnsSchemelessAndDriveLetterUrisIntoFileUris() {
        SpecResourceLoader loader = new SpecResourceLoader();

        // a bare path has no scheme
        Assertions.assertEquals("file", loader.normalizeBaseUri(URI.create("api.yml")).getScheme());
        // a Windows drive letter parses as a one character scheme, which is never a real scheme
        Assertions.assertEquals("file", loader.normalizeBaseUri(URI.create("C:/apis/api.yml")).getScheme());
    }

    @Test
    void normalizeBaseUriLeavesRealSchemesAlone() {
        SpecResourceLoader loader = new SpecResourceLoader();
        URI http = URI.create("http://example.org/api.yml");

        Assertions.assertEquals(http, loader.normalizeBaseUri(http));
        Assertions.assertThrows(IllegalArgumentException.class, () -> loader.normalizeBaseUri(null));
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolvesRelativeReferencesAgainstTheDeclaringDocument() {
        SpecResourceLoader loader = new SpecResourceLoader();
        URI declaring = URI.create("http://example.org/apis/api.yml");

        Assertions.assertEquals(URI.create("http://example.org/apis/schemas.yml"),
                loader.resolve("./schemas.yml", declaring));
        Assertions.assertEquals(URI.create("http://example.org/shared/schemas.yml"),
                loader.resolve("../shared/schemas.yml", declaring));
        // a reference with no leading dot is still relative
        Assertions.assertEquals(URI.create("http://example.org/apis/nested/schemas.yml"),
                loader.resolve("nested/schemas.yml", declaring));
    }

    @Test
    void absoluteReferencesIgnoreTheDeclaringDocument() {
        SpecResourceLoader loader = new SpecResourceLoader();
        URI declaring = URI.create("http://example.org/apis/api.yml");

        Assertions.assertEquals(URI.create("https://other.org/schemas.yml"),
                loader.resolve("https://other.org/schemas.yml", declaring));
        Assertions.assertEquals(URI.create("classpath:/io/example/schemas.yml"),
                loader.resolve("classpath:io/example/schemas.yml", declaring));
    }

    @Test
    void referencesWithoutADeclaringDocumentResolveAgainstTheWorkingDirectory() {
        URI resolved = new SpecResourceLoader().resolve("schemas.yml", null);

        Assertions.assertEquals("file", resolved.getScheme());
        Assertions.assertTrue(resolved.getPath().endsWith("/schemas.yml"), resolved.toString());
    }

    private void respond(HttpExchange exchange, AtomicInteger authenticatedRequests, String content) throws IOException {
        if ("Bearer test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            authenticatedRequests.incrementAndGet();
        }
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
