package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.Model;

import java.util.Map;

public class JDLProcessor extends AbstractBaseProcessor {
    @Override
    public Map<String, ?> process(Map<String, ?> contextModel) {
        Map<String, ?> jdlModel = targetProperty != null? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        // TODO placeholder for future processing
        return contextModel;
    }
}
