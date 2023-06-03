package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZDLUtils {

    public static List<String> findAllServiceFacingEntities(Map<String, Object> model) {
        var serviceEntities = ZDLUtils.findMethodParameterAndReturnTypes(model);
        return ZDLUtils.findDependentEntities(model, serviceEntities);
    }

    public static List<String> findAllPaginatedEntities(Map<String, Object> model) {
        return JSONPath.get(model, "$.services[*].methods[*][?(@.options.pageable == true)].returnType", List.<String>of());
    }

    public static List<String> findAllEntitiesReturnedAsList(Map<String, Object> model) {
        return JSONPath.get(model, "$.services[*].methods[*][?(@.returnTypeIsArray == true && @.options.pageable != true)].returnType", List.<String>of());
    }

    public static List<String> findDependentEntities(Map<String, Object> model, String entityName) {
        List<String> dependentEntities = new ArrayList<>();
        dependentEntities.add(entityName);
        var allEntitiesAndEnums = JSONPath.get(model, "$.allEntitiesAndEnums", Map.of());
        var fields = JSONPath.get(model, "$.allEntitiesAndEnums." + entityName + ".fields", Map.<String, Map>of());
        for (var field : fields.values()) {
            var fieldType = (String) field.get("type");
            if(allEntitiesAndEnums.containsKey(fieldType)) {
                dependentEntities.add(fieldType);
                if(!dependentEntities.contains(fieldType)) {
                    dependentEntities.addAll(findDependentEntities(model, fieldType));
                }
            }
        }
        var toRelationships = JSONPath.get(model, "$.relationships[*][*][?(@.from=='" + entityName + "')].to", List.<String>of());
        for (var toRelationship : toRelationships) {
            dependentEntities.add(toRelationship);
            if(!dependentEntities.contains(toRelationship)) {
                dependentEntities.addAll(findDependentEntities(model, toRelationship));
            }
        }
        return dependentEntities;
    }

    public static List<String> findDependentEntities(Map<String, Object> model, List<String> entityNames) {
        System.out.println("entityNames = " + entityNames);
        return entityNames.stream().map(entityName -> findDependentEntities(model, entityName)).peek(System.out::println).flatMap(List::stream).collect(Collectors.toList());
    }

    public static List<String> findMethodParameterAndReturnTypes(Map<String, Object> model) {
        var entities = JSONPath.get(model, "$.services[*].methods[*]['parameter','returnType']");
        return new ArrayList(new HashSet(JSONPath.get(entities, "$[*][*]")));
    }
}
