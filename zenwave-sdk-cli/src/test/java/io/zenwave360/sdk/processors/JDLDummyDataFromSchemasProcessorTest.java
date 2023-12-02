package io.zenwave360.sdk.processors;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.zenwave360.sdk.zdl.ZDLFindUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLDummyDataFromSchemasProcessorTest {

    private Map<String, Object> loadOpenApi(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty("api").parse();
        return new OpenApiProcessor().process(model);
    }

    @Test
    public void testProcessJDLWithOpenAPI() throws Exception {
        var openapiModel = loadOpenApi("classpath:io/zenwave360/sdk/resources/openapi/oas-controllers-with-no-jdl.yml");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);

        var processor = new JDLDummyDataFromSchemasProcessor();
        var processed = processor.process(model);
        Assertions.assertNotNull(processed.get(processor.jdlProperty));
        Map zdlModel = (Map) processed.get(processor.jdlProperty);
        Assertions.assertNotNull(JSONPath.get(zdlModel, "$.entities"));
        Assertions.assertNotNull(JSONPath.get(zdlModel, "$.entities.Pet"));
        Assertions.assertEquals("PetService", ZDLFindUtils.findServiceName("Pet", zdlModel));
    }

}
