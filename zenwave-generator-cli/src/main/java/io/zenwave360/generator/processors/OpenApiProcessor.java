package io.zenwave360.generator.processors;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenApiProcessor extends AbstractBaseProcessor implements Processor {

    @Override
    public Map<String, ?> process(Map<String, ?> contextModel) {
        Map<String, ?> apiModel = targetProperty != null? (Map<String, ?>) contextModel.get(targetProperty) : contextModel;

        List<Map<String, Object>> parametersParents = JsonPath.read(apiModel, "$.paths[*][?(@.parameters)]");
        for (Map<String, Object> parametersParent : parametersParents) {
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) parametersParent.remove("parameters");
            for (Object pathItemEntry : parametersParent.values()) {
                addParentParametersToOperation((Map<String, Object>) pathItemEntry, parameters);
            }
        }

        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(apiModel, "$.paths");
        for (Map.Entry<String, Map<String, Map<String, Object>>> path : paths.entrySet()) {
            for (Map.Entry<String, Map<String, Object>> pathItem : path.getValue().entrySet()) {
                addPathNameToOperation(pathItem.getValue(), path.getKey());
                addHttpVerbToOperation(pathItem.getValue(), pathItem.getKey());
            }
        }

        return contextModel;
    }


    private void addPathNameToOperation(Map<String, Object> operation, String channelName) {
        if (operation != null) {
            operation.put("x--path", channelName);
        }
    }

    private void addHttpVerbToOperation(Map<String, Object> operation, String operationType) {
        if (operation != null) {
            operation.put("x--httpVerb", operationType);
        }
    }

    private void addParentParametersToOperation(Map<String, Object> operation, List<Map<String, Object>> parentParameters) {
        if (operation != null && parentParameters != null && !parentParameters.isEmpty()) {
            if(!operation.containsKey("parameters")) {
                operation.put("parameters", new ArrayList<>());
            }
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.get("parameters");
            parameters.addAll(parentParameters);
        }
    }


}
