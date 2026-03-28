package io.zenwave360.sdk.plugins.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CrudMethodSupportTest {

    @Test
    void matchesSingularCrudMethods() {
        var entity = Map.<String, Object>of("name", "Customer", "classNamePlural", "Customers");
        var method = Map.<String, Object>of("name", "getCustomer");

        Assertions.assertTrue(CrudMethodSupport.isCrudMethod("get", entity, method));
        Assertions.assertFalse(CrudMethodSupport.isCrudMethod("delete", entity, method));
    }

    @Test
    void matchesPluralCrudMethodsWhenReturnTypeIsArray() {
        var entity = Map.<String, Object>of("name", "Customer", "classNamePlural", "Customers");
        var method = Map.<String, Object>of("name", "listCustomers", "returnTypeIsArray", true);

        Assertions.assertTrue(CrudMethodSupport.isCrudMethod("list", entity, method));
        Assertions.assertFalse(CrudMethodSupport.isCrudMethod("list", entity, Map.of("name", "searchCustomers", "returnTypeIsArray", true)));
    }
}
