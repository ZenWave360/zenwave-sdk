package io.zenwave360.sdk.generators;

import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.TestUtils;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.Model;

public class AbstractOpenAPIGeneratorTest {

    private AbstractOpenAPIGenerator newAbstractAsyncapiGenerator() {
        return new AbstractOpenAPIGenerator() {
            @Override
            public GeneratedProjectFiles generate(Map<String, Object> apiModel) {
                return null;
            }
        };
    }

    @Test
    public void test_filter_operations_by_tag_and_verb() throws Exception {
        Model model = TestUtils.loadYmlModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        AbstractOpenAPIGenerator openapiGenerator = newAbstractAsyncapiGenerator();
        Map<String, List<Map<String, Object>>> allOperations = openapiGenerator.getOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> getOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.GET);
        Map<String, List<Map<String, Object>>> putOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.PUT);
        Map<String, List<Map<String, Object>>> headOperations = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.HEAD);
        Map<String, List<Map<String, Object>>> parametersInPath = openapiGenerator.getOperationsGroupedByTag(model, AbstractOpenAPIGenerator.OperationType.HEAD);
        Assertions.assertEquals(9, allOperations.get("Pet").size());
        Assertions.assertEquals(3, getOperations.get("Pet").size());
        Assertions.assertEquals(1, putOperations.get("Pet").size());
        Assertions.assertEquals("findPetsByStatus", getOperations.get("Pet").get(0).get("operationId"));
        Assertions.assertTrue(headOperations.isEmpty());
        Assertions.assertTrue(parametersInPath.isEmpty());
    }

    @Test
    public void test_getOperationsByOperationIds_Pets() throws Exception {
        Model model = TestUtils.loadYmlModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        AbstractOpenAPIGenerator openapiGenerator = newAbstractAsyncapiGenerator();
        var operationIds = List.of("addPet", "getPetById", "updatePet", "deletePet", "getPetById");
        List<Map<String, Object>> operations = openapiGenerator.getOperationsByOperationIds(model, operationIds);

        Assertions.assertEquals(5, operations.size());
        Assertions.assertEquals("addPet", operations.get(0).get("operationId"));
        Assertions.assertEquals("getPetById", operations.get(1).get("operationId"));
        Assertions.assertEquals("updatePet", operations.get(2).get("operationId"));
        Assertions.assertEquals("deletePet", operations.get(3).get("operationId"));
        Assertions.assertEquals("getPetById", operations.get(4).get("operationId"));
    }

    @Test
    public void test_getOperationsByOperationIds_Orders() throws Exception {
        Model model = TestUtils.loadYmlModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-orders.yml");
        AbstractOpenAPIGenerator openapiGenerator = newAbstractAsyncapiGenerator();
        var operationIds = List.of("createCustomer", "getCustomer", "updateCustomer", "deleteCustomer", "getCustomer");
        List<Map<String, Object>> operations = openapiGenerator.getOperationsByOperationIds(model, operationIds);

        Assertions.assertEquals(5, operations.size());
        Assertions.assertEquals("createCustomer", operations.get(0).get("operationId"));
        Assertions.assertEquals("getCustomer", operations.get(1).get("operationId"));
        Assertions.assertEquals("updateCustomer", operations.get(2).get("operationId"));
        Assertions.assertEquals("deleteCustomer", operations.get(3).get("operationId"));
        Assertions.assertEquals("getCustomer", operations.get(4).get("operationId"));
    }

}
