package io.zenwave360.generator.processors;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.utils.JSONPath;

public class OpenApiProcessorTest {

    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    private Map<String, Object> loadOpenAPIModelFromResource(String resource) throws Exception {
        return new DefaultYamlParser().withSpecFile(URI.create(resource)).parse();
    }

    @Test
    public void testProcessOpenAPI() throws Exception {
        Map<String, Object> model = loadOpenAPIModelFromResource("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        OpenApiProcessor processor = new OpenApiProcessor();;
        Model processed = (Model) processor.process(model).get("api");
        List httpVerbs = JSONPath.get(processed, "$.paths..x--httpVerb");
        Assertions.assertFalse(httpVerbs.isEmpty());

        List<Map<String, Object>> pathItems = JSONPath.get(processed, "$.paths[*][*]");
        for (Map<String, Object> pathItem : pathItems) {
            Assertions.assertTrue(pathItem.containsKey("x--httpVerb"));
            Assertions.assertTrue(pathItem.containsKey("x--path"));
        }
    }

}
