package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ZDLProcessor extends AbstractBaseProcessor {

    public ZDLProcessor() {
        this.targetProperty = "zdl";
    }

    private Logger log = LoggerFactory.getLogger(getClass());

    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> zdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        if(zdlModel == null) {
            return contextModel;
        }

        processServiceName(zdlModel);
        processServiceAsyncMethods(zdlModel);

        contextModel = new ZDL2JDLProcessor().process(contextModel); // FIXME: why here in the middle of the process?

        List<Map<String, Object>> entities = JSONPath.get(zdlModel, "$.entities[*]");
        for (Map<String, Object> entity : entities) {
            if(JSONPath.get(entity, "options.extends") != null) {
                fillExtendsEntities(entity, zdlModel);
            }
            if(JSONPath.get(entity, "options.copy") != null) {
                fillCopyEntities(entity, zdlModel);
            }
            fillEntityRelationships(entity, zdlModel);
        }

        processCopyAnnotation(zdlModel);

        return contextModel;
    }

    public void processServiceName(Map<String, Object> zdlModel) {
        var services = JSONPath.get(zdlModel, "$.services", Map.of());
        for (Map.Entry<Object, Object> service : services.entrySet()) {
            var aggregates = JSONPath.get(service.getValue(), "$.aggregates", List.of());
            for (Object aggregate : aggregates) {
                if(JSONPath.get(zdlModel, "$.entities." + aggregate) != null) {
                    JSONPath.set(zdlModel, "$.entities." + aggregate + ".options.service", service.getKey());
                }
//                if(JSONPath.get(zdlModel, "$.aggregates." + aggregate) != null) {
//                    JSONPath.set(zdlModel, "$.aggregates." + aggregate + ".options.service", service.getKey());
//                }
            }
        }
    }

    public void processServiceAsyncMethods(Map<String, Object> zdlModel) {
        var asyncMethods = JSONPath.get(zdlModel, "$.services[*].methods[*][?(@.options.async)]", List.<Map>of());
        for (Map asyncMethod : asyncMethods) {
            Map service = JSONPath.get(zdlModel, "$.services." + asyncMethod.get("serviceName"));
            var syncMethodName = asyncMethod.get("name") + "Sync";
            if(JSONPath.get(service, "$.methods." + syncMethodName) == null) {
                var syncMethod = Maps.copy(asyncMethod);
                syncMethod.put("name", syncMethodName);
                ((Map)syncMethod.get("options")).remove("async");
                ((List)syncMethod.get("optionsList")).removeIf(o -> "async".equals(((Map)o).get("name")));
                ((Map)service.get("methods")).put(syncMethodName, syncMethod);
            } else {
                log.error("Error Processing @async({}) in {}. Method {} already defined.", asyncMethod.get("name"), asyncMethod.get("serviceName"), syncMethodName);
            }
        }
    }

    public void processCopyAnnotation(Map<String, Object> zdlModel) {
        var allEntities = new HashMap<>();
        allEntities.putAll(JSONPath.get(zdlModel, "$.allEntitiesAndEnums"));
        allEntities.putAll(JSONPath.get(zdlModel, "$.events"));
        var entitiesToPopulate = new ArrayList<Map>();
        entitiesToPopulate.addAll(JSONPath.get(zdlModel, "$.allEntitiesAndEnums[*][?(@.options.copy)]"));
        entitiesToPopulate.addAll(JSONPath.get(zdlModel, "$.events[*][?(@.options.copy)]"));
        for (var entity : entitiesToPopulate) {
            var toCopyName = JSONPath.get(entity, "$.options.copy");
            var entityToCopy = JSONPath.get(allEntities, "$." + toCopyName);
            if(entityToCopy == null) {
                log.error("Error Processing @copy({}) in {} {}", toCopyName, JSONPath.get(entity, "$.type"), JSONPath.get(entity, "$.name"));
                continue;
            }
            var fieldsToCopy = JSONPath.get(entityToCopy, "$.fields", Map.of());
            var entityFields = JSONPath.get(entity, "$.fields", Map.of());

            var copiedFields = new LinkedHashMap<>();
            copiedFields.putAll(fieldsToCopy);
            copiedFields.putAll(entityFields);

            entityFields.clear();
            entityFields.putAll(copiedFields);
        }
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
            relationshipMap.put("mapsId", JSONPath.get(relationship, "toOptions.Id", false));
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
