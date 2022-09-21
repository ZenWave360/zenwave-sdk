package io.zenwave360.generator.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;

public class JDLProcessor extends AbstractBaseProcessor {

    public JDLProcessor() {
        this.targetProperty = "jdl";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> jdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        List<Map<String, Object>> entitiesWithCriteria = JSONPath.get(jdlModel, "$.entities[*][?(@.options.searchCriteria)]");
        for (Map<String, Object> entity : entitiesWithCriteria) {
            fillCriteriaObject(entity, jdlModel);
        }
        return contextModel;
    }

    protected void fillCriteriaObject(Map<String, Object> entity, Map<String, Object> jdlModel) {
        Object searchCriteria = JSONPath.get(entity, "$.options.searchCriteria");
        if (searchCriteria == Boolean.TRUE) {
            searchCriteria = entity.get("name");
        }

        Map searchCriteriaObject = Maps.copy(JSONPath.get(jdlModel, "$.entities." + searchCriteria));
        searchCriteriaObject.put("options", new HashMap<>());
        JSONPath.set(entity, "options.searchCriteriaObject", searchCriteriaObject);
    }
}
