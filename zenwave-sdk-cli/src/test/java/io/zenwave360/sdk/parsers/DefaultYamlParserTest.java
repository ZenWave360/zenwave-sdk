package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
}
