package io.zenwave360.generator.processors;

import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.JDL_DEFAULT_PROPERTY;
import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.OPENAPI_DEFAULT_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.utils.JSONPath;

public class JDLWithOpenApiProcessorTest {

    private Map<String, Object> loadOpenApi(File file) throws Exception {
        return loadOpenApi(file.getAbsoluteFile().toURI().toString());
    }
    private Map<String, Object> loadJDL(File file) throws Exception {
        return loadJDL(file.getAbsolutePath());
    }
    private Map<String, Object> loadOpenApi(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty(OPENAPI_DEFAULT_PROPERTY).parse();
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
        List requestEntities = JSONPath.get(processed, "$..[?(@.x--request-entity)]");
        Assertions.assertFalse(requestEntities.isEmpty());
        List responseEntities = JSONPath.get(processed, "$..[?(@.x--response-entity)]");
        Assertions.assertFalse(responseEntities.isEmpty());
    }

    @Test
    // @Disabled
    public void testProcessJDLWithOpenAPI_registry() throws Exception {
        var openapiModel = loadOpenApi("classpath:io/zenwave360/generator/resources/openapi/openapi-orders.yml");
        var jdlModel = loadJDL("classpath:io/zenwave360/generator/resources/jdl/orders-model.jdl");
        var model = new HashMap<String, Object>();
        model.putAll(openapiModel);
        model.putAll(jdlModel);

        var processed = new JDLWithOpenApiProcessor().process(model);
        List requestEntities = JSONPath.get(processed, "$..[?(@.x--request-entity)]");
        Assertions.assertFalse(requestEntities.isEmpty());
        List responseEntities = JSONPath.get(processed, "$..[?(@.x--response-entity)]");
        Assertions.assertFalse(responseEntities.isEmpty());
    }
}
