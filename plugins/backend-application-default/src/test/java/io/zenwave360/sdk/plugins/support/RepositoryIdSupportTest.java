package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RepositoryIdSupportTest {

    private Map<String, Object> loadZdl(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void usesNaturalIdRepositoryCallWhenMethodDeclaresNaturalId() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var method = (Map<String, Object>) JSONPath.get(zdl, "$.services.CustomerService.methods.getCustomer");

        Assertions.assertEquals(
                "findByCustomerIdAndAnotherId(customerId, anotherId)",
                RepositoryIdSupport.findById(zdl, method));
        Assertions.assertEquals(
                "customerId, anotherId",
                RepositoryIdSupport.idParamsCallSignature(zdl, method));
    }

    @Test
    void initializesNaturalIdFieldsFromEntityDefinition() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var method = (Map<String, Object>) JSONPath.get(zdl, "$.services.CustomerService.methods.updateCustomer");

        var initialization = RepositoryIdSupport.idFieldInitialization(zdl, method, "Long");

        Assertions.assertTrue(initialization.contains("var customerId = "));
        Assertions.assertTrue(initialization.contains("var anotherId = "));
        Assertions.assertFalse(initialization.contains("Long id = null;"));
    }

    @Test
    void fallsBackToSingleIdWhenMethodHasNoNaturalId() {
        var zdl = Map.<String, Object>of();
        var method = Map.<String, Object>of("name", "getCustomer");

        Assertions.assertEquals("findById(id)", RepositoryIdSupport.findById(zdl, method));
        Assertions.assertEquals("id", RepositoryIdSupport.idParamsCallSignature(zdl, method));
        Assertions.assertEquals("String id = null;", RepositoryIdSupport.idFieldInitialization(zdl, method, "String"));
    }
}
