package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadiaEditionsEventCatalogGeneratorTest {

    private static final String ARCHITECTURE_URL =
            "https://raw.githubusercontent.com/arcadia-editions/arcadia-editions-docs/main/zenwave-architecture.yml";
    private static final Path OUTPUT_FOLDER = Path.of(
            "target", "arcadia-event-catalog-output-test");

    @Test
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
    }
}
