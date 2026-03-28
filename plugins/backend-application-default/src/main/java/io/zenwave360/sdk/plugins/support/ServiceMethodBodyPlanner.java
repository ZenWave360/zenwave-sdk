package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.Map;
import java.util.Objects;

public final class ServiceMethodBodyPlanner {

    private ServiceMethodBodyPlanner() {
    }

    public static MethodBodyPlan planEntityMethodBody(Map<String, Object> method,
                                                      Map<String, Object> entity,
                                                      Map<String, Object> returnEntity) {
        boolean hasEntity = entity != null;
        boolean isAsync = optionEnabled(method, "async");
        boolean isDelete = optionEnabled(method, "delete");
        boolean isPatch = optionEnabled(method, "patch");
        boolean hasId = method != null && method.get("paramId") != null;
        boolean hasInput = method != null && method.get("parameter") != null;
        boolean hasReturnType = method != null && method.get("returnType") != null;
        boolean isOptional = Boolean.TRUE.equals(method != null ? method.get("returnTypeIsOptional") : null);
        boolean isArray = Boolean.TRUE.equals(method != null ? method.get("returnTypeIsArray") : null);
        boolean hasTransition = JSONPath.get(method, "$.transition.from") != null || JSONPath.get(method, "$.transition.to") != null;
        boolean hasLifecycle = entity != null && JSONPath.get(entity, "lifecycle") != null;
        boolean requiresMapping = hasEntity
                && hasReturnType
                && returnEntity != null
                && !Objects.equals(entity.get("name"), returnEntity.get("name"));

        MethodBodyCase bodyCase;
        if (isAsync) {
            bodyCase = MethodBodyCase.ASYNC_DELEGATE;
        } else if (hasEntity && hasLifecycle && hasId && hasInput && hasReturnType && isOptional) {
            bodyCase = MethodBodyCase.ENTITY_LIFECYCLE_UPDATE_OPTIONAL;
        } else if (hasEntity && hasLifecycle && hasId && hasInput && hasReturnType) {
            bodyCase = MethodBodyCase.ENTITY_LIFECYCLE_UPDATE_REQUIRED;
        } else if (hasEntity && isDelete && hasId) {
            bodyCase = MethodBodyCase.ENTITY_DELETE;
        } else if (hasEntity && hasReturnType && isArray) {
            bodyCase = MethodBodyCase.ENTITY_LIST;
        } else if (hasEntity && isPatch && hasId && hasInput && hasReturnType && isOptional) {
            bodyCase = MethodBodyCase.ENTITY_PATCH_OPTIONAL;
        } else if (hasEntity && hasId && hasInput && hasReturnType && isOptional) {
            bodyCase = MethodBodyCase.ENTITY_UPDATE_OPTIONAL;
        } else if (hasEntity && hasId && hasInput && hasReturnType) {
            bodyCase = MethodBodyCase.ENTITY_UPDATE_REQUIRED;
        } else if (hasEntity && hasLifecycle && hasId && !hasInput && hasReturnType && isOptional && hasTransition) {
            bodyCase = MethodBodyCase.ENTITY_TRANSITION_OPTIONAL;
        } else if (hasEntity && hasLifecycle && hasId && !hasInput && hasReturnType && hasTransition) {
            bodyCase = MethodBodyCase.ENTITY_TRANSITION_REQUIRED;
        } else if (hasEntity && hasId && hasReturnType && isOptional) {
            bodyCase = MethodBodyCase.ENTITY_GET_OPTIONAL;
        } else if (hasEntity && hasId && hasReturnType) {
            bodyCase = MethodBodyCase.ENTITY_GET_REQUIRED;
        } else if (hasEntity && hasInput && hasReturnType && isOptional) {
            bodyCase = MethodBodyCase.ENTITY_QUERY_BY_INPUT_OPTIONAL;
        } else if (hasEntity && hasInput && hasReturnType) {
            bodyCase = MethodBodyCase.ENTITY_QUERY_BY_INPUT_REQUIRED;
        } else if (hasEntity && hasReturnType) {
            bodyCase = MethodBodyCase.ENTITY_NEW_INSTANCE_RETURN;
        } else if (hasEntity) {
            bodyCase = MethodBodyCase.ENTITY_NEW_INSTANCE_VOID;
        } else {
            bodyCase = MethodBodyCase.FALLBACK_TODO;
        }

        return new MethodBodyPlan(bodyCase, entity, requiresMapping, hasLifecycle, hasTransition);
    }

    private static boolean optionEnabled(Map<String, Object> method, String optionName) {
        return Boolean.TRUE.equals(JSONPath.get(method, "options." + optionName, false));
    }
}
