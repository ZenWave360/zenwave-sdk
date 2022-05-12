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
        File file = getClasspathResourceAsFile("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml");
        Model model = new DefaultYamlParser().parse(file);
        Assertions.assertNotNull(model.model());
        Assertions.assertNotNull(model.getJsonPath("$.channels.createProductNotification.subscribe.message"));
    }
}
