package io.zenwave360.sdk.plugins.annotators;

import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import io.zenwave360.sdk.zdl.utils.ZDLAnnotator;

import java.util.Map;

public class JSpecifyAnnotator implements ZDLAnnotator {

    @Override
    public void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
        service.annotations().add(new JavaZdlModel.Annotation("org.jspecify.annotations.NullMarked", null, null));
    }

    @Override
    public void annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> method, Map<String, Object> zdl) {
        if(methodParameter.isOptional()) {
            methodParameter.annotations().add(new JavaZdlModel.Annotation("org.jspecify.annotations.Nullable", null, null));
        }
    }
}
