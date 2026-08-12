package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogConsumersTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDeclaredConsumersAndClassifiesMessagesWithExplicitPrecedence() throws Exception {
        Path provider = Files.createDirectories(tempDir.resolve("provider"));
        Path consumer = Files.createDirectories(tempDir.resolve("consumer"));
        Path legacy = Files.createDirectories(tempDir.resolve("legacy"));
        Files.writeString(tempDir.resolve("zenwave-architecture.yml"), manifest());
        Files.writeString(provider.resolve("asyncapi.yml"), providerAsyncApi());
        Files.writeString(provider.resolve("openapi.yml"), providerOpenApi());
        Files.writeString(consumer.resolve("asyncapi-client.yml"), consumerAsyncApi());
        Files.writeString(consumer.resolve("openapi.yml"), consumerOpenApi());
        Files.writeString(legacy.resolve("asyncapi-client.yml"), legacyConsumerAsyncApi());

        Path output = tempDir.resolve("catalog");
        new MainGenerator().generate(new EventCatalogPlugin()
                .withOption("inputFile", tempDir.resolve("zenwave-architecture.yml").toString())
                .withOption("outputFolder", output.toString())
                .withOption("preferredSource", "workspace")
                .withOption("allowFallback", false)
                .withOption("linkSource", "workspace"));

        Path serviceBase = output.resolve("domains/test/services/provider.service");
        String event = Files.readString(serviceBase.resolve("events/provider.service.order-created/index.mdx"));
        String command = Files.readString(serviceBase.resolve("commands/provider.service.create-order/index.mdx"));
        String query = Files.readString(serviceBase.resolve("queries/provider.service.listOrders/index.mdx"));
        String service = Files.readString(serviceBase.resolve("index.mdx"));
        String legacyService = Files.readString(output.resolve("domains/test/services/legacy.service/index.mdx"));

        assertTrue(event.contains("consumer.service-1.0.0"), event);
        assertTrue(event.contains("provider.service-1.0.0"), "Provider self-consumption must remain on the event");
        assertTrue(event.contains("consumer.service.onOrderCreated"), event);
        assertTrue(event.contains("matched by external-ref"), event);

        assertTrue(command.contains("producers:"), command);
        assertTrue(command.contains("consumer.service-1.0.0"), command);
        assertTrue(command.contains("consumers:"), command);
        assertTrue(command.contains("provider.service-1.0.0"), command);
        assertTrue(command.contains("consumer.service.sendCreateOrder"), command);
        assertFalse(query.contains("consumer.service-1.0.0"),
                "Declared OpenAPI consumers belong on the service page, not every query");
        assertTrue(service.contains("## Declared API consumers"), service);
        assertTrue(service.contains("`consumer.service`"), service);
        assertFalse(service.contains("service.## Declared API consumers"), service);
        assertFalse(Files.exists(serviceBase.resolve("commands/provider.service.order-created/index.mdx")),
                "A provider send+receive event channel must not also become a command");
        assertTrue(Files.exists(serviceBase.resolve("commands/provider.service.event-looking-channel/index.mdx")),
                "Channel x-message-type must override event naming and send direction");
        assertFalse(Files.exists(serviceBase.resolve("events/provider.service.event-looking-channel/index.mdx")));
        assertTrue(Files.exists(serviceBase.resolve("events/provider.service.command-looking-channel/index.mdx")),
                "Message x-message-type must override command naming and receive direction");
        assertTrue(Files.exists(serviceBase.resolve("events/provider.service.named-event-channel/index.mdx")),
                "Event naming must override receive-only direction when no extension is present");
        String coincidental = Files.readString(
                serviceBase.resolve("events/provider.service.coincidental-channel/index.mdx"));
        assertFalse(coincidental.contains("consumer.service.coincidentalReceive"),
                "A local channel-key coincidence without an external ref or address must not create an edge");
        assertTrue(legacyService.contains("provider.service.order-created"),
                "Qualified consumers must not disable legacy address matching for unrelated services");
    }

    private String manifest() {
        return """
                config:
                  version: 1.0.0
                  contentResolution: [workspace]
                  artifactIdExpression: "${artifact.fileNameWithoutExtension}"
                  sources:
                    workspace:
                      basePathExpression: "${owner.repository}"
                domains:
                  test:
                    services:
                      provider:
                        id: provider.service
                        repository: provider
                        version: 1.0.0
                        artifacts:
                          - type: asyncapi
                            path: asyncapi.yml
                            version: 1.0.0
                          - type: openapi
                            path: openapi.yml
                            version: 1.0.0
                        consumers:
                          - "consumer.service#asyncapi-client"
                          - "consumer.service#openapi"
                      consumer:
                        id: consumer.service
                        repository: consumer
                        version: 1.0.0
                        artifacts:
                          - type: asyncapi-client
                            path: asyncapi-client.yml
                            version: 1.0.0
                          - type: openapi
                            path: openapi.yml
                            version: 1.0.0
                      legacy:
                        id: legacy.service
                        repository: legacy
                        version: 1.0.0
                        artifacts:
                          - type: asyncapi-client
                            path: asyncapi-client.yml
                            version: 1.0.0
                """;
    }

    private String providerAsyncApi() {
        return """
                asyncapi: 3.0.0
                info:
                  title: Provider contract
                  version: 1.0.0
                channels:
                  order-created:
                    address: test.order-created.event.v1
                    messages:
                      OrderCreated:
                        payload:
                          type: object
                  create-order:
                    address: test.create-order.command.v1
                    messages:
                      CreateOrder:
                        payload:
                          type: object
                  event-looking-channel:
                    x-message-type: command
                    messages:
                      ForcedCommand:
                        payload:
                          type: object
                  command-looking-channel:
                    messages:
                      ForcedEvent:
                        x-message-type: event
                        payload:
                          type: object
                  named-event-channel:
                    messages:
                      NamedEvent:
                        payload:
                          type: object
                  coincidental-channel:
                    messages:
                      Coincidental:
                        payload:
                          type: object
                operations:
                  publishOrderCreated:
                    action: send
                    channel:
                      $ref: '#/channels/order-created'
                  handleOwnOrderCreated:
                    action: receive
                    channel:
                      $ref: '#/channels/order-created'
                  handleCreateOrder:
                    action: receive
                    channel:
                      $ref: '#/channels/create-order'
                  publishForcedCommand:
                    action: send
                    channel:
                      $ref: '#/channels/event-looking-channel'
                  handleForcedEvent:
                    action: receive
                    channel:
                      $ref: '#/channels/command-looking-channel'
                  handleNamedEvent:
                    action: receive
                    channel:
                      $ref: '#/channels/named-event-channel'
                  publishCoincidental:
                    action: send
                    channel:
                      $ref: '#/channels/coincidental-channel'
                """;
    }

    private String consumerAsyncApi() {
        return """
                asyncapi: 3.0.0
                info:
                  title: Consumer contract
                  version: 1.0.0
                channels:
                  order-created:
                    $ref: ../provider/asyncapi.yml#/channels/order-created
                  create-order:
                    $ref: ../provider/asyncapi.yml#/channels/create-order
                  coincidental-channel: {}
                operations:
                  onOrderCreated:
                    action: receive
                    channel:
                      $ref: '#/channels/order-created'
                  sendCreateOrder:
                    action: send
                    channel:
                      $ref: '#/channels/create-order'
                  coincidentalReceive:
                    action: receive
                    channel:
                      $ref: '#/channels/coincidental-channel'
                """;
    }

    private String legacyConsumerAsyncApi() {
        return """
                asyncapi: 3.0.0
                info:
                  title: Legacy address consumer
                  version: 1.0.0
                channels:
                  order-created:
                    address: test.order-created.event.v1
                operations:
                  receiveOrderCreated:
                    action: receive
                    channel:
                      $ref: '#/channels/order-created'
                """;
    }

    private String providerOpenApi() {
        return """
                openapi: 3.0.3
                info:
                  title: Provider HTTP API
                  version: 1.0.0
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '200':
                          description: Orders
                """;
    }

    private String consumerOpenApi() {
        return """
                openapi: 3.0.3
                info:
                  title: Consumer HTTP API
                  version: 1.0.0
                paths: {}
                """;
    }
}
