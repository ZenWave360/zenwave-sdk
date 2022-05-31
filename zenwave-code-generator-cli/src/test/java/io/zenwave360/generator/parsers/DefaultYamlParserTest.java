package io.zenwave360.generator.parsers;

import io.zenwave360.generator.processors.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class DefaultYamlParserTest {

    private File getClasspathResourceAsFile(String resource) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(resource).toURI());
    }

    @Test
    public void testParseYml() throws URISyntaxException, IOException {
        String targetProperty = "_api";
        File file = getClasspathResourceAsFile("io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
        DefaultYamlParser parser = new DefaultYamlParser().withSpecFile(file.getAbsolutePath()).withTargetProperty(targetProperty);
        Model model = (Model) parser.parse().get(targetProperty);
        Assertions.assertNotNull(model);
        Assertions.assertNotNull(JSONPath.get(model,"$.channels.createProductNotification.subscribe.message"));
    }
}
