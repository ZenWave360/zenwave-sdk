package io.zenwave360.sdk.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.utils.JSONPath;

public class DefaultYamlParserTest {

    @Test
    public void testParseYml() throws URISyntaxException, IOException {
        String targetProperty = "_api";
        String resource = "classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml";
        DefaultYamlParser parser = new DefaultYamlParser().withApiFile(URI.create(resource)).withTargetProperty(targetProperty);
        Model model = (Model) parser.parse().get(targetProperty);
        Assertions.assertNotNull(model);
        Assertions.assertNotNull(JSONPath.get(model, "$.channels.createProductNotification.subscribe.message"));
    }

    @Test
    public void testParseWithMissingOverlayThrowsIOException() {
        DefaultYamlParser parser = new DefaultYamlParser()
                .withApiFile(URI.create("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml"));
        parser.apiOverlayFiles = List.of("does-not-exist-overlay.yml");

        Assertions.assertThrows(IOException.class, parser::parse);
    }

    @Test
    public void testParseWithFilesystemOverlayPath() throws Exception {
        Path overlayFile = Files.createTempFile("openapi-overlay-", ".yml");
        try {
            Files.writeString(overlayFile, """
                    actions:
                      - target: $.info.title
                        update: Overlayed From Filesystem Path
                    """, StandardCharsets.UTF_8);

            DefaultYamlParser parser = new DefaultYamlParser()
                    .withApiFile(URI.create("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml"));
            parser.apiOverlayFiles = List.of(toNativePathString(overlayFile));

            Model model = (Model) parser.parse().get("api");

            Assertions.assertEquals("Overlayed From Filesystem Path", JSONPath.get(model, "$.info.title"));
        } finally {
            Files.deleteIfExists(overlayFile);
        }
    }

    private String toNativePathString(Path path) {
        String normalized = path.toAbsolutePath().toString();
        if (File.separatorChar == '\\') {
            return normalized.replace('/', '\\');
        }
        return normalized;
    }
}
