package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JDefinedClass;
import org.jsonschema2pojo.AbstractAnnotator;
import org.jsonschema2pojo.GenerationConfig;

import java.util.Map;

public class CustomAnnotator extends AbstractAnnotator {

    private final String annotationClassName;
    private final Map<String, String> annotationProperties;

    public CustomAnnotator(GenerationConfig generationConfig, String annotationClassName, Map<String, String> annotationProperties) {
        super(generationConfig);
        this.annotationClassName = annotationClassName;
        this.annotationProperties = annotationProperties;
    }

    @Override
    public void typeInfo(JDefinedClass clazz, JsonNode schema) {
        // Add the custom annotation to the class using fully qualified name
        JAnnotationUse annotation = clazz.annotate(clazz.owner().ref(annotationClassName));

        // Add annotation properties if any
        for (Map.Entry<String, String> entry : annotationProperties.entrySet()) {
            annotation.param(entry.getKey(), entry.getValue());
        }

        // Call super to maintain any default annotations
        super.typeInfo(clazz, schema);
    }
}
