package io.zenwave360.sdk.zdl.utils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;

public class ZDLHttpUtilsTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    private Map<String, Object> loadOpenAPI(String resource) throws IOException {
        Map<String, Object> model = new DefaultYamlParser().withApiFile(URI.create(resource)).parse();
        return (Map<String, Object>) new OpenApiProcessor().process(model).get("api");
    }

    @Test
    void testFindMethodParams() throws IOException {
        var model = loadOpenAPI("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml");
        var operation = (Map) JSONPath.get(model, "$.paths./customers/customers/{customerId}.put");

        var parameters = ZDLHttpUtils.methodParameters(operation, "", "DTO");
        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertEquals("String", parameters.get(0).getKey());
        Assertions.assertEquals("CustomerInputDTO", parameters.get(1).getKey());
    }

    @Test
    void testFindMethodParamsMultipart() throws IOException {
        var model = loadOpenAPI("classpath:io/zenwave360/sdk/resources/openapi/documents-openapi.yml");
        var operation = (Map) JSONPath.get(model, "$.paths./documents/upload.post");

        var parameters = ZDLHttpUtils.methodParameters(operation, "", "DTO");
        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(9, parameters.size());
        Assertions.assertEquals("org.springframework.web.multipart.MultipartFile", parameters.get(0).getKey());
        Assertions.assertEquals("file", parameters.get(0).getValue());
        Assertions.assertEquals("Long", parameters.get(1).getKey());
        Assertions.assertEquals("id", parameters.get(1).getValue());
    }

    @Test
    void testFindMethodParamsPatch() throws IOException {
        var model = loadOpenAPI("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        var operation = (Map) JSONPath.get(model, "$.paths./pet.patch");

        var parameters = ZDLHttpUtils.methodParameters(operation, "", "DTO");
        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(1, parameters.size());
        Assertions.assertEquals("Map", parameters.get(0).getKey());
        Assertions.assertEquals("input", parameters.get(0).getValue());
    }


    @Test
    void testFindPathParams() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.updateCustomer");

        var pathParam = ZDLHttpUtils.getFirstPathParamsFromMethod(method);
        Assertions.assertEquals("customerId", pathParam);
    }

    @Test
    void getRequestBodyType() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.updateCustomer");

        var pathParam = ZDLHttpUtils.getRequestBodyType(method, model);
        Assertions.assertEquals("CustomerInput", pathParam);
    }

    @Test
    void getRequestPathParamsAsObject() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.updateCustomer");
        String idType = "string";
        String idTypeFormat = null;
        Map naturalIdTypes = new HashMap();
        var pathParamsMap = ZDLHttpUtils.getPathParamsAsObject(model, method, naturalIdTypes, idType, idTypeFormat);
        Assertions.assertEquals("customerId", pathParamsMap.get(0).get("name"));
    }

    @Test
    void getRequestQueryParamsAsObject() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.listCustomers");
        var queryParamsMap = ZDLHttpUtils.getQueryParamsAsObject(method, model);
        Assertions.assertEquals("search", queryParamsMap.get(0).get("name"));
        Assertions.assertEquals("string", queryParamsMap.get(0).get("type"));
    }

    @Test
    void getRequestQueryParamsAsObjectWithParamOptions() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.searchCustomers");
        var queryParamsMap = ZDLHttpUtils.getQueryParamsAsObject(method, model);
        Assertions.assertEquals("name", queryParamsMap.get(0).get("name"));
        Assertions.assertEquals("email", queryParamsMap.get(1).get("name"));
        Assertions.assertEquals("city", queryParamsMap.get(2).get("name"));
        Assertions.assertEquals("state", queryParamsMap.get(3).get("name"));
    }



    @Test
    void getRequestBodyTypeInline() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.services.CustomerService.methods.addCustomerAddress");

        var pathParam = ZDLHttpUtils.getRequestBodyType(method, model);
        Assertions.assertEquals("Address", pathParam);
    }

    @Test
    void testGetJavaType() {
        // Test binary format
        Map binaryParam = Map.of("schema", Map.of("type", "string", "format", "binary"));
        Assertions.assertEquals("org.springframework.web.multipart.MultipartFile",
            ZDLHttpUtils.getJavaType(binaryParam, "", ""));

        // Test date format
        Map dateParam = Map.of("schema", Map.of("type", "string", "format", "date"));
        Assertions.assertEquals("LocalDate", ZDLHttpUtils.getJavaType(dateParam, "", ""));

        // Test date-time format
        Map dateTimeParam = Map.of("schema", Map.of("type", "string", "format", "date-time"));
        Assertions.assertEquals("Instant", ZDLHttpUtils.getJavaType(dateTimeParam, "", ""));

        // Test integer with int64 format
        Map longParam = Map.of("schema", Map.of("type", "integer", "format", "int64"));
        Assertions.assertEquals("Long", ZDLHttpUtils.getJavaType(longParam, "", ""));

        // Test integer without format
        Map intParam = Map.of("schema", Map.of("type", "integer"));
        Assertions.assertEquals("Integer", ZDLHttpUtils.getJavaType(intParam, "", ""));

        // Test number type
        Map numberParam = Map.of("schema", Map.of("type", "number"));
        Assertions.assertEquals("BigDecimal", ZDLHttpUtils.getJavaType(numberParam, "", ""));

        // Test boolean type
        Map booleanParam = Map.of("schema", Map.of("type", "boolean"));
        Assertions.assertEquals("Boolean", ZDLHttpUtils.getJavaType(booleanParam, "", ""));

        // Test array type
        Map arrayParam = Map.of("schema", Map.of("type", "array", "items", Map.of("type", "string")));
        Assertions.assertEquals("List<String>", ZDLHttpUtils.getJavaType(arrayParam, "", ""));

        // Test schema name with prefixes/suffixes
        Map schemaParam = Map.of("schema", Map.of("x--schema-name", "Customer"));
        Assertions.assertEquals("CustomerDTO", ZDLHttpUtils.getJavaType(schemaParam, "", "DTO"));

        // Test default string type
        Map stringParam = Map.of("schema", Map.of("type", "string"));
        Assertions.assertEquals("String", ZDLHttpUtils.getJavaType(stringParam, "", ""));
    }

}
