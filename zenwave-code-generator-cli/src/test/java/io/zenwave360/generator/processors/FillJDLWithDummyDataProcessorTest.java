package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.JDL_DEFAULT_PROPERTY;
import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.OPENAPI_DEFAULT_PROPERTY;

public class FillJDLWithDummyDataProcessorTest {

    private Map<String, Object> loadOpenApi(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(resource).withTargetProperty(OPENAPI_DEFAULT_PROPERTY).parse();
        return new OpenApiProcessor().withTargetProperty(OPENAPI_DEFAULT_PROPERTY).process(model);
    }

    @Test
    public void testProcessJDLWithOpenAPI() throws Exception {
        var openapiModel = loadOpenApi("classpath:io/zenwave360/generator/resources/openapi/oas-controllers-with-no-jdl.yml");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);

        var processor = new FillJDLWithDummyDataProcessor();
        var processed = processor.process(model);
        Assertions.assertNotNull(processed.get(processor.jdlProperty));
        Map jdlModel = (Map) processed.get(processor.jdlProperty);
        Assertions.assertNotNull(JSONPath.get(jdlModel, "$.entities"));
        Assertions.assertNotNull(JSONPath.get(jdlModel, "$.entities.Pet"));
        Assertions.assertEquals("PetService", JSONPath.get(jdlModel, "$.entities.Pet.options.service"));
    }

}
