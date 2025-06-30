package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ZDLHttpUtils {

    public static List<Pair<String, String>> methodParameters(Map operation, String openApiModelNamePrefix, String openApiModelNameSuffix) {
        List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
        if(JSONPath.get(operation, "requestBody.content['multipart/form-data']") instanceof Map) {
            params = JSONPath.get(operation, "requestBody.content['multipart/form-data'].schema.properties", Map.of())
                    .entrySet().stream().map(entry -> {
                        return Map.of("name", entry.getKey(), "schema", entry.getValue());
                    }).toList();
        };
        List<Pair<String, String>> methodParams = params.stream()
                .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                .map(param -> {
                    String javaType = getJavaType(param, openApiModelNamePrefix, openApiModelNameSuffix);
                    String name = JSONPath.get(param, "$.name");
                    return Pair.of(javaType, name);
                }).collect(Collectors.toList());
        if (operation.containsKey("x--request-dto")) {
            if("patch".equals(JSONPath.get(operation, "x--httpVerb"))) {
                methodParams.add(Pair.of("Map", "input"));
            } else {
                var dto = (String) operation.get("x--request-dto");
                methodParams.add(Pair.of(format("%s%s%s", openApiModelNamePrefix, dto, openApiModelNameSuffix), "reqBody"));

            }
        }
        return methodParams;
    }

    public static String getJavaType(Map<String, Object> param, String openApiModelNamePrefix, String openApiModelNameSuffix) {
        String type = JSONPath.get(param, "$.schema.type");
        String format = JSONPath.get(param, "$.schema.format");
        String schemaName = JSONPath.get(param, "$.schema.x--schema-name");

        if("binary".equals(format)) {
            return "org.springframework.web.multipart.MultipartFile";
        }
        if ("date".equals(format)) {
            return "LocalDate";
        }
        if ("date-time".equals(format)) {
            return "Instant";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "Long";
        }
        if ("integer".equals(type)) {
            return "Integer";
        }
        if ("number".equals(type)) {
            return "BigDecimal";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if ("array".equals(type)) {
            return "List<String>";
        }
        if(schemaName != null) {
            return openApiModelNamePrefix + schemaName + openApiModelNameSuffix;
        }

        return "String";
    }

    public static int compareParamsByRequire(Map<String, Object> param1, Map<String, Object> param2) {
        boolean required1 = JSONPath.get(param1, "required", false);
        boolean required2 = JSONPath.get(param2, "required", false);
        return (required1 && required2) || (!required1 && !required2) ? 0 : required1 ? -1 : 1;
    }


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
                    var isArray = JSONPath.get(zdl, "$.allEntitiesAndEnums." + methodParameterType + ".fields." + entry.getKey() + ".isArray", false);
                    var typeAndFormat = EntitiesToSchemasConverter.schemaTypeAndFormat((String) type);
                    var description = JSONPath.get(zdl, "$.allEntitiesAndEnums." + methodParameterType + ".fields." + entry.getKey() + ".javadoc");
                    return Maps.of("name", entry.getKey(), "type", typeAndFormat.get("type"), "format", typeAndFormat.get("format"), "description", description, "isArray", isArray);
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
