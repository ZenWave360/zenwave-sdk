package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        parser.apiOverlayFiles = java.util.List.of("does-not-exist-overlay.yml");

        Assertions.assertThrows(IOException.class, parser::parse);
    }

    @Test
    public void testParseWithWindowsStyleOverlayPath() throws Exception {
        Path overlayFile = Files.createTempFile("openapi-overlay-", ".yml");
        Files.writeString(overlayFile, """
                actions:
                  - target: $.info.title
                    update: Overlayed From Windows Path
                """, StandardCharsets.UTF_8);

        DefaultYamlParser parser = new DefaultYamlParser()
                .withApiFile(URI.create("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml"));
        parser.apiOverlayFiles = java.util.List.of(overlayFile.toString().replace("/", "\\"));

        Model model = (Model) parser.parse().get("api");

        Assertions.assertEquals("Overlayed From Windows Path", JSONPath.get(model, "$.info.title"));
    }
}
