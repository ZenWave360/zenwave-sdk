package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.Processor;
import io.zenwave360.jsonrefparser.$Ref;
import io.zenwave360.jsonrefparser.$Refs;

import java.util.List;
import java.util.Map;

public class OriginalRefProcessor implements Processor {

    public String sourceProperty = "api";

    public String originalRefProperty = "x--originalRef";

    Model getApiModel(Map<String, Object> contextModel) {
        return(Model) contextModel.get(sourceProperty);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);
        $Refs refs = apiModel.getRefs();
        Map<String, Object> model = apiModel.model();
        processOriginalRef(model, refs);
        return contextModel;
    }

    protected void processOriginalRef(Object model, $Refs refs) {
        if(model instanceof List) {
            for (Object value : (List) model) {
                processOriginalRef(value, refs);
            }
        }
        if(model instanceof Map) {
            for (Object value : ((Map) model).values()) {
                $Ref originalRef = refs.getOriginalRef(value);
                if (originalRef != null && value instanceof Map) {
                    ((Map) value).put(originalRefProperty, originalRef.getRef());
                }
                processOriginalRef(value, refs);
            }
        }
    }
}
