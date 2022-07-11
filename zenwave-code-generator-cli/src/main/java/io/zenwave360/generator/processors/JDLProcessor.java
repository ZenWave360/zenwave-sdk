package io.zenwave360.generator.processors;

import java.util.Map;

public class JDLProcessor extends AbstractBaseProcessor {
    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> jdlModel = targetProperty != null? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        // TODO placeholder for future processing
        return contextModel;
    }
}
