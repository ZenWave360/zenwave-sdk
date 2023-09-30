package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AbstractBaseProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.FluentMap;
import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathsProcessor extends AbstractBaseProcessor implements Processor {

    {
        targetProperty = "zdl";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map apiModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;

        var services = JSONPath.get(apiModel, "$.services", Map.of());
        services.values().forEach(service -> {
            var restOption = JSONPath.get(service, "$.options.rest");
            if(restOption != null) {
                var basePath = restOption instanceof String? restOption : JSONPath.get(restOption, "$.path", "");
                var methods = JSONPath.get(service, "$.methods", Map.<String, Map>of());
                var paths = new FluentMap();
                methods.forEach((methodName, method) -> {
                    var paginated = JSONPath.get(method, "$.options.paginated");
                    var httpOption = getHttpOption((Map) method);
                    if(httpOption != null) {
                        var methodVerb = httpOption.get("httpMethod");
                        var httpOptions = httpOption.get("httpOptions");
                        var methodPath = httpOptions instanceof String? httpOptions : JSONPath.get(httpOptions, "$.path", "");
                        var path = (String) basePath + methodPath;
                        var params = httpOption.get("params");
                        var pathParams = getPathParams(path);
                        var hasParams = params != null || pathParams.size() > 0 || paginated != null;
                        paths.appendTo(path, (String) methodVerb, new FluentMap()
                                .with("operationId", methodName)
                                .with("httpMethod", methodVerb)
                                .with("tags", new String[]{(String) ((Map)service).get("name")})
                                .with("summary", method.get("javadoc"))
                                .with("hasParams", hasParams)
                                .with("params", params)
                                .with("pathParams", pathParams)
                                .with("requestBody", method.get("parameter"))
                                .with("responseBody", method.get("returnType"))
                                .with("isResponseBodyArray", method.get("returnTypeIsArray"))
                                .with("paginated", paginated)
                                .with("httpOptions", httpOptions)
                                .with("serviceMethod", method)
                        );
                    }
                });
                ((Map) service).put("paths", paths);
            }
        });

        return contextModel;
    }

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([a-zA-Z0-9-_]+)\\}");
    private List<String> getPathParams(String path) {
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

    Map<String, Object> getHttpOption(Map method) {
        var get = JSONPath.get(method, "$.options.get");
        var put = JSONPath.get(method, "$.options.put");
        var post = JSONPath.get(method, "$.options.post");
        var delete = JSONPath.get(method, "$.options.delete");
        var httpOptions = ObjectUtils.firstNonNull(get, put, post, delete);
        var httpMethod = get != null? "get" : put != null? "put" : post != null? "post" : delete != null? "delete" : null;
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
