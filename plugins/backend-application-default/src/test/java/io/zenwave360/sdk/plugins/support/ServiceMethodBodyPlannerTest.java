package io.zenwave360.sdk.plugins.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ServiceMethodBodyPlannerTest {

    @Test
    void planEntityMethodBody_classifiesLifecycleUpdateBeforeDelete() {
        var method = Map.<String, Object>of(
                "paramId", "id",
                "parameter", "OrderInput",
                "returnType", "Order",
                "options", Map.of("delete", true)
        );
        var entity = Map.<String, Object>of(
                "name", "Order",
                "lifecycle", Map.of("field", "status")
        );

        var plan = ServiceMethodBodyPlanner.planEntityMethodBody(method, entity, entity);

        Assertions.assertEquals(MethodBodyCase.ENTITY_LIFECYCLE_UPDATE_REQUIRED, plan.bodyCase());
        Assertions.assertTrue(plan.lifecycleManaged());
    }

    @Test
    void planEntityMethodBody_classifiesPatchOptionalSeparately() {
        var method = Map.<String, Object>of(
                "paramId", "id",
                "parameter", "PatchMap",
                "returnType", "CustomerOutput",
                "returnTypeIsOptional", true,
                "options", Map.of("patch", true)
        );
        var entity = Map.<String, Object>of("name", "Customer");
        var returnEntity = Map.<String, Object>of("name", "CustomerOutput");

        var plan = ServiceMethodBodyPlanner.planEntityMethodBody(method, entity, returnEntity);

        Assertions.assertEquals(MethodBodyCase.ENTITY_PATCH_OPTIONAL, plan.bodyCase());
        Assertions.assertTrue(plan.requiresMapping());
    }

    @Test
    void planEntityMethodBody_classifiesLifecycleTransitionsWithoutInput() {
        var method = Map.<String, Object>of(
                "paramId", "id",
                "returnType", "Order",
                "transition", Map.of(
                        "from", List.of("PLACED"),
                        "to", "CONFIRMED"
                )
        );
        var entity = Map.<String, Object>of(
                "name", "Order",
                "lifecycle", Map.of("field", "status")
        );

        var plan = ServiceMethodBodyPlanner.planEntityMethodBody(method, entity, entity);

        Assertions.assertEquals(MethodBodyCase.ENTITY_TRANSITION_REQUIRED, plan.bodyCase());
        Assertions.assertTrue(plan.transitionDriven());
    }

    @Test
    void planEntityMethodBody_classifiesOptionalGetAndArrayListCases() {
        var optionalGet = ServiceMethodBodyPlanner.planEntityMethodBody(
                Map.<String, Object>of("paramId", "id", "returnType", "Customer", "returnTypeIsOptional", true),
                Map.<String, Object>of("name", "Customer"),
                Map.<String, Object>of("name", "Customer"));
        var list = ServiceMethodBodyPlanner.planEntityMethodBody(
                Map.<String, Object>of("returnType", "CustomerOutput", "returnTypeIsArray", true, "options", Map.of("paginated", true)),
                Map.<String, Object>of("name", "Customer"),
                Map.<String, Object>of("name", "CustomerOutput"));

        Assertions.assertEquals(MethodBodyCase.ENTITY_GET_OPTIONAL, optionalGet.bodyCase());
        Assertions.assertEquals(MethodBodyCase.ENTITY_LIST, list.bodyCase());
        Assertions.assertTrue(list.requiresMapping());
    }

    @Test
    void planEntityMethodBody_classifiesFallbackAndAsync() {
        var async = ServiceMethodBodyPlanner.planEntityMethodBody(
                Map.<String, Object>of("options", Map.of("async", true), "returnType", "Customer"),
                Map.<String, Object>of("name", "Customer"),
                Map.<String, Object>of("name", "Customer"));
        var fallback = ServiceMethodBodyPlanner.planEntityMethodBody(
                Map.<String, Object>of("name", "doSomething"),
                null,
                null);

        Assertions.assertEquals(MethodBodyCase.ASYNC_DELEGATE, async.bodyCase());
        Assertions.assertEquals(MethodBodyCase.FALLBACK_TODO, fallback.bodyCase());
    }
}
