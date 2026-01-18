package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;

import java.util.List;
import java.util.Map;

public interface ZDLAnnotator {

    default void annotate(JavaZdlModel javaModel, Map<String, Object> zdl) {
        for (JavaZdlModel.Service service : javaModel.services) {
            var zdlService = JSONPath.get(zdl, "$.services." + service.name(), Map.<String, Object>of());
            annotate(service, zdlService, zdl);

            for (JavaZdlModel.ServiceMethod method : service.methods()) {
                var zdlMethod = JSONPath.get(zdl, "$.services." + service.name() + ".methods." + method.name(), Map.<String, Object>of());
                annotate(method, zdlMethod, zdl);
                for (JavaZdlModel.MethodParameter parameter : method.parameters()) {
                    annotate(parameter, zdlMethod, zdl);
                }
            }
        }
    }

    default void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> method, Map<String, Object> zdl) {
    }

    default void annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> method, Map<String, Object> zdl) {
    }

}
