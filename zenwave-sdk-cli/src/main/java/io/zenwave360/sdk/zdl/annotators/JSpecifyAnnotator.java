package io.zenwave360.sdk.zdl.annotators;

import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotation;
import io.zenwave360.sdk.zdl.utils.ZDLAnnotator;

import java.util.Map;

/**
 * Adds <a href="https://jspecify.dev">JSpecify</a> nullability annotations.
 *
 * <p>
 * Both annotations are artifact independent ({@link Annotation#of}), so {@code @NullMarked} renders
 * on the service port and on its implementation alike, and {@code @Nullable} renders in every
 * signature that prints the parameter.
 * </p>
 */
public class JSpecifyAnnotator implements ZDLAnnotator {

    private static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";
    private static final String NULLABLE = "org.jspecify.annotations.Nullable";

    @Override
    public void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
        service.addAnnotation(Annotation.of(NULL_MARKED));
    }

    @Override
    public void annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> method, Map<String, Object> zdl) {
        if (methodParameter.isOptional()) {
            methodParameter.addAnnotation(Annotation.of(NULLABLE));
        }
    }
}
