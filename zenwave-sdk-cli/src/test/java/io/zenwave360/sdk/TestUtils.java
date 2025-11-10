package io.zenwave360.sdk;

import java.net.URI;
import java.util.Map;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.processors.OpenApiProcessor;

public class TestUtils {

    public static Model loadYmlModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withApiFile(URI.create(resource)).parse();
        return (Model) new OpenApiProcessor().process(model).get("api");
    }

    public static Model loadAsyncApiYmlModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withApiFile(URI.create(resource)).parse();
        return (Model) new AsyncApiProcessor().process(model).get("api");
    }
}
