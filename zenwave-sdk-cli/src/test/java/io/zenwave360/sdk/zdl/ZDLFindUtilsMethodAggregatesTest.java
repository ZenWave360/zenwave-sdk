package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.zenwave360.sdk.zdl.ZDLFindUtilsTest.loadZDL;

public class ZDLFindUtilsMethodAggregatesTest {

    @Test
    public void should_return_single_service_entity() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForEntity", "someMethod");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate", aggregatesMapForMethod);
        assertEquals("MyEntity", "$[0].entity.name", aggregatesMapForMethod);
    }

    @Test
    public void should_return_single_service_entity_crud() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForEntity", "createMyEntity");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate", aggregatesMapForMethod);
        assertEquals("MyEntity", "$[0].entity.name", aggregatesMapForMethod);
        assertEquals("createMyEntity", "$[0].crudMethod", aggregatesMapForMethod);
    }

    @Test
    public void should_return_command_returnType() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForEntities", "shouldResolveByReturnType");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate", aggregatesMapForMethod);
        assertEquals("MyEntity2", "$[0].entity.name", aggregatesMapForMethod);
    }

    @Test
    public void should_return_command_returnType_entity_crud() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForAggregate", "createMyEntity");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate", aggregatesMapForMethod);
        assertEquals("MyEntity", "$[0].entity.name", aggregatesMapForMethod);
        assertEquals("createMyEntity", "$[0].crudMethod", aggregatesMapForMethod);
    }

    @Test
    public void should_return_command_returnType_aggregate_crud() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForAggregate", "createMyAggregate");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate.name", aggregatesMapForMethod);
        assertEquals(null, "$[0].crudMethod", aggregatesMapForMethod);
        assertEquals("MyEntity", "$[0].entity.name", aggregatesMapForMethod);
    }

    @Test
    public void should_return_command_returnType_aggregate() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForAggregates", "shouldResolveByReturnType");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals(null, "$[0].aggregate", aggregatesMapForMethod);
        assertEquals("MyEntity2", "$[0].entity.name", aggregatesMapForMethod);
    }

    @Test
    public void should_return_command_returnType_aggregate_with_command() throws Exception {
        var aggregatesMapForMethod = aggregateCommandsForMethod("MyServiceForAggregates", "shouldResolveByReturnTypeWithCommand");

        Assertions.assertEquals(1, aggregatesMapForMethod.size());
        assertEquals("MyAggregate2", "$[0].aggregate.name", aggregatesMapForMethod);
        assertEquals("aggregates", "$[0].aggregate.type", aggregatesMapForMethod);
        assertEquals("shouldResolveByReturnTypeWithCommand", "$[0].command.name", aggregatesMapForMethod);
    }


    List<Map<String, Object>> aggregateCommandsForMethod(String service, String methodName) throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/zdl/aggregate-service-methods.zdl");

        var jsonPath = String.format("$.services.%s.methods.%s", service, methodName);
        var createMyEntityMethod = JSONPath.get(model, jsonPath, Map.<String, Object>of());

        var aggregatesMapForMethod = ZDLFindUtils.findAggregateCommandsForMethod(model, createMyEntityMethod);
        Assertions.assertNotNull(aggregatesMapForMethod);
        return aggregatesMapForMethod;
    }

    void assertEquals(Object expected, String jsonpath, Object object) {
        Assertions.assertEquals(expected, JSONPath.get(object, jsonpath));
    }
}
