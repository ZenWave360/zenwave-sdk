package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The merge / overlay post-processing step every YAML generating plugin inherits.
 */
class YamlOverlayGeneratedFilesProcessorTest {

    @TempDir
    Path tempDir;

    /** Stands in for AsyncAPIOverlayProcessor and OpenAPIOverlayProcessor. */
    static class TestProcessor extends YamlOverlayGeneratedFilesProcessor {

        String mergeFile;
        List<String> overlayFiles;
        UnaryOperator<Map<String, Object>> documentOrderer = UnaryOperator.identity();

        @Override
        protected String getMergeFile() {
            return mergeFile;
        }

        @Override
        protected List<String> getOverlayFiles() {
            return overlayFiles;
        }

        @Override
        protected UnaryOperator<Map<String, Object>> getDocumentOrderer() {
            return documentOrderer;
        }
    }

    private static final String GENERATED = """
            asyncapi: 3.0.0
            info:
              title: Generated API
              version: 0.0.1
            """;

    private static GeneratedProjectFiles filesWith(TemplateOutput... outputs) {
        var generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.addAll(List.of(outputs));
        return generatedProjectFiles;
    }

    private static TemplateOutput yaml(String targetFile, String content) {
        return new TemplateOutput(targetFile, content, OutputFormatType.YAML.toString());
    }

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    // ── no-op cases ───────────────────────────────────────────────────────────

    @Test
    void doesNothingWhenNeitherAMergeFileNorOverlaysAreConfigured() throws IOException {
        var output = yaml("asyncapi.yml", GENERATED);
        var processor = new TestProcessor();

        processor.process(filesWith(output));

        Assertions.assertEquals(GENERATED, output.getContent());
    }

    @Test
    void doesNothingWhenTheOverlayListIsEmpty() throws IOException {
        var output = yaml("asyncapi.yml", GENERATED);
        var processor = new TestProcessor();
        processor.overlayFiles = List.of();

        processor.process(filesWith(output));

        Assertions.assertEquals(GENERATED, output.getContent());
    }

    // ── merge ─────────────────────────────────────────────────────────────────

    @Test
    void appliesTheMergeFileOnTopOfTheGeneratedDocument() throws IOException {
        Path mergeFile = write("merge.yml", """
                info:
                  version: 9.9.9
                servers:
                  production:
                    host: example.org
                """);
        var output = yaml("asyncapi.yml", GENERATED);
        var processor = new TestProcessor();
        processor.mergeFile = mergeFile.toString();

        processor.process(filesWith(output));

        Assertions.assertTrue(output.getContent().contains("9.9.9"), output.getContent());
        Assertions.assertTrue(output.getContent().contains("example.org"), output.getContent());
        // values not mentioned by the merge file survive
        Assertions.assertTrue(output.getContent().contains("Generated API"), output.getContent());
    }

    // ── overlay ───────────────────────────────────────────────────────────────

    @Test
    void appliesOverlayFilesInOrder() throws IOException {
        Path first = write("first.yml", overlayRenaming("Renamed once"));
        Path second = write("second.yml", overlayRenaming("Renamed twice"));
        var output = yaml("asyncapi.yml", GENERATED);
        var processor = new TestProcessor();
        processor.overlayFiles = List.of(first.toString(), second.toString());

        processor.process(filesWith(output));

        Assertions.assertTrue(output.getContent().contains("Renamed twice"), output.getContent());
        Assertions.assertFalse(output.getContent().contains("Renamed once"), output.getContent());
    }

    @Test
    void appliesTheDocumentOrdererToTheResult() throws IOException {
        Path overlay = write("overlay.yml", overlayRenaming("Renamed"));
        var output = yaml("asyncapi.yml", GENERATED);
        var processor = new TestProcessor();
        processor.overlayFiles = List.of(overlay.toString());
        processor.documentOrderer = document -> {
            var reordered = new LinkedHashMap<String, Object>();
            reordered.put("info", document.get("info"));
            reordered.put("asyncapi", document.get("asyncapi"));
            return reordered;
        };

        processor.process(filesWith(output));

        Assertions.assertTrue(output.getContent().indexOf("info:") < output.getContent().indexOf("asyncapi:"),
                output.getContent());
    }

