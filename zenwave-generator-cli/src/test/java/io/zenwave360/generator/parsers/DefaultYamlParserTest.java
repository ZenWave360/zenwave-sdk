package io.zenwave360.generator.parsers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class DefaultYamlParserTest {

    private File getClasspathResourceAsFile(String resource) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(resource).toURI());
    }

    @Test
    public void testParseYml() throws URISyntaxException, IOException {
        String targetProperty = "_api";
        File file = getClasspathResourceAsFile("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml");
        DefaultYamlParser parser = new DefaultYamlParser(file.getAbsolutePath(), targetProperty);
        Model model = parser.parse().get(targetProperty);
        Assertions.assertNotNull(model);
        Assertions.assertNotNull(model.getJsonPath("$.channels.createProductNotification.subscribe.message"));
    }
}
