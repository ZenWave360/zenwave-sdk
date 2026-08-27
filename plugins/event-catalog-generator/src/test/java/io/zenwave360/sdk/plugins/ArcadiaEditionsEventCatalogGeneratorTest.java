package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadiaEditionsEventCatalogGeneratorTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> FLOW_STEP_PAYLOADS = List.of(
            "message", "agent", "service", "flow", "container", "dataProduct", "actor", "custom");
    private static final String ARCHITECTURE_URL =
            "https://raw.githubusercontent.com/arcadia-editions/arcadia-editions-docs/main/zenwave-architecture.yml";
    private static final Path OUTPUT_FOLDER = Path.of(
            "target", "arcadia-event-catalog-output-test");

    @Test
    @EnabledIfSystemProperty(named = "arcadia.integration", matches = "true",
            disabledReason = "Manual test: depends on the external Arcadia Editions architecture")
    void generatesEventCatalogContentFromArcadiaArchitecture() throws Exception {
        String architecture = System.getProperty("arcadia.architecture", ARCHITECTURE_URL);
        boolean workspaceArchitecture = !architecture.startsWith("http://") && !architecture.startsWith("https://");
        new MainGenerator().generate(
                new EventCatalogPlugin()
                        .withOption("inputFile", architecture)
                        .withOption("preferredSource", workspaceArchitecture ? "workspace" : "git")
                        .withOption("allowFallback", false)
                        .withOption("linkSource", workspaceArchitecture ? "workspace" : "git")
                        .withOption("outputFolder", OUTPUT_FOLDER.toString()));

        assertTrue(Files.isDirectory(OUTPUT_FOLDER), "EventCatalog output folder must be created");
        try (var generatedFiles = Files.walk(OUTPUT_FOLDER)) {
            assertFalse(generatedFiles
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".mdx"))
                            .toList()
                            .isEmpty(),
                    "At least one EventCatalog MDX content file must be generated");
        }
        assertArchCatalogFlowStepShapes(OUTPUT_FOLDER.resolve(
                "domains/architecture/flows/place-order-flow/index.mdx"));

        if (workspaceArchitecture) {
            String flow = Files.readString(OUTPUT_FOLDER.resolve(
                    "domains/architecture/flows/place-order-flow/index.mdx"));
            assertTrue(flow.contains(
                    "id: \"payments.payment-processing.payments-processing\"\n    version: \"0.1.0\""));
            assertFalse(flow.contains("url: \"/docs/services/"));
            assertTrue(flow.contains("id: \"catalog.inventory-management.catalog-inventory.reserveStock\""));
            assertTrue(flow.contains("type: \"command\""));
            assertFalse(flow.contains("has no matching EventCatalog command"));
            assertFalse(flow.contains("start-order-checkout-2"));
            assertFalse(flow.contains("reservation-expired-2"));
        }
    }

    @SuppressWarnings("unchecked")
    private void assertArchCatalogFlowStepShapes(Path flowPage) throws Exception {
        String mdx = Files.readString(flowPage);
        String yaml = mdx.substring(4, mdx.indexOf("\n---", 4));
        Map<String, Object> frontmatter = YAML.readValue(yaml, Map.class);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) frontmatter.get("steps");
        for (int index = 0; index < steps.size(); index++) {
            Map<String, Object> step = steps.get(index);
            long typedPayloads = FLOW_STEP_PAYLOADS.stream().filter(step::containsKey).count();
            int stepIndex = index;
            assertTrue(typedPayloads <= 1,
                    () -> "ArchCatalog flow step " + stepIndex + " has multiple typed payloads: " + step);
        }
    }
}
