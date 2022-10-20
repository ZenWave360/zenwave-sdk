package io.zenwave360.generator.processors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;
import io.zenwave360.generator.utils.NamingUtils;

public class JDLProcessor extends AbstractBaseProcessor {

    public JDLProcessor() {
        this.targetProperty = "jdl";
    }

    // Undocumented. Plugins using this should document the meaning of this option .
    public String criteriaDTOSuffix = "Criteria";

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
            // we are searching for all fields in entity, we create a jdl criteria object with all fields
            searchCriteria = entity.get("name");

            Map searchCriteriaObject = Maps.copy(JSONPath.get(jdlModel, "$.entities." + searchCriteria));
            searchCriteriaObject.put("options", Maps.of("skip", true, "isCriteria", true));

            List.of("name", "instanceName", "className", "classNamePlural", "instanceNamePlural", "tableName")
                    .forEach(field -> searchCriteriaObject.put(field, entity.get(field) + criteriaDTOSuffix));
            List.of("kebabCase", "kebabCasePlural").forEach(field -> searchCriteriaObject.put(field, entity.get(field) + "-" + NamingUtils.kebabCase(criteriaDTOSuffix)));

            ((List) JSONPath.get(searchCriteriaObject, "$.fields.*.validations")).forEach(h -> ((Map) h).clear());

            searchCriteria = searchCriteria + criteriaDTOSuffix;
            JSONPath.set(entity, "$.options.searchCriteria", searchCriteria);
            JSONPath.set(jdlModel, "$.entities." + searchCriteria, searchCriteriaObject);
        }

        JSONPath.get(jdlModel, "$.entities." + searchCriteria + ".options", Collections.emptyMap()).put("isCriteria", true);
        Map searchCriteriaObject = Maps.copy(JSONPath.get(jdlModel, "$.entities." + searchCriteria));
        JSONPath.set(entity, "options.searchCriteriaObject", searchCriteriaObject);
    }
}
