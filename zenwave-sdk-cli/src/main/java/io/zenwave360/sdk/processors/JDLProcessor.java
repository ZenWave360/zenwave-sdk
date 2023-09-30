package io.zenwave360.sdk.processors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import org.apache.commons.lang3.StringUtils;

public class JDLProcessor extends AbstractBaseProcessor {

    public JDLProcessor() {
        this.targetProperty = "zdl";
    }

    // Undocumented. Plugins using this should document the meaning of this option .
    public String criteriaDTOSuffix = "Criteria";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        contextModel = new ZDL2JDLProcessor().process(contextModel);

        Map<String, Object> zdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        List<Map<String, Object>> entities = JSONPath.get(zdlModel, "$.entities[*]");
        for (Map<String, Object> entity : entities) {
            if(JSONPath.get(entity, "options.searchCriteria") != null) {
                fillCriteriaObject(entity, zdlModel);
            }
            if(JSONPath.get(entity, "options.extends") != null) {
                fillExtendsEntities(entity, zdlModel);
            }
            if(JSONPath.get(entity, "options.copy") != null) {
                fillCopyEntities(entity, zdlModel);
            }
            fillEntityRelationships(entity, zdlModel);
        }
        return contextModel;
    }

    protected void fillExtendsEntities(Map<String, Object> entity, Map<String, Object> zdlModel) {
        String superClassName = JSONPath.get(entity, "options.extends");
        JSONPath.set(zdlModel, "$.entities['" + superClassName + "'].options.isSuperClass", true);
        boolean isExtendsAuditing = JSONPath.get(zdlModel, "$.entities['" + superClassName + "'].options.auditing") != null;
        if(isExtendsAuditing) {
            JSONPath.set(entity, "options.extendsAuditing", true);
        }
    }

    protected void fillCopyEntities(Map<String, Object> entity, Map<String, Object> zdlModel) {
        String entityName = JSONPath.get(entity, "name");
        String superClassName = JSONPath.get(entity, "options.copy");
        Map<String, Object> superClass = JSONPath.get(zdlModel, "$.entities['" + superClassName + "']");
        if(superClass != null) {
            Map<String, Map> fields = JSONPath.get(entity, "$.fields");
            Map<String, Map> superClassFields = JSONPath.get(superClass, "$.fields");
            for (var fieldEntry : superClassFields.entrySet()) {
                if(!fields.containsKey(fieldEntry.getKey())) {
                    fields.put(fieldEntry.getKey(), fieldEntry.getValue());
                }
            }
        }
    }
    protected void fillCriteriaObject(Map<String, Object> entity, Map<String, Object> zdlModel) {
        Object searchCriteria = JSONPath.get(entity, "$.options.searchCriteria");
        if (searchCriteria == Boolean.TRUE) {
            // we are searching for all fields in entity, we create a jdl criteria object with all fields
            searchCriteria = entity.get("name");

            Map searchCriteriaObject = Maps.copy(JSONPath.get(zdlModel, "$.entities." + searchCriteria));
            searchCriteriaObject.put("options", Maps.of("skip", true, "isCriteria", true));

            List.of("name", "instanceName", "className", "classNamePlural", "instanceNamePlural", "tableName")
                    .forEach(field -> searchCriteriaObject.put(field, entity.get(field) + criteriaDTOSuffix));
            List.of("kebabCase", "kebabCasePlural").forEach(field -> searchCriteriaObject.put(field, entity.get(field) + "-" + NamingUtils.kebabCase(criteriaDTOSuffix)));

            ((List) JSONPath.get(searchCriteriaObject, "$.fields.*.validations")).forEach(h -> ((Map) h).clear());

            searchCriteria = searchCriteria + criteriaDTOSuffix;
            JSONPath.set(entity, "$.options.searchCriteria", searchCriteria);
            JSONPath.set(zdlModel, "$.entities." + searchCriteria, searchCriteriaObject);
        }

        JSONPath.get(zdlModel, "$.entities." + searchCriteria + ".options", Collections.emptyMap()).put("isCriteria", true);
        Map searchCriteriaObject = Maps.copy(JSONPath.get(zdlModel, "$.entities." + searchCriteria));
        JSONPath.set(entity, "options.searchCriteriaObject", searchCriteriaObject);
    }

    protected void fillEntityRelationships(Map entity, Map zdlModel) {
        String entityName = (String) entity.get("name");
        List<Map> relationships = JSONPath.get(zdlModel, "$.relationships[*][*][?(@.from == '" + entityName + "' || @.to == '" + entityName + "')]");
        entity.put("relationships", relationships.stream().map(r -> buildRelationship(entityName, r)).collect(Collectors.toList()));
    }

    protected Map buildRelationship(String entityName, Map relationship) {
        Map relationshipMap = Maps.of("type", relationship.get("type"), "_relationship", relationship);
        var from = relationship.get("from");
        var to = relationship.get("to");
        if (from.equals(entityName)) {
            relationshipMap.put("entityName", from);
            relationshipMap.put("otherEntityName", to);
            relationshipMap.put("ownerSide", true);
            relationshipMap.put("isCollection", relationship.get("type").toString().endsWith("Many"));
            if(relationship.get("injectedFieldInFrom") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInFrom"), ")","").split("\\(");
                relationshipMap.put("fieldName", fillInjectedFieldInFrom[0]);
                relationshipMap.put("required", relationship.getOrDefault("isInjectedFieldInFromRequired", false));
            }
            if(relationship.get("injectedFieldInTo") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInTo"), ")","").split("\\(");
                relationshipMap.put("otherEntityFieldName", fillInjectedFieldInFrom[0]);
            }
        } else {
            relationshipMap.put("entityName", to);
            relationshipMap.put("otherEntityName", from);
            relationshipMap.put("ownerSide", false);
            relationshipMap.put("isCollection", relationship.get("type").toString().startsWith("Many"));
            if(relationship.get("injectedFieldInTo") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInTo"), ")","").split("\\(");
                relationshipMap.put("fieldName", fillInjectedFieldInFrom[0]);
                relationshipMap.put("required", relationship.getOrDefault("isInjectedFieldInToRequired", false));
            }
            if(relationship.get("injectedFieldInFrom") != null) {
                var fillInjectedFieldInFrom = StringUtils.replace((String) relationship.get("injectedFieldInFrom"), ")","").split("\\(");
                relationshipMap.put("otherEntityFieldName", fillInjectedFieldInFrom[0]);
            }
        }
        return relationshipMap;
    }
}
