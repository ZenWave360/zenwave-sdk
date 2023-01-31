package io.zenwave360.generator.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.utils.JSONPath;

public class DefaultYamlParserTest {

    @Test
    public void testParseYml() throws URISyntaxException, IOException {
        String targetProperty = "_api";
        String resource = "classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml";
        DefaultYamlParser parser = new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty(targetProperty);
        Model model = (Model) parser.parse().get(targetProperty);
        Assertions.assertNotNull(model);
        Assertions.assertNotNull(JSONPath.get(model, "$.channels.createProductNotification.subscribe.message"));
    }
}
