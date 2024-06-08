package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLHttpUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
        return ZDLJavaSignatureUtils.toKotlinMethodSignature(methodParametersSignature);
    }

    public CharSequence asMethodParametersInitializer(Object operation, Options options) throws IOException {
        if (operation instanceof Map) {
            var methodParams = ZDLHttpUtils.methodParameters((Map) operation, openApiModelNamePrefix, openApiModelNameSuffix);
            if(methodParams.isEmpty()) {
                return "";
            }
            return methodParams.stream()
                    .map(param -> param.getValue() + ": " + param.getKey() + " = null;")
                    .collect(Collectors.joining("\n"));
        }
        return options.fn(operation);
    }
}
