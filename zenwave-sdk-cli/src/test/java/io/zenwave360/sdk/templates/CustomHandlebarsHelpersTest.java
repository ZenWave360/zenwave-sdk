package io.zenwave360.sdk.templates;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.TestUtils;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.templating.CustomHandlebarsHelpers;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class CustomHandlebarsHelpersTest {

    @Test
    void populatePropertyTest() throws Exception {
        Model model = TestUtils.loadYmlModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        var schemas = JSONPath.get(model, "components.schemas.[*].properties", List.<Map<String, Map>>of());
        for (var properties : schemas) {
            for (var entry : properties.entrySet()) {
                Options options = new Options.Builder(null, null, null, null, null)
                        .setHash(Map.of("openApiModelNameSuffix", "")).build();
                var propertyInitialized = CustomHandlebarsHelpers.populateProperty(entry.getValue(), options);
                System.out.println(entry.getKey() + " = " + propertyInitialized + "; // " + JSONPath.get(entry.getValue(), "$.['type', 'format']"));
            }
        }
    }
}
