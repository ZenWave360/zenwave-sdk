package io.zenwave360.sdk.plugins.kotlin;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLHttpUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class OpenAPIControllersKotlinHelpers {

    public final String openApiModelNamePrefix;
    public final String openApiModelNameSuffix;

    public OpenAPIControllersKotlinHelpers(String openApiModelNamePrefix, String openApiModelNameSuffix) {
        this.openApiModelNamePrefix = openApiModelNamePrefix;
        this.openApiModelNameSuffix = openApiModelNameSuffix;
    }

    public String kotlinMethodParametersSignature(String methodParametersSignature, Options options) {
        return ZDLJavaSignatureUtils.toKotlinMethodSignature(methodParametersSignature)
                .replaceAll(": Integer", ": Int");
    }

    public String voidUnit(String returnType, Options options) {
        return returnType.replace("void", "Unit").replace("Void", "Unit");
    }

    public CharSequence asMethodParametersInitializer(Object operation, Options options) throws IOException {
        if (operation instanceof Map) {
            var methodParams = ZDLHttpUtils.methodParameters((Map) operation, openApiModelNamePrefix, openApiModelNameSuffix);
            if(methodParams.isEmpty()) {
                return "";
            }
            return methodParams.stream()
                    .map(param -> "val " + param.getValue() + ": " + param.getKey() + " = " + instantiateParam((Map) operation, param.getValue(), param.getKey()))
                    .collect(Collectors.joining("\n"))
                    .replaceAll(" Integer ", " Int ");
        }
        return options.fn(operation);
    }

    private String instantiateParam(Map operation, String paramName, String dtoName) {
        var params = JSONPath.get(operation, "parameters[?(@.name == '" + paramName + "')]", List.of());
        var param = params.isEmpty() ? null : params.get(0);
        var isArray = "array".equals(JSONPath.get(param, "schema.type"));
        var isEnum = JSONPath.get(param, "schema.enum") != null;
        if(isEnum) {
            return dtoName + "." + JSONPath.get(param, "schema.enum[0]");
        }
        if(isArray) {
            return "mutableListOf()";
        }
        var isNumber = "number".equals(JSONPath.get(param, "schema.type")) || "integer".equals(JSONPath.get(param, "schema.type"));
        var isBoolean = "boolean".equals(JSONPath.get(param, "schema.type"));
        var isString = "string".equals(JSONPath.get(param, "schema.type"));
        var isDate = "date".equals(JSONPath.get(param, "schema.format"));
        var isDateTime = "date-time".equals(JSONPath.get(param, "schema.format"));
        if(isNumber) {
            return "0";
        }
        if(isDate) {
            return "LocalDate.now()";
        }
        if(isDateTime) {
            return "Instant.now()";
        }
        if(isBoolean) {
            return "false";
        }
        if(isString) {
            return "\"\"";
        }
        var requiredFields = JSONPath.get(operation, "x--request-schema.required", List.<String>of()).stream()
                .map(it -> it + " = \"\"").collect(Collectors.joining(", "));
        return dtoName + "(" + requiredFields + ")";
    }
}
