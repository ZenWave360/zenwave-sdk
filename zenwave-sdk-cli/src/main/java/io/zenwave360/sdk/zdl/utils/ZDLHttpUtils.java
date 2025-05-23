package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZDLHttpUtils {

    public static String getPathFromMethod(Map method) {
        var httpOption = getHttpOption(method);
        return getPathFromMethodOptions(httpOption);
    }

    public static String getPathFromMethodOptions(Map httpOption) {
        if (httpOption == null) {
            return "";
        }
        var httpOptions = httpOption.get("httpOptions");
        return httpOptions instanceof String? (String) httpOptions : JSONPath.get(httpOptions, "$.path", "");
    }

    public static List<Map<String, Object>> getPathParamsAsObject(Map zdl, Map method, Map naturalIdTypes, String idType, String idTypeFormat) {
        var path = getPathFromMethod(method);
        var httpOption = getHttpOption(method);
        var params = new HashMap(naturalIdTypes);
        var methodParameterType = (String) method.get("parameter");
        params.putAll(JSONPath.get(httpOption, "$.httpOptions.params", Map.of()));
        return (List) getPathParams(path).stream().map(param -> {
            var type = params.getOrDefault(param, "String");
            var typeAndFormat = EntitiesToSchemasConverter.schemaTypeAndFormat((String) type);
            var description = JSONPath.get(zdl, "$.allEntitiesAndEnums." + methodParameterType + ".fields." + param + ".javadoc");
            if(!params.containsKey(param) && (param.startsWith("id") || param.endsWith("Id"))) {
                typeAndFormat.put("type", idType);
                typeAndFormat.put("format", idTypeFormat);
            }
            return Maps.of("name", param,"type", typeAndFormat.get("type"), "format", typeAndFormat.get("format"), "description", description);
        }).toList();
    }

    public static List<Map<String, Object>> getQueryParamsAsObject(Map method, Map zdl) {
        var pathParams = getPathParamsFromMethod(method);
        var httpOption = getHttpOption(method);
        var params = new LinkedHashMap<String, Object>(JSONPath.get(httpOption, "$.httpOptions.params", Map.of()));
        var methodParameterType = (String) method.get("parameter");
        if ("get".equals(httpOption.get("httpMethod"))) {
            var parameterEntity = JSONPath.get(zdl, "$.allEntitiesAndEnums." + methodParameterType);
            if (parameterEntity != null) {
                var fields = JSONPath.get(parameterEntity, "$.fields", Map.<String, Map>of());
                for (var field : fields.values()) {
                    if (!JSONPath.get(field, "$.isComplexType", false)) {
                        params.put((String) field.get("name"), field.get("type"));
                    }
                }
            }
        }
        return (List) params.entrySet().stream()
                .filter(entry -> !pathParams.contains(entry.getKey()))
                .map(entry -> {
                    var type = entry.getValue();
                    var typeAndFormat = EntitiesToSchemasConverter.schemaTypeAndFormat((String) type);
                    var description = JSONPath.get(zdl, "$.allEntitiesAndEnums." + methodParameterType + ".fields." + entry.getKey() + ".javadoc");
                    return Maps.of("name", entry.getKey(), "type", typeAndFormat.get("type"), "format", typeAndFormat.get("format"), "description", description);
                })
                .toList();
    }
    public static List<String> getPathParamsFromMethod(Map method) {
        var path = getPathFromMethod(method);
        return getPathParams(path);
    }

    public static String getFirstPathParamsFromMethod(Map method) {
        var pathParams = getPathParamsFromMethod(method);
        return !pathParams.isEmpty() ? pathParams.get(0) : null;
    }

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([a-zA-Z0-9-_]+)\\}");
    public static List<String> getPathParams(String path) {
        final Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        var pathParams = new ArrayList<String>();

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                var pathParam = matcher.group(i);
                pathParams.add(pathParam);
            }
        }
        return pathParams;
    }

    public static String getRequestBodyType(Map<String, Object> method, Map apiModel) {
        var methodParameterType = (String) method.get("parameter");
        var parameterEntity = JSONPath.get(apiModel, "$.allEntitiesAndEnums." + methodParameterType);
        if(parameterEntity == null) {
            return null;
        }
        var isInline = JSONPath.get(parameterEntity, "$.options.inline", false);
        if (isInline) {
            var fields = JSONPath.get(parameterEntity, "$.fields", Map.<String, Map>of());
            for (Map field : fields.values()) {
                if (JSONPath.get(field, "$.isComplexType", false)) {
                    return JSONPath.get(field, "$.type");
                }
            }
        }
        return methodParameterType;
    }

    public static Map<String, Object> getHttpOption(Map method) {
        var get = JSONPath.get(method, "$.options.get");
        var post = JSONPath.get(method, "$.options.post");
        var put = JSONPath.get(method, "$.options.put");
        var patch = JSONPath.get(method, "$.options.patch");
        var delete = JSONPath.get(method, "$.options.delete");
        var httpOptions = ObjectUtils.firstNonNull(get, put, post, patch, delete);
        var httpMethod = get != null? "get" : put != null? "put" : post != null? "post" : delete != null? "delete" : patch != null? "patch" : null;
        if (httpMethod == null) {
            return null;
        }
        var optionsMap = new LinkedHashMap();
        if(httpOptions instanceof String) {
            optionsMap.put("path", httpOptions);
        }
        else if(httpOptions instanceof Map) {
            optionsMap.putAll((Map) httpOptions);
        }
        return Maps.of("httpMethod", httpMethod, "httpOptions", optionsMap);
    }
}
