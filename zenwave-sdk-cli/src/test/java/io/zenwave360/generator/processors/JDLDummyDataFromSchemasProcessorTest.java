package io.zenwave360.generator.processors;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.utils.JSONPath;

public class JDLDummyDataFromSchemasProcessorTest {

    private Map<String, Object> loadOpenApi(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty("api").parse();
        return new OpenApiProcessor().process(model);
    }

    @Test
    public void testProcessJDLWithOpenAPI() throws Exception {
        var openapiModel = loadOpenApi("classpath:io/zenwave360/generator/resources/openapi/oas-controllers-with-no-jdl.yml");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);

        var processor = new JDLDummyDataFromSchemasProcessor();
        var processed = processor.process(model);
        Assertions.assertNotNull(processed.get(processor.jdlProperty));
        Map jdlModel = (Map) processed.get(processor.jdlProperty);
        Assertions.assertNotNull(JSONPath.get(jdlModel, "$.entities"));
        Assertions.assertNotNull(JSONPath.get(jdlModel, "$.entities.Pet"));
        Assertions.assertEquals("PetService", JSONPath.get(jdlModel, "$.entities.Pet.options.service"));
    }

}
