package io.zenwave360.sdk.plugins.annotators;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;

import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationHelper {
    public static String annotate(Object javaModel, Options options) {
        var zdl = (Map) options.get("zdl");
        var model = options.param(0);
        if(javaModel instanceof JavaZdlModel.Service) {
            return annotate((JavaZdlModel.Service) javaModel, (Map) model, zdl);
        }
        if(javaModel instanceof JavaZdlModel.ServiceMethod) {
            return annotate((JavaZdlModel.ServiceMethod) javaModel, (Map) model, zdl);
        }
        return "";
    }

    public static String addImports(Object javaModel, Options options) {
        var zdl = (Map) options.get("zdl");
        var model = options.param(0);
        if(javaModel instanceof JavaZdlModel.Service) {
            return addImports((JavaZdlModel.Service) javaModel, (Map) model, zdl);
        }
        return "";
    }

    private static String addImports(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
        return service.annotations().stream().map(a -> "import " + a.name()).collect(Collectors.joining(";\n"));
    }


    private static String annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
        return service.annotations().stream().map(a -> "@" + a.name()).collect(Collectors.joining("\n"));
    }

    private static String annotate(JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> zdlMethod, Map<String, Object> zdl) {
        return "// todo";
    }

    private static String annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> zdlService, Map<String, Object> zdl) {
        return methodParameter.annotations().stream().map(a -> "@" + a.name()).collect(Collectors.joining("\n"));
    }
}
