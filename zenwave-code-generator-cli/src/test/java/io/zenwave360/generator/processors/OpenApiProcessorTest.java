package io.zenwave360.generator.processors;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenApiProcessorTest {

    String targetProperty = "_api";
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    private Map<String, Model> loadOpenAPIModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        return new DefaultYamlParser().withSpecFile(file.getAbsolutePath()).withTargetProperty(targetProperty).parse();
    }

    @Test
    public void testProcessOpenAPI() throws Exception {
        Map<String, Model> model = loadOpenAPIModelFromResource("io/zenwave360/generator/parsers/openapi-petstore.yml");
        OpenApiProcessor processor = new OpenApiProcessor().withTargetProperty(targetProperty);;
        Model processed = (Model) processor.process(model).get(targetProperty);
        List httpVerbs = processed.getJsonPath("$.paths..x--httpVerb");
        Assertions.assertFalse(httpVerbs.isEmpty());

        List<Map<String, Object>> pathItems = processed.getJsonPath("$.paths[*][*]");
        for (Map<String, Object> pathItem : pathItems) {
            Assertions.assertTrue(pathItem.containsKey("x--httpVerb"));
            Assertions.assertTrue(pathItem.containsKey("x--path"));
        }
    }


}