    // ── which outputs are touched ─────────────────────────────────────────────

    @Test
    void onlyYamlOutputsWithContentAreProcessed() throws IOException {
        var yaml = yaml("asyncapi.yml", GENERATED);
        var java = new TemplateOutput("Service.java", GENERATED, OutputFormatType.JAVA.toString());
        var noMimeType = new TemplateOutput("unknown.txt", GENERATED);
        var noContent = yaml("deferred.yml", null);

        Path overlay = write("overlay.yml", overlayRenaming("Renamed"));
        var processor = new TestProcessor();
        processor.overlayFiles = List.of(overlay.toString());

        processor.process(filesWith(yaml, java, noMimeType, noContent));

        Assertions.assertTrue(yaml.getContent().contains("Renamed"));
        Assertions.assertEquals(GENERATED, java.getContent());
        Assertions.assertEquals(GENERATED, noMimeType.getContent());
        Assertions.assertNull(noContent.getContent());
    }

    @Test
    void mergedOutputsKeepTheirTargetFileMimeTypeAndSkipOverwriteFlag() throws IOException {
        var output = new TemplateOutput("apis/asyncapi.yml", GENERATED, OutputFormatType.YAML.toString(), true);
        Path overlay = write("overlay.yml", overlayRenaming("Renamed"));
        var processor = new TestProcessor();
        processor.overlayFiles = List.of(overlay.toString());

        processor.process(filesWith(output));

        Assertions.assertEquals("apis/asyncapi.yml", output.getTargetFile());
        Assertions.assertEquals(OutputFormatType.YAML.toString(), output.getMimeType());
        Assertions.assertTrue(output.isSkipOverwrite());
    }

    // ── failures ──────────────────────────────────────────────────────────────

    @Test
    void aFailingOverlayNamesTheFileItWasBeingAppliedTo() {
        var processor = new TestProcessor();
        processor.overlayFiles = List.of(tempDir.resolve("missing-overlay.yml").toString());

        IOException exception = Assertions.assertThrows(IOException.class,
                () -> processor.process(filesWith(yaml("apis/asyncapi.yml", GENERATED))));

        Assertions.assertEquals("Failed to apply merge or overlay resources to apis/asyncapi.yml", exception.getMessage());
        Assertions.assertNotNull(exception.getCause());
    }

    @Test
    void aFailingMergeFileNamesTheFileItWasBeingAppliedTo() {
        var processor = new TestProcessor();
        processor.mergeFile = tempDir.resolve("missing-merge.yml").toString();

        IOException exception = Assertions.assertThrows(IOException.class,
                () -> processor.process(filesWith(yaml("apis/asyncapi.yml", GENERATED))));

        Assertions.assertEquals("Failed to apply merge or overlay resources to apis/asyncapi.yml", exception.getMessage());
    }

    // ── project class loader ──────────────────────────────────────────────────

    @Test
    void resolvesOverlayResourcesFromTheProjectClassLoader() throws Exception {
        write("classpath-overlay.yml", overlayRenaming("Renamed from classpath"));
        var output = yaml("asyncapi.yml", GENERATED);

        try (var classLoader = new java.net.URLClassLoader(new java.net.URL[] { tempDir.toUri().toURL() }, null)) {
            var processor = new TestProcessor();
            processor.overlayFiles = List.of("classpath:classpath-overlay.yml");
            processor.withProjectClassLoader(classLoader).process(filesWith(output));
        }

        Assertions.assertTrue(output.getContent().contains("Renamed from classpath"), output.getContent());
    }

    private static String overlayRenaming(String title) {
        return """
                overlay: 1.0.0
                info:
                  title: Rename the API
                  version: 1.0.0
                actions:
                  - target: $.info
                    update:
                      title: %s
                """.formatted(title);
    }
}
