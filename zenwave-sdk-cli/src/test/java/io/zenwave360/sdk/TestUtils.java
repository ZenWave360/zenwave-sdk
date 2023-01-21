package io.zenwave360.sdk;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.OpenApiProcessor;

import java.net.URI;
import java.util.Map;

public class TestUtils {

    public static Model loadYmlModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).parse();
        return (Model) new OpenApiProcessor().process(model).get("api");
    }
}
