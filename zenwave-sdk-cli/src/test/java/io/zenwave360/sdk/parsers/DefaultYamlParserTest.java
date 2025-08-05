package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
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
    public void testParseAuthYml() throws Exception {
        Plugin testPlugin = new Plugin()
                .withChain(DefaultYamlParser.class)
                .withApiFile("https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/refs/heads/main/zenwave-sdk-test-resources/src/main/resources/io/zenwave360/sdk/resources/asyncapi/v3/customer-address.yml")
                .withOption("authApiKeysByHostMap", "{ '*.githubusercontent.com': 'Authentication: Bearer XXXX', '*.githubusercontent.com': 'Authentication2: Other XXXX', localhost2:'Authentication: Bearer YYYYY' }");

        new MainGenerator().generate(testPlugin);
    }
}
