package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCatalogZflFlowTest {

    private static final Path OUTPUT = Path.of("target", "event-catalog-zfl-flow-test");

    @Test
    void generatesLinkedFlowWithCatalogResourcesBranchesAndRepeatedOperations() throws Exception {
        new MainGenerator().generate(new EventCatalogPlugin()
                .withOption("inputFile", fixture("zenwave-architecture.yml").toString())
                .withOption("preferredSource", "workspace")
                .withOption("allowFallback", false)
                .withOption("outputFolder", OUTPUT.toString()));

        String flow = Files.readString(OUTPUT.resolve(
                "domains/architecture/flows/place-order-flow/index.mdx"));
        assertTrue(flow.contains("<Flow id=\"place-order-flow\" version=\"1.2.0\" />"));
        assertTrue(flow.contains("type: \"actor\""));
        assertTrue(flow.contains("type: \"node\""));
        assertTrue(flow.contains("name: \"Customer\""));
        assertTrue(flow.contains("Scheduled: 10 minutes after OrderCreated"));
        assertTrue(flow.contains("id: \"orders.checkout\""));
        assertTrue(flow.contains("id: \"orders.checkout.start-order-checkout\""));
        assertTrue(flow.contains("type: \"command\""));
        assertFalse(flow.contains("id: \"start-order-checkout-2\""),
                "A start signal must not be rendered again as a synthetic event");
        assertTrue(flow.contains("id: \"orders.checkout.order-created-event-v1\""));
        assertTrue(flow.contains("occurrences.authorizePayment%0040when%005BOrderCreated%005D"));
        assertTrue(flow.contains("occurrences.authorizePayment%0040when%005BPaymentRetried%005D"));
        assertEquals(2, steps(flow).stream()
                .filter(step -> "Authorize Payment Command".equals(step.get("title")))
                .count(), "Repeated operation occurrences must retain distinct graph step IDs");
        assertTrue(flow.contains("label: \"authorized\""));
        assertTrue(flow.contains("zfl_outcome/flows.PlaceOrderFlow.outcomes.completed"));
        assertTrue(flow.contains("Records the completed payment as an internal order operation."));
        Map<String, Object> finalizeOrder = step(flow,
                "artifact/architecture/place-order-flow/zfl_step/flows.PlaceOrderFlow.occurrences.finalizeOrder%0040when%005BPaymentAuthorized%005D");
        assertFalse(finalizeOrder.containsKey("service"),
                "ArchCatalog flow steps permit only one typed payload");
        Map<String, Object> finalizeOrderServiceStep = step(flow,
                "artifact/architecture/place-order-flow/zfl_step/flows.PlaceOrderFlow.occurrences.finalizeOrder%0040when%005BPaymentAuthorized%005D:service");
        Map<?, ?> finalizeOrderService = assertInstanceOf(Map.class, finalizeOrderServiceStep.get("service"));
        assertEquals("orders.checkout", finalizeOrderService.get("id"));
        assertEquals("1.2.0", finalizeOrderService.get("version"));
        assertFalse(finalizeOrderServiceStep.containsKey("custom"),
                "ArchCatalog flow steps permit only one typed payload");
        Map<?, ?> finalizeOrderCustom = assertInstanceOf(Map.class, finalizeOrder.get("custom"));
        assertEquals("operation", finalizeOrderCustom.get("type"));
        assertEquals("blue", finalizeOrderCustom.get("color"));
        assertFalse(finalizeOrder.containsKey("message"));
        assertTrue(flow.contains("type: \"unresolved-operation\""));
        assertTrue(flow.contains("color: \"red\""));
        assertFalse(flow.contains("type: \"service\""));
        assertFalse(flow.contains("type: \"custom\""));
        assertValidConnections(flow);

        String domain = Files.readString(OUTPUT.resolve("domains/architecture/index.mdx"));
        assertTrue(domain.contains("id: \"place-order-flow\""));
        String orders = Files.readString(OUTPUT.resolve(
                "domains/orders/services/orders.checkout/index.mdx"));
        String payments = Files.readString(OUTPUT.resolve(
                "domains/payments/services/payments.processing/index.mdx"));
        assertTrue(orders.contains("id: \"place-order-flow\""));
        assertTrue(payments.contains("id: \"place-order-flow\""));
    }

    @SuppressWarnings("unchecked")
    private void assertValidConnections(String mdx) throws Exception {
        List<Map<String, Object>> steps = steps(mdx);
        Set<String> ids = new HashSet<>();
        Set<String> allowedTypes = Set.of("node", "message", "agent", "user", "actor");
        for (Map<String, Object> step : steps) {
            assertTrue(ids.add(step.get("id").toString()), "Flow step IDs must be unique");
            assertTrue(allowedTypes.contains(step.get("type").toString()),
                    () -> "Unsupported EventCatalog flow step type: " + step.get("type"));
            long typedPayloads = List.of("message", "agent", "service", "flow", "container",
                            "dataProduct", "actor", "custom").stream()
                    .filter(step::containsKey)
                    .count();
            assertTrue(typedPayloads <= 1,
                    () -> "ArchCatalog flow steps permit at most one typed payload: " + step.get("id"));
        }
        for (Map<String, Object> step : steps) {
            if (step.get("next_step") instanceof Map<?, ?> next) {
                assertTrue(ids.contains(next.get("id").toString()), "next_step must resolve");
            }
            if (step.get("next_steps") instanceof List<?> nextSteps) {
                for (Object raw : nextSteps) {
                    Map<?, ?> next = (Map<?, ?>) raw;
                    assertTrue(ids.contains(next.get("id").toString()), "next_steps must resolve");
                }
            }
        }
    }

    private Map<String, Object> step(String mdx, String id) throws Exception {
        return steps(mdx).stream()
                .filter(step -> id.equals(step.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing flow step: " + id));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> steps(String mdx) throws Exception {
        String yaml = mdx.substring(4, mdx.indexOf("\n---", 4));
        Map<String, Object> frontmatter = new ObjectMapper(new YAMLFactory()).readValue(yaml, Map.class);
        return (List<Map<String, Object>>) frontmatter.get("steps");
    }

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(EventCatalogZflFlowTest.class.getClassLoader()
                .getResource("zfl-flow/" + name).toURI());
    }
}
