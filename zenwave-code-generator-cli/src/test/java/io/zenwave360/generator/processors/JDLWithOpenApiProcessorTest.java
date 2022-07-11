package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.JDL_DEFAULT_PROPERTY;
import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.OPENAPI_DEFAULT_PROPERTY;

public class JDLWithOpenApiProcessorTest {

    private Map<String, Object> loadOpenApi(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(resource).withTargetProperty(OPENAPI_DEFAULT_PROPERTY).parse();
        return new OpenApiProcessor().withTargetProperty(OPENAPI_DEFAULT_PROPERTY).process(model);
    }

    private Map<String, Object> loadJDL(String resource) throws IOException {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).withTargetProperty(JDL_DEFAULT_PROPERTY).parse();
        return new JDLProcessor().withTargetProperty(JDL_DEFAULT_PROPERTY).process(model);
    }

    @Test
    public void testProcessJDLWithOpenAPI() throws Exception {
        var openapiModel = loadOpenApi("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        var jdlModel = loadJDL("classpath:io/zenwave360/generator/resources/jdl/petstore.jdl");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);
        model.putAll(jdlModel);

        var processed = new JDLWithOpenApiProcessor().process(model);
        List requestEntities = JSONPath.get(processed,"$..[?(@.x--request-entity)]");
        Assertions.assertFalse(requestEntities.isEmpty());
        List responseEntities = JSONPath.get(processed,"$..[?(@.x--response-entity)]");
        Assertions.assertFalse(responseEntities.isEmpty());
    }

    @Test
    @Disabled
    public void testProcessJDLWithOpenAPI_registry() throws Exception {
        var openapiModel = loadOpenApi("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\openapi.yml");
        var jdlModel = loadJDL("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\api-registry.jdl");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);
        model.putAll(jdlModel);

        var processed = new JDLWithOpenApiProcessor().process(model);
        List requestEntities = JSONPath.get(processed,"$..[?(@.x--request-entity)]");
        Assertions.assertFalse(requestEntities.isEmpty());
        List responseEntities = JSONPath.get(processed,"$..[?(@.x--response-entity)]");
        Assertions.assertFalse(responseEntities.isEmpty());
    }
}
