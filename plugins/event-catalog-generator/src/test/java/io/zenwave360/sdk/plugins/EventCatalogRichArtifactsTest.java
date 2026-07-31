package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogRichArtifactsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @TempDir
    Path tempDir;

    @Test
    void remoteSpecificationComponentsAreGeneratedOnlyForHttpUrlsAndUseTheSmartMessageName() throws Exception {
        var method = EventCatalogGenerator.class.getDeclaredMethod("messageBody", String.class, Map.class);
        method.setAccessible(true);
        String body = (String) method.invoke(new EventCatalogGenerator(), "event", Map.of(
                "summary", "Order created",
                "schemaPath", "https://contracts.example.com/OrderCreated.avsc",
                "_remoteSchemaUrl", "https://contracts.example.com/asyncapi.yml",
                "_remoteSchemaMessage", "OrderCreated"));

        assertTrue(body.contains(
                "import RemoteSpecificationSchema from '@catalog/components/RemoteSpecificationSchema.astro';"));
        assertTrue(body.contains(
                "<RemoteSpecificationSchema url=\"https://contracts.example.com/asyncapi.yml\" message=\"OrderCreated\" />"));
        assertFalse(body.contains("<SchemaViewer"),
                "A remote schema must have exactly one viewer usage");
    }

    @Test
    void generatesCatalogFromRichArtifactsAndSkipsMalformedOptionalInputs() throws Exception {
        Path serviceDir = tempDir.resolve("sales/orders/order-service");
        Files.createDirectories(serviceDir.resolve("schemas"));
        write(tempDir.resolve("zenwave-architecture.yml"), manifest());
        write(serviceDir.resolve("SUMMARY.md"), "# Order service\n\nOwns the order lifecycle.");
        write(serviceDir.resolve("asyncapi.yml"), asyncApi());
        write(serviceDir.resolve("asyncapi-client.yml"), asyncApiClient());
        write(serviceDir.resolve("openapi.yml"), openApi());
        write(serviceDir.resolve("domain-model.zdl"), zdl());
        write(serviceDir.resolve("malformed-asyncapi.yml"), "channels: [not-a-map");
        write(serviceDir.resolve("malformed-openapi.yml"), "paths: [not-a-map");
        write(serviceDir.resolve("malformed-domain-model.zdl"), "entity Broken {");
        write(serviceDir.resolve("schemas/Order.yaml"), "type: object\nproperties: {}\n");
        write(serviceDir.resolve("schemas/OrderCreated.avsc"), """
                {
                  "type": "record",
                  "name": "OrderCreated",
                  "fields": [
                    { "name": "orderId", "type": "string" }
                  ]
                }
                """);

        Path output = tempDir.resolve("catalog");
        new MainGenerator().generate(new EventCatalogPlugin()
                .withOption("inputFile", tempDir.resolve("zenwave-architecture.yml").toString())
                .withOption("outputFolder", output.toString())
                .withOption("preferredSource", "workspace")
                .withOption("linkSource", "workspace"));

        Path serviceBase = output.resolve("domains/sales/subdomains/sales.orders/services/sales.orders.order-service");
        assertTrue(Files.exists(serviceBase.resolve("index.mdx")));
        assertTrue(Files.readString(serviceBase.resolve("index.mdx")).contains("Owns the order lifecycle."));

        Map<String, Object> service = frontmatter(serviceBase.resolve("index.mdx"));
        assertTrue(pointerIds(service.get("sends")).contains("external-order-events"),
                "An unowned client channel should use its stable channel-key fallback");
        assertTrue(pointerIds(service.get("receives")).contains("external-order-commands"));

        Path listOrders = serviceBase.resolve("queries/sales.orders.order-service.listOrders/index.mdx");
        Path getOrder = serviceBase.resolve("queries/sales.orders.order-service.getOrder/index.mdx");
        Path inlineOrder = serviceBase.resolve("queries/sales.orders.order-service.inlineOrder/index.mdx");
        assertTrue(Files.exists(listOrders));
        assertTrue(Files.exists(getOrder));
        assertTrue(Files.exists(inlineOrder));
        assertFalse(Files.exists(serviceBase.resolve("queries/sales.orders.order-service.missingOperationId/index.mdx")));
        assertFalse(frontmatter(listOrders).containsKey("schemaPath"));
        assertTrue(frontmatter(getOrder).get("schemaPath").toString().endsWith("schemas/Order.yaml"));
        assertFalse(frontmatter(inlineOrder).containsKey("schemaPath"));
        String getOrderContent = Files.readString(getOrder);
        assertTrue(getOrderContent.contains("<SchemaViewer"));
        assertFalse(getOrderContent.contains("RemoteSpecificationSchema"),
                "Local file specifications must keep using the legacy local schema viewer");
        String inlineOrderContent = Files.readString(inlineOrder);
        assertFalse(inlineOrderContent.contains("RemoteSpecificationSchema"),
                "Local file specifications must not be passed to build-time HTTP fetching");

        Map<String, Object> order = frontmatter(serviceBase.resolve("entities/sales.orders.order-service.order/index.mdx"));
        assertEquals("orderNumber", order.get("identifier"));
        List<Map<String, Object>> properties = maps(order.get("properties"));
        Map<String, Object> lineItems = property(properties, "lineItems");
        assertEquals("OrderLine", lineItems.get("type"));
        assertEquals("OrderLine", lineItems.get("references"));
        assertTrue(maps(order.get("properties")).stream()
                .anyMatch(property -> "status".equals(property.get("name"))
                        && stringList(property.get("enum")).containsAll(List.of("NEW", "FULFILLED"))));

        Map<String, Object> member = frontmatter(serviceBase.resolve("entities/sales.orders.order-service.member/index.mdx"));
        assertEquals("Group", property(maps(member.get("properties")), "groups").get("references"));
        Map<String, Object> address = frontmatter(serviceBase.resolve("entities/sales.orders.order-service.address/index.mdx"));
        assertEquals("Customer", property(maps(address.get("properties")), "customer").get("references"));
        Map<String, Object> user = frontmatter(serviceBase.resolve("entities/sales.orders.order-service.user/index.mdx"));
        assertEquals("Profile", property(maps(user.get("properties")), "profile").get("references"));

        Path event = serviceBase.resolve("events/sales.orders.order-service.order-created/index.mdx");
        assertTrue(frontmatter(event).get("schemaPath").toString().endsWith("schemas/OrderCreated.avsc"));
        String eventContent = Files.readString(event);
        assertTrue(eventContent.contains("<SchemaViewer"));
        assertFalse(eventContent.contains("RemoteSpecificationSchema"),
                "Local file specifications must keep using the legacy local schema viewer");
        Path command = serviceBase.resolve("commands/sales.orders.order-service.create-order/index.mdx");
        assertFalse(frontmatter(command).containsKey("schemaPath"));
        String commandContent = Files.readString(command);
        assertFalse(commandContent.contains("RemoteSpecificationSchema"),
                "Local inline schemas must not be passed to build-time HTTP fetching");
    }

    private String manifest() {
        return """
                config:
                  title: Rich artifact catalog
                  version: 3.0.0
                  contentResolution: [workspace]
                domains:
                  sales:
                    name: Sales
                    subdomains:
                      orders:
                        name: Orders
                        services:
                          order-service:
                            name: Order Service
                            version: 2.1.0
                            docs:
                              summary: SUMMARY.md
                            artifacts:
                              - type: asyncapi
                                path: asyncapi.yml
                              - type: asyncapi
                                path: malformed-asyncapi.yml
                              - type: asyncapi
                                path: missing-asyncapi.yml
                              - type: asyncapi-client
                                path: asyncapi-client.yml
                              - type: openapi
                                path: openapi.yml
                              - type: openapi
                                path: malformed-openapi.yml
                              - type: openapi
                                path: missing-openapi.yml
                              - type: zdl
                                path: domain-model.zdl
                              - type: zdl
                                path: malformed-domain-model.zdl
                              - type: zdl
                                path: missing-domain-model.zdl
                """;
    }

    private String asyncApi() {
        return """
                asyncapi: 3.0.0
                info:
                  title: Order events
                servers:
                  local:
                    protocol: kafka
                  incomplete: not-a-map
                channels:
                  order-created:
                    address: sales.orders.order-created
                    messages:
                      OrderCreated:
                        $ref: '#/components/messages/OrderCreatedMessage'
                  create-order:
                    address: sales.orders.create-order
                    summary: Create Order
                    messages:
                      CreateOrder:
                        $ref: '#/components/messages/CreateOrderMessage'
                  no-address:
                    summary: No Address
                operations:
                  publishOrderCreated:
                    action: send
                    channel:
                      $ref: '#/channels/order-created'
                  receiveCreateOrder:
                    action: receive
                    channel:
                      address: sales.orders.create-order
                      summary: Create Order
                      messages:
                        CreateOrder:
                          $ref: '#/components/messages/CreateOrderMessage'
                  missingAction:
                    channel:
                      $ref: '#/channels/order-created'
                  invalidReference:
                    action: send
                    channel:
                      $ref: '#/components/channels/order-created'
                  missingAddress:
                    action: send
                    channel:
                      $ref: '#/channels/no-address'
                  unindexedInlineChannel:
                    action: send
                    channel:
                      address: sales.orders.unknown
                components:
                  messages:
                    OrderCreatedMessage:
                      name: OrderCreated
                      payload:
                        schemaFormat: application/vnd.apache.avro+json;version=1.9.0
                        schema:
                          $ref: './schemas/OrderCreated.avsc'
                    CreateOrderMessage:
                      name: CreateOrder
                      payload:
                        type: object
                        required:
                          - orderId
                        properties:
                          orderId:
                            type: string
                  schemas:
                    UnusedSchema:
                      type: object
                """;
    }

    private String asyncApiClient() {
        return """
                asyncapi: 3.0.0
                info:
                  title: External order clients
                  version: 1.0.0
                channels:
                  external-order-events:
                    address: external.orders.events
                  external-order-commands:
                    address: external.orders.commands
                  no-address: {}
                operations:
                  publishExternal:
                    action: send
                    channel:
                      $ref: '#/channels/external-order-events'
                  receiveExternal:
                    action: receive
                    channel:
                      $ref: '#/channels/external-order-commands'
                  unsupportedAction:
                    action: request
                    channel:
                      $ref: '#/channels/external-order-events'
                  missingAction:
                    channel:
                      $ref: '#/channels/external-order-events'
                  missingAddress:
                    action: send
                    channel:
                      $ref: '#/channels/no-address'
                """;
    }

    private String openApi() {
        return """
                openapi: 3.0.3
                info:
                  title: Orders API
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses: {}
                  /orders/{id}:
                    get:
                      operationId: getOrder
                      summary: Get Order
                      description: Returns an order
                      responses:
                        '200':
                          content:
                            application/json:
                              schema:
                                $ref: './schemas/Order.yaml#/Order'
                        '404':
                          description: Not found
                  /inline:
                    get:
                      operationId: inlineOrder
                      responses:
                        '200':
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Order'
                  /missing-id:
                    get:
                      summary: Missing operation id
                  /not-a-query:
                    post:
                      operationId: createOrder
                """;
    }

    private String zdl() {
        return """
                config {}

                /** Order aggregate. */
                @aggregate
                entity Order {
                    @naturalId
                    orderNumber String required /** Stable business identifier. */
                    lineItems OrderLine[] required
                    status OrderStatus required
                    ownerId UUID
                }

                entity OrderLine {
                    id UUID
                    order Order
                }

                enum OrderStatus { NEW, FULFILLED }

                relationship OneToMany {
                    Order{lineItems required} to OrderLine{order required}
                }

                entity User {
                    profile Profile
                }

                entity Profile {
                    user User
                }

                relationship OneToOne {
                    User{profile} to Profile{user}
                }

                entity Customer {
                    addresses Address[]
                }

                entity Address {
                    customer Customer
                }

                relationship ManyToOne {
                    Address{customer} to Customer{addresses}
                }

                entity Member {
                    groups Group[]
                }

                entity Group {
                    members Member[]
                }

                relationship ManyToMany {
                    Member{groups} to Group{members}
                }

                entity EmptyEntity {}
                """;
    }

    private void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private Map<String, Object> frontmatter(Path page) throws Exception {
        assertTrue(Files.exists(page), "Expected generated page " + page);
        String content = Files.readString(page);
        int end = content.indexOf("---\n", 4);
        assertTrue(end > 4, "Expected frontmatter in " + page);
        return YAML.readValue(content.substring(4, end), MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        return value == null ? List.of() : (List<String>) value;
    }

    private List<String> pointerIds(Object value) {
        return maps(value).stream().map(pointer -> pointer.get("id").toString()).toList();
    }

    private Map<String, Object> property(List<Map<String, Object>> properties, String name) {
        return properties.stream()
                .filter(property -> name.equals(property.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing property " + name));
    }
}
