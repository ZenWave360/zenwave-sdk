package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.jsonrefparser.$Ref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenApiProcessor extends AbstractBaseProcessor implements Processor {

    @Override
    public Map<String, ?> process(Map<String, ?> contextModel) {
        Model apiModel = targetProperty != null? (Model) contextModel.get(targetProperty) : (Model) contextModel;

        apiModel.getRefs().getOriginalRefsList().forEach(pair -> {
            if(pair.getValue() instanceof Map) {
                ((Map) pair.getValue()).put("x--original-$ref", pair.getKey().getRef());
            }
        });


        List<Map<String, Object>> parametersParents = getJsonPath(apiModel, "$.paths[*][?(@.parameters)]");
        for (Map<String, Object> parametersParent : parametersParents) {
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) parametersParent.remove("parameters");
            for (Object pathItemEntry : parametersParent.values()) {
                addParentParametersToOperation((Map<String, Object>) pathItemEntry, parameters);
            }
        }

        Map<String, Map<String, Map<String, Object>>> paths = JSONPath.get(apiModel, "$.paths", Collections.emptyMap());
        for (Map.Entry<String, Map<String, Map<String, Object>>> path : paths.entrySet()) {
            for (Map.Entry<String, Map<String, Object>> pathItem : path.getValue().entrySet()) {
                addPathNameToOperation(pathItem.getValue(), path.getKey());
                addHttpVerbToOperation(pathItem.getValue(), pathItem.getKey());
                addNormalizedTagName(pathItem.getValue());
                addOperationIdVariants(pathItem.getValue());
            }
        }

        List<Map<String, Object>> operations = getJsonPath(apiModel, "$.paths[*][*][?(@.operationId)]");
        for (Map<String, Object> operation : operations) {
            prepareOperationRequestInfo(apiModel, operation);
            simplifyOperationResponseInfo(apiModel, operation);
        }

        Map<String, Map> schemas = JSONPath.get(apiModel, "$.components.schemas", Collections.emptyMap());
        for (Map.Entry<String, Map> entry : schemas.entrySet()) {
            entry.getValue().put("x--schema-name", entry.getKey());
        }

        List<Map<String, Map>> properties = JSONPath.get(apiModel, "$.components.schemas..[?(@.properties)].properties");
        for (Map<String, Map> property : properties) {
            for (Map.Entry<String, Map> entry : property.entrySet()) {
                entry.getValue().put("x--property-name", entry.getKey());
            }
        }

        return contextModel;
    }


    private void addPathNameToOperation(Map<String, Object> operation, String path) {
        if (operation != null) {
            operation.put("x--path", path);
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

    private void prepareOperationRequestInfo(Model apiModel, Map<String, Object> operation) {
        Map<String, Object> requestBodySchema = getJsonPath(operation, "$.requestBody.content['application/json'].schema");
        if (requestBodySchema != null) {
            $Ref ref = apiModel.getRefs().getOriginalRef(requestBodySchema);
            operation.put("x--content-type", "application/json");
            operation.put("x--request-dto", getDtoName(ref));
            operation.put("x--request-schema", requestBodySchema);
        }
    }

    private void simplifyOperationResponseInfo(Model apiModel, Map<String, Object> operation) {
        Map<String, Map<String, Object>> responses = getJsonPath(operation, "$.responses");
        if(responses != null) {
            operation.put("x--response", getSimplifiedResponseInfo(apiModel, responses.entrySet().stream().findFirst().get()));
            for (Map.Entry<String, Map<String, Object>> responseEntry : responses.entrySet()) {
                Map<String, Object> simplified = getSimplifiedResponseInfo(apiModel, responseEntry);
                responseEntry.getValue().putAll(simplified);
            }
        }
    }

    private Map<String, Object> getSimplifiedResponseInfo(Model apiModel, Map.Entry<String, Map<String, Object>> response) {
        Map<String, Object> simplified = new HashMap<>();
        simplified.put("x--statusCode", response.getKey());
        Map<String, Object> responseBodySchema = getJsonPath(response.getValue(), "$.content['application/json'].schema");
        if(responseBodySchema != null) {
            $Ref ref = apiModel.getRefs().getOriginalRef(responseBodySchema);
            simplified.put("x--content-type", "application/json");
            simplified.put("x--response-schema", responseBodySchema);
            simplified.put("x--response-dto", getDtoName(ref));
        }
        return simplified;
    }

    private String getDtoName($Ref ref) {
        if (ref != null) {
            return ref.getRef().replace("#/components/schemas/", "");
        }
        return null;
    }

}
