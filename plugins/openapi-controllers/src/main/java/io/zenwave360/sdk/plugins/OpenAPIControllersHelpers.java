package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.zdl.utils.ZDLHttpUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAPIControllersHelpers {

    public final String openApiModelNamePrefix;
    public final String openApiModelNameSuffix;

    public OpenAPIControllersHelpers(String openApiModelNamePrefix, String openApiModelNameSuffix) {
        this.openApiModelNamePrefix = openApiModelNamePrefix;
        this.openApiModelNameSuffix = openApiModelNameSuffix;
    }


    public CharSequence asMethodParametersInitializer(Object operation, Options options) throws IOException {
        if (operation instanceof Map) {
            var methodParams = ZDLHttpUtils.methodParameters((Map) operation, openApiModelNamePrefix, openApiModelNameSuffix);
            if(methodParams.isEmpty()) {
                return "";
            }
            return methodParams.stream()
                    .map(param -> param.getKey() + " " + param.getValue() + " = null;")
                    .collect(Collectors.joining("\n"));
        }
        return options.fn(operation);
    }

    public CharSequence asMethodParameterValues(Object operation, Options options) throws IOException {
        if (operation instanceof Map) {
            var methodParams = ZDLHttpUtils.methodParameters((Map) operation, openApiModelNamePrefix, openApiModelNameSuffix);
            return methodParams.stream()
                    .map(param -> param.getValue())
                    .collect(Collectors.joining(", "));
        }
        return options.fn(operation);
    }
}
