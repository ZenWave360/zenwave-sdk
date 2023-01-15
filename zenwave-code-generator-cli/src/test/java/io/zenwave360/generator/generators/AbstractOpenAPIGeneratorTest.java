package io.zenwave360.generator.generators;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;

public class AbstractOpenAPIGeneratorTest {

    private Model loadAsyncapiModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).parse();
        return (Model) new OpenApiProcessor().process(model).get("api");
    }

    private AbstractOpenAPIGenerator newAbstractAsyncapiGenerator() {
        return new AbstractOpenAPIGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, Object> apiModel) {
                return null;
            }
        };
    }

    @Test
    public void test_filter_operations_by_tag_and_verb() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        AbstractOpenAPIGenerator openapiGenerator = newAbstractAsyncapiGenerator();
        Map<String, List<Map<String, Object>>> allOperations = openapiGenerator.getOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> getOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.GET);
        Map<String, List<Map<String, Object>>> putOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.PUT);
        Map<String, List<Map<String, Object>>> headOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.HEAD);
        Map<String, List<Map<String, Object>>> parametersInPath = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.HEAD);
        Assertions.assertEquals(8, allOperations.get("Pet").size());
        Assertions.assertEquals(3, getOperations.get("Pet").size());
        Assertions.assertEquals(1, putOperations.get("Pet").size());
        Assertions.assertEquals("findPetsByStatus", getOperations.get("Pet").get(0).get("operationId"));
        Assertions.assertTrue(headOperations.isEmpty());
        Assertions.assertTrue(parametersInPath.isEmpty());
    }

}
