package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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
