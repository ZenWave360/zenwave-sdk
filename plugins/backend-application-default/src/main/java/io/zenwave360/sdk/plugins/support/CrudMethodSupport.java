package io.zenwave360.sdk.plugins.support;

import java.util.Map;

public final class CrudMethodSupport {

    private CrudMethodSupport() {
    }

    public static boolean isCrudMethod(String crudMethodPrefix,
                                       Map<String, Object> entity,
                                       Map<String, Object> method) {
        var entityName = (String) entity.get("name");
        var entityNamePlural = (String) entity.get("classNamePlural");
        var methodName = (String) method.get("name");
        var isArray = "true".equals(String.valueOf(method.get("returnTypeIsArray")));
        var entityMethodSuffix = isArray ? entityNamePlural : entityName;
        return methodName.equals(crudMethodPrefix + entityMethodSuffix);
    }
}

