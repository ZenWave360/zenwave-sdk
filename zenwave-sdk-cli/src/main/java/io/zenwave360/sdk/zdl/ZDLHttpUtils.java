package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static List<Map<String, Object>> getPathParamsAsObject(Map method, String idType, String idTypeFormat) {
        var path = getPathFromMethod(method);
        var httpOption = getHttpOption(method);
        var params = JSONPath.get(httpOption, "$.httpOptions.params", Map.of());
        return (List) getPathParams(path).stream().map(param -> {
            var type = params.getOrDefault(param, "String");
            var typeAndFormat = EntitiesToSchemasConverter.schemaTypeAndFormat((String) type);
            if(!params.containsKey(param) && (param.startsWith("id") || param.endsWith("Id"))) {
                typeAndFormat.put("type", idType);
                typeAndFormat.put("format", idTypeFormat);
            }
            return Maps.of("name", param,"type", typeAndFormat.get("type"), "format", typeAndFormat.get("format"));
        }).toList();
    }

    public static List<Map<String, Object>> getQueryParamsAsObject(Map method) {
        var pathParams = getPathParamsFromMethod(method);
        var httpOption = getHttpOption(method);
        var params = JSONPath.get(httpOption, "$.httpOptions.params", Map.of());
        return (List) params.entrySet().stream().filter(entry -> !pathParams.contains(entry.getKey())).map(entry -> {
            var type = entry.getValue();
            var typeAndFormat = EntitiesToSchemasConverter.schemaTypeAndFormat((String) type);
            return Maps.of("name", entry.getKey(),"type", typeAndFormat.get("type"), "format", typeAndFormat.get("format"));
        }).toList();
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
        var put = JSONPath.get(method, "$.options.put");
        var post = JSONPath.get(method, "$.options.post");
        var delete = JSONPath.get(method, "$.options.delete");
        var httpOptions = ObjectUtils.firstNonNull(get, put, post, delete);
        var httpMethod = get != null? "get" : put != null? "put" : post != null? "post" : delete != null? "delete" : null;
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
        return Map.of("httpMethod", httpMethod, "httpOptions", optionsMap);
    }
}
