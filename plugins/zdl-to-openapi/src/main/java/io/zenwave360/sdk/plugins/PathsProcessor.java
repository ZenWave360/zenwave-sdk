package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.AbstractBaseProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.FluentMap;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.ZDLHttpUtils;

import java.util.Map;

public class PathsProcessor extends AbstractBaseProcessor implements Processor {

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    {
        targetProperty = "zdl";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map zdl = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;

        var services = JSONPath.get(zdl, "$.services", Map.of());
        services.values().forEach(service -> {
            var restOption = JSONPath.get(service, "$.options.rest");
            if(restOption != null) {
                var basePath = restOption instanceof String? restOption : JSONPath.get(restOption, "$.path", "");
                var methods = JSONPath.get(service, "$.methods", Map.<String, Map>of());
                var paths = new FluentMap();
                methods.forEach((methodName, method) -> {
                    var paginated = JSONPath.get(method, "$.options.paginated");
                    var httpOption = ZDLHttpUtils.getHttpOption((Map) method);
                    if(httpOption != null) {
                        var methodVerb = httpOption.get("httpMethod");
                        var methodPath = ZDLHttpUtils.getPathFromMethod(method);
                        var path = basePath + methodPath;
//                        var params = httpOption.get("params");
                        var pathParams = ZDLHttpUtils.getPathParams(path);
                        var pathParamsMap = ZDLHttpUtils.getPathParamsAsObject(method, idType, idTypeFormat);
                        var queryParamsMap = ZDLHttpUtils.getQueryParamsAsObject(method, zdl);
                        var hasParams = !pathParams.isEmpty() || !queryParamsMap.isEmpty() || paginated != null;
                        paths.appendTo(path, (String) methodVerb, new FluentMap()
                                .with("operationId", methodName)
                                .with("httpMethod", methodVerb)
                                .with("tags", new String[]{(String) ((Map)service).get("name")})
                                .with("summary", method.get("javadoc"))
                                .with("hasParams", hasParams)
//                                .with("params", params)
                                .with("pathParams", pathParams)
                                .with("pathParamsMap", pathParamsMap)
                                .with("queryParamsMap", queryParamsMap)
                                .with("requestBody", ZDLHttpUtils.getRequestBodyType(method, zdl))
                                .with("responseBody", method.get("returnType"))
                                .with("isResponseBodyArray", method.get("returnTypeIsArray"))
                                .with("paginated", paginated)
                                .with("httpOptions", httpOption.get("httpOptions"))
                                .with("serviceMethod", method)
                        );
                    }
                });
                ((Map) service).put("paths", paths);
            }
        });

        return contextModel;
    }
}
