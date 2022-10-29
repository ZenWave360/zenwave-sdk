package io.zenwave360.generator.processors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;
import io.zenwave360.generator.utils.NamingUtils;
import org.apache.commons.lang3.StringUtils;

public class JDLProcessor extends AbstractBaseProcessor {

    public JDLProcessor() {
        this.targetProperty = "jdl";
    }

    // Undocumented. Plugins using this should document the meaning of this option .
    public String criteriaDTOSuffix = "Criteria";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> jdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        List<Map<String, Object>> entities = JSONPath.get(jdlModel, "$.entities[*]");
        for (Map<String, Object> entity : entities) {
            if(JSONPath.get(entity, "options.searchCriteria") != null) {
                fillCriteriaObject(entity, jdlModel);
            }
            fillEntityRelationships(entity, jdlModel);
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

    protected void fillEntityRelationships(Map entity, Map jdlModel) {
        String entityName = (String) entity.get("name");
        List<Map> relationships = JSONPath.get(jdlModel, "$.relationships[*][*][*][?(@.from == '" + entityName + "' || @.to == '" + entityName + "')]");
        entity.put("relationships", relationships.stream().map(r -> buildRelationship(entityName, r)).collect(Collectors.toList()));
    }

    protected Map buildRelationship(String entityName, Map relationship) {
        Map relationshipMap = Maps.of("type", relationship.get("type"), "_relationship", relationship);
        var from = relationship.get("from");
        var to = relationship.get("to");
        if (from.equals(entityName)) {
            relationshipMap.put("otherEntityName", to);
            relationshipMap.put("ownerSide", true);
            relationshipMap.put("isCollection", relationship.get("type").toString().endsWith("Many"));
            if(relationship.get("injectedFieldInFrom") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInFrom"), ")","").split("\\(");
                relationshipMap.put("fieldName", fillInjectedFieldInFrom[0]);
            }
            if(relationship.get("injectedFieldInTo") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInTo"), ")","").split("\\(");
                relationshipMap.put("otherEntityFieldName", fillInjectedFieldInFrom[0]);
            }
        } else {
            relationshipMap.put("otherEntityName", from);
            relationshipMap.put("ownerSide", false);
            relationshipMap.put("isCollection", relationship.get("type").toString().startsWith("Many"));
            if(relationship.get("injectedFieldInTo") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInTo"), ")","").split("\\(");
                relationshipMap.put("fieldName", fillInjectedFieldInFrom[0]);
            }
            if(relationship.get("injectedFieldInFrom") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInFrom"), ")","").split("\\(");
                relationshipMap.put("otherEntityFieldName", fillInjectedFieldInFrom[0]);
            }
        }
        return relationshipMap;
    }
}
