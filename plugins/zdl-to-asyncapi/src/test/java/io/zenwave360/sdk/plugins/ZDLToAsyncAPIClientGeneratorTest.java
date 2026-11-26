package io.zenwave360.sdk.plugins;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ZDLToAsyncAPIClientGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadZDLModel(String zdl) throws Exception {
        Map<String, Object> model = new ZDLParser().withContent(zdl).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    public void test_zdl_to_asyncapi_client() throws Exception {
        var model = loadZDLModel("""
                config {
                    title "Payments Processing"
                }

                apis {
                    asyncapi client OrdersCheckoutApi "https://registry.example.com/apis/orders-checkout/asyncapi.yml"
                    asyncapi client FulfillmentShippingApi "https://registry.example.com/apis/fulfillment-shipping/asyncapi.yml"
                }

                @aggregate
                entity Payment {
                    id String
                }

                input AuthorizePaymentInput {
                    id String
                }

                service PaymentsProcessingService for (Payment) {
                    @asyncapi({ api: OrdersCheckoutApi, channel: OrderCreatedChannel })
                    authorizePayment(AuthorizePaymentInput) Payment

                    @asyncapi({ channel: PaymentFailedChannel })
                    retryPayment(AuthorizePaymentInput) Payment

                    @asyncapi({ api: FulfillmentShippingApi, channel: FulfillmentScheduledChannel })
                    scheduleFulfillment(AuthorizePaymentInput) Payment
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();

        Assertions.assertEquals(1, outputTemplates.size());
        Map<String, Object> asyncapi = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertEquals("urn:arcadiaeditions:asyncapi:payments-processing:client", asyncapi.get("id"));
        Assertions.assertEquals("AsyncAPI client for Payments Processing", JSONPath.get(asyncapi, "$.info.title"));
        Assertions.assertEquals(
                "https://registry.example.com/apis/orders-checkout/asyncapi.yml#/channels/OrderCreatedChannel",
                JSONPath.get(asyncapi, "$.channels.OrderCreatedChannel.$ref"));
        Assertions.assertEquals(
                "https://registry.example.com/apis/fulfillment-shipping/asyncapi.yml#/channels/FulfillmentScheduledChannel",
                JSONPath.get(asyncapi, "$.channels.FulfillmentScheduledChannel.$ref"));
        Assertions.assertNull(JSONPath.get(asyncapi, "$.channels.PaymentFailedChannel"));
        Assertions.assertEquals("receive", JSONPath.get(asyncapi, "$.operations.onOrderCreated.action"));
        Assertions.assertEquals("#/channels/OrderCreatedChannel", JSONPath.get(asyncapi, "$.operations.onOrderCreated.channel.$ref"));
        Assertions.assertEquals("#/channels/FulfillmentScheduledChannel", JSONPath.get(asyncapi, "$.operations.onFulfillmentScheduled.channel.$ref"));
        Assertions.assertNull(JSONPath.get(asyncapi, "$.operations.onPaymentFailed"));
    }

    @Test
    public void test_duplicate_consumed_channel_generates_method_operations() throws Exception {
        var model = loadZDLModel("""
                apis {
                    asyncapi client OrdersCheckoutApi "https://registry.example.com/apis/orders-checkout/asyncapi.yml"
                }

                @aggregate
                entity Payment {
                    id String
                }

                input PaymentInput {
                    id String
                }

                service PaymentsProcessingService for (Payment) {
                    @asyncapi({ api: OrdersCheckoutApi, channel: OrderCreatedChannel })
                    firstHandler(PaymentInput) Payment

                    @asyncapi({ api: OrdersCheckoutApi, channel: OrderCreatedChannel })
                    secondHandler(PaymentInput) Payment
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        Map<String, Object> asyncapi = mapper.readValue(generator.generate(model).getAllTemplateOutputs().get(0).getContent(), Map.class);

        Assertions.assertEquals("#/channels/OrderCreatedChannel", JSONPath.get(asyncapi, "$.operations.onOrdersCheckoutFirstHandler.channel.$ref"));
        Assertions.assertEquals("#/channels/OrderCreatedChannel", JSONPath.get(asyncapi, "$.operations.onOrdersCheckoutSecondHandler.channel.$ref"));
        Assertions.assertNull(JSONPath.get(asyncapi, "$.operations.onOrderCreated"));
    }

    @Test
    public void test_multiple_asyncapi_annotations_on_one_method() throws Exception {
        var model = loadZDLModel("""
                apis {
                    asyncapi client PaymentsProcessingApi "https://registry.example.com/apis/payments-processing/asyncapi.yml"
                }

                @aggregate
                entity StockReservation {
                    id String
                }

                input ReleaseStockInput {
                    id String
                }

                service CatalogInventoryService for (StockReservation) {
                    @asyncapi(api: PaymentsProcessingApi, channel: "payment-declined-event-v1")
                    @asyncapi(api: PaymentsProcessingApi, channel: "payment-retry-exhausted-event-v1")
                    @asyncapi(api: PaymentsProcessingApi, channel: "payment-voided-event-v1")
                    releaseStock(ReleaseStockInput) StockReservation
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        Map<String, Object> asyncapi = mapper.readValue(generator.generate(model).getAllTemplateOutputs().get(0).getContent(), Map.class);

        var base = "https://registry.example.com/apis/payments-processing/asyncapi.yml#/channels/";
        Map<String, Map<String, Object>> channels = (Map) asyncapi.get("channels");
        Map<String, Map<String, Object>> operations = (Map) asyncapi.get("operations");
        Assertions.assertEquals(3, channels.size());
        Assertions.assertEquals(base + "payment-declined-event-v1", channels.get("payment-declined-event-v1").get("$ref"));
        Assertions.assertEquals(base + "payment-retry-exhausted-event-v1", channels.get("payment-retry-exhausted-event-v1").get("$ref"));
        Assertions.assertEquals(base + "payment-voided-event-v1", channels.get("payment-voided-event-v1").get("$ref"));
        Assertions.assertEquals(3, operations.size());
        Assertions.assertEquals("#/channels/payment-declined-event-v1", JSONPath.get(asyncapi, "$.operations.onPaymentDeclinedEventV1.channel.$ref"));
        Assertions.assertEquals("#/channels/payment-retry-exhausted-event-v1", JSONPath.get(asyncapi, "$.operations.onPaymentRetryExhaustedEventV1.channel.$ref"));
        Assertions.assertEquals("#/channels/payment-voided-event-v1", JSONPath.get(asyncapi, "$.operations.onPaymentVoidedEventV1.channel.$ref"));
    }

    @Test
    public void test_undeclared_api_in_non_last_asyncapi_annotation_fails() throws Exception {
        var model = loadZDLModel("""
                apis {
                    asyncapi client OrdersCheckoutApi "https://registry.example.com/apis/orders-checkout/asyncapi.yml"
                }

                @aggregate
                entity Payment {
                    id String
                }

                input PaymentInput {
                    id String
                }

                service PaymentsProcessingService for (Payment) {
                    @asyncapi(api: MissingApi, channel: OrderCancelledChannel)
                    @asyncapi(api: OrdersCheckoutApi, channel: OrderCreatedChannel)
                    firstHandler(PaymentInput) Payment
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.generate(model));
    }

    @Test
    public void test_undeclared_api_fails() throws Exception {
        var model = loadZDLModel("""
                @aggregate
                entity Payment {
                    id String
                }

                input PaymentInput {
                    id String
                }

                service PaymentsProcessingService for (Payment) {
                    @asyncapi({ api: MissingApi, channel: OrderCreatedChannel })
                    firstHandler(PaymentInput) Payment
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.generate(model));
    }

    @Test
    public void test_no_consumed_channels_generates_empty_objects() throws Exception {
        var model = loadZDLModel("""
                @aggregate
                entity Payment {
                    id String
                }

                input PaymentInput {
                    id String
                }

                service PaymentsProcessingService for (Payment) {
                    createPayment(PaymentInput) Payment
                }
                """);

        ZDLToAsyncAPIClientGenerator generator = new ZDLToAsyncAPIClientGenerator();
        String content = generator.generate(model).getAllTemplateOutputs().get(0).getContent();
        Assertions.assertTrue(content.contains("channels: {}"), content);
        Assertions.assertTrue(content.contains("operations: {}"), content);
        Map<String, Object> asyncapi = mapper.readValue(content, Map.class);
        Assertions.assertEquals(Map.of(), asyncapi.get("channels"));
        Assertions.assertEquals(Map.of(), asyncapi.get("operations"));
    }
}
