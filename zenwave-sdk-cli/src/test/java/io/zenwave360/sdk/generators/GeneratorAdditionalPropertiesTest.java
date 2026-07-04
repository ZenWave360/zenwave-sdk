package io.zenwave360.sdk.generators;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GeneratorAdditionalPropertiesTest {

    @Test
    void unknown_options_are_captured_flattened_and_aliased() throws Exception {
        var source = new LinkedHashMap<String, Object>();
        source.put("sourceProperty", "zdl"); // known field -> bound normally, not swept
        source.put("id", "urn:example:orders"); // scalar pass-through
        source.put("x-server-id", Map.of("host", "localhost", "port", 9092)); // object pass-through

        var generator = new ZDLProjectGenerator();
        new ObjectMapper().updateValue(generator, source);

        // known field is bound to the field, not captured as an additional property
        Assertions.assertEquals("zdl", generator.sourceProperty);
        Assertions.assertFalse(generator.additionalProperties.containsKey("sourceProperty"));

        // everything unknown is captured by the single inherited @JsonAnySetter
        Assertions.assertEquals("urn:example:orders", generator.additionalProperties.get("id"));
        Assertions.assertEquals(Map.of("host", "localhost", "port", 9092), generator.additionalProperties.get("x-server-id"));

        // and flattened to the top level of the template model ({{id}} / {{[x-server-id]}})
        var model = generator.asConfigurationMap();
        Assertions.assertEquals("urn:example:orders", model.get("id"));
        Assertions.assertEquals(Map.of("host", "localhost", "port", 9092), model.get("x-server-id"));

        // backwards-compatible alias: 'options' is the very same map and is still exposed nested as {{options.*}}
        Assertions.assertSame(generator.additionalProperties, generator.options);
        Assertions.assertEquals("urn:example:orders", ((Map<?, ?>) model.get("options")).get("id"));
    }

    @Test
    void additional_properties_do_not_override_declared_generator_properties() {
        var generator = new ZDLProjectGenerator();
        generator.sourceProperty = "declared-value";
        generator.additionalProperties.put("sourceProperty", "additional-value");

        var model = generator.asConfigurationMap();

        Assertions.assertEquals("declared-value", model.get("sourceProperty"));
    }
}
