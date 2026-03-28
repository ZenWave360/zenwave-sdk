package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class MapperSupportTest {

    private Map<String, Object> loadZdl(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void collectsServiceParameterEntityPairsIncludingPatch() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var service = (Map<String, Object>) JSONPath.get(zdl, "$.services.CustomerService");

        var pairs = MapperSupport.serviceParameterEntityPairs(zdl, service, "Input");

        Assertions.assertTrue(pairs.containsKey("java.util.Map-Customer"));
        Assertions.assertTrue(pairs.values().stream().map(Map.class::cast).anyMatch(pair ->
                "Customer".equals(JSONPath.get(pair, "entity.className"))
                        && "CustomerInput".equals(JSONPath.get(pair, "input.className"))));
    }

    @Test
    void collectsEntityReturnTypePairsAndBuildsMapperCalls() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var service = (Map<String, Object>) JSONPath.get(zdl, "$.services.OrdersService");
        var method = (Map<String, Object>) JSONPath.get(zdl, "$.services.OrdersService.methods.searchOrders");
        method = new java.util.LinkedHashMap<>(method);
        method.put("serviceName", "OrdersService");
        var entity = (Map<String, Object>) JSONPath.get(zdl, "$.entities.CustomerOrder");
        var returnType = (Map<String, Object>) JSONPath.get(zdl, "$.allEntitiesAndEnums.CustomerOrderOutput");

        var pairs = MapperSupport.serviceEntityReturnTypePairs(zdl, service);

        Assertions.assertTrue(pairs.containsKey("CustomerOrder-CustomerOrderOutput"));
        Assertions.assertEquals(
                "ordersServiceMapper.asCustomerOrderOutputList(customerOrders)",
                MapperSupport.wrapWithMapper(method, entity, returnType));
    }

    @Test
    void returnsEntityInstanceNameWhenNoMappingNeeded() {
        var method = Map.<String, Object>of("serviceName", "CustomerService");
        var entity = Map.<String, Object>of("name", "Customer", "instanceName", "customer");
        var returnType = Map.<String, Object>of("name", "Customer");

        Assertions.assertEquals("customer", MapperSupport.wrapWithMapper(method, entity, returnType));
    }
}
