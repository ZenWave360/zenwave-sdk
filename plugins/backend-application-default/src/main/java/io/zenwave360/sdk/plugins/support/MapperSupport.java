package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MapperSupport {

    private MapperSupport() {
    }

    public static Map<String, Object> serviceParameterEntityPairs(Map<String, Object> zdl,
                                                                  Map<String, Object> service,
                                                                  String inputDtoSuffix) {
        var map = new HashMap<String, Object>();
        for (Map method : JSONPath.get(service, "methods[*]", java.util.List.<Map>of())) {
            var input = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("parameter"));
            var entity = (Map) JSONPath.get(zdl, "$.entities." + method.get("entity"));
            var isInput = input != null && "inputs".equals(input.get("type"));
            var isPatch = JSONPath.get(method, "options.patch") != null;

            if (entity != null) {
                if (isPatch) {
                    var key = "java.util.Map-" + entity.get("className");
                    map.put(key, Maps.of("input", Map.of("className", "Map"), "entity", entity, "method", method));
                } else if (input != null) {
                    var key = input.get("className") + (isInput ? inputDtoSuffix : "") + "-" + entity.get("className");
                    map.put(key, Maps.of("input", input, "entity", entity, "method", method));
                }
            }
        }
        return map;
    }

    public static Map<String, Object> serviceEntityReturnTypePairs(Map<String, Object> zdl,
                                                                   Map<String, Object> service) {
        var map = new HashMap<String, Object>();
        for (Map method : JSONPath.get(service, "methods[*]", java.util.List.<Map>of())) {
            var entity = (Map) JSONPath.get(zdl, "$.entities." + method.get("entity"));
            var output = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("returnType"));
            var isArray = Boolean.TRUE.equals(method.get("returnTypeIsArray"));
            var isOptional = Boolean.TRUE.equals(method.get("returnTypeIsOptional"));
            var isPaginated = JSONPath.get(method, "options.paginated", false);

            if (entity != null && output != null) {
                if (entity.get("name").equals(output.get("name"))) {
                    continue;
                }
                var key = entity.get("className") + "-" + output.get("className");
                map.put(key, Maps.of("entity", entity, "output", output, "method", method,
                        "isArray", isArray, "isOptional", isOptional, "isPaginated", isPaginated));
            }
        }
        return map;
    }

    public static String wrapWithMapper(Map<String, Object> method,
                                        Map<String, Object> entity,
                                        Map<String, Object> returnType) {
        if (returnType == null) {
            return "";
        }
        var returnTypeIsArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
        var instanceName = returnTypeIsArray ? entity.get("instanceNamePlural") : entity.get("instanceName");
        var serviceInstanceName = io.zenwave360.sdk.utils.NamingUtils.asInstanceName((String) method.get("serviceName"));
        if (Objects.equals(entity.get("name"), returnType.get("name"))) {
            return (String) instanceName;
        }
        if (returnTypeIsArray) {
            if (JSONPath.get(method, "options.paginated", false)) {
                return String.format("%sMapper.as%sPage(%s)", serviceInstanceName, returnType.get("className"), instanceName);
            }
            return String.format("%sMapper.as%sList(%s)", serviceInstanceName, returnType.get("className"), instanceName);
        }
        return String.format("%sMapper.as%s(%s)", serviceInstanceName, returnType.get("className"), instanceName);
    }
}
