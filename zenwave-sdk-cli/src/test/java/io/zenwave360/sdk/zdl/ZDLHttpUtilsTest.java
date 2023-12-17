package io.zenwave360.sdk.zdl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;

public class ZDLHttpUtilsTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    void testFindPathParams() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = (Map) JSONPath.get(model, "$.zdl.services.CustomerService.methods.updateCustomer");

        var pathParam = ZDLHttpUtils.getFirstPathParamsFromMethod(method);
        Assertions.assertEquals("customerId", pathParam);
    }

}
