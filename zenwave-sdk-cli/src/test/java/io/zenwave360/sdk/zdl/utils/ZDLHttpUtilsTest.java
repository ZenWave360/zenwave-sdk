package io.zenwave360.sdk.zdl.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        var pathParamsMap = ZDLHttpUtils.getPathParamsAsObject(method, naturalIdTypes, idType, idTypeFormat);
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

}
