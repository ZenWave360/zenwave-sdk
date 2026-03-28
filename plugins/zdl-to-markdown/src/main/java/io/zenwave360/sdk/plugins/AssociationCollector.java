package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class AssociationCollector {

    private AssociationCollector() {
    }

    static List<Map<String, Object>> collectEntityAssociations(Map<String, Object> entity, Map<String, Object> zdlModel) {
        if (entity == null || zdlModel == null) {
            return List.of();
        }
        var associations = new ArrayList<Map<String, Object>>();
        var visitedEntities = new LinkedHashSet<String>();
        var visitedLinks = new LinkedHashSet<String>();
        collectEntityAssociations(entity, zdlModel, associations, visitedEntities, visitedLinks);
        return associations;
    }

    static List<Map<String, Object>> collectCollectionAssociations(Collection<Map<String, Object>> entities, Map<String, Object> zdlModel) {
        if (entities == null || zdlModel == null) {
            return List.of();
        }
        var associations = new ArrayList<Map<String, Object>>();
        var visitedEntities = new LinkedHashSet<String>();
        var visitedLinks = new LinkedHashSet<String>();
        for (var entity : entities) {
            collectEntityAssociations(entity, zdlModel, associations, visitedEntities, visitedLinks);
        }
        return associations;
    }

    static List<Map<String, Object>> uniqueAssociationTargets(Collection<Map<String, Object>> associations, Object excludedNames) {
        if (associations == null) {
            return List.of();
        }
        Set<String> excluded = normalizeExcludedNames(excludedNames);
        var unique = new LinkedHashMap<String, Map<String, Object>>();
        for (var association : associations) {
            if (association == null) {
                continue;
            }
            var entity = (Map<String, Object>) association.get("entity");
            String name = JSONPath.get(entity, "$.name", (String) null);
            if (name != null && !excluded.contains(name)) {
                unique.putIfAbsent(name, entity);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static void collectEntityAssociations(Map<String, Object> entity,
                                                  Map<String, Object> zdlModel,
                                                  List<Map<String, Object>> associations,
                                                  Set<String> visitedEntities,
                                                  Set<String> visitedLinks) {
        if (entity == null) {
            return;
        }
        String sourceName = JSONPath.get(entity, "$.name", (String) null);
        if (sourceName == null || !visitedEntities.add(sourceName)) {
            return;
        }

        var compositions = JSONPath.get(entity, "fields[*][?(@.isEntity==true || @.isEnum==true)].type", List.<String>of());
        for (String targetName : compositions) {
            Map<String, Object> target = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + targetName);
            addAssociation(sourceName, "*--", target, associations, visitedLinks);
            if (target != null && !"enums".equals(JSONPath.get(target, "type", ""))) {
                collectEntityAssociations(target, zdlModel, associations, visitedEntities, visitedLinks);
            }
        }

        String sourceBoundary = aggregateBoundaryName(zdlModel, sourceName);
        var relationships = JSONPath.get(entity, "relationships[?(@.fieldName)]", List.<Map<String, Object>>of());
        for (Map<String, Object> relationship : relationships) {
            if (!Boolean.TRUE.equals(JSONPath.get(relationship, "$.ownerSide", false))) {
                continue;
            }
            String targetName = JSONPath.get(relationship, "$.otherEntityName", (String) null);
            if (targetName == null) {
                continue;
            }
            Map<String, Object> target = JSONPath.get(zdlModel, "$.entities." + targetName);
            String targetBoundary = aggregateBoundaryName(zdlModel, targetName);
            boolean sameBoundary = Objects.equals(sourceBoundary, targetBoundary);
            addAssociation(sourceName, sameBoundary ? "o--" : "..>", target, associations, visitedLinks);
            if (sameBoundary) {
                collectEntityAssociations(target, zdlModel, associations, visitedEntities, visitedLinks);
            }
        }
    }

    private static String aggregateBoundaryName(Map<String, Object> zdlModel, String entityName) {
        if (zdlModel == null || entityName == null) {
            return null;
        }
        if (Boolean.TRUE.equals(JSONPath.get(zdlModel, "$.entities." + entityName + ".options.aggregate", false))) {
            return entityName;
        }
        for (String aggregateRoot : aggregateBoundaryRoots(zdlModel)) {
            if (isEntityWithinAggregateBoundary(zdlModel, aggregateRoot, entityName, new LinkedHashSet<>())) {
                return aggregateRoot;
            }
        }
        return null;
    }

    private static List<String> aggregateBoundaryRoots(Map<String, Object> zdlModel) {
        var roots = new LinkedHashSet<String>();
        var aggregates = (Map<String, Object>) JSONPath.get(zdlModel, "$.aggregates", Map.<String, Object>of());
        for (Object value : aggregates.values()) {
            if (!(value instanceof Map<?, ?> aggregate)) {
                continue;
            }
            String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", (String) null);
            if (aggregateRoot != null) {
                roots.add(aggregateRoot);
            }
        }
        var entities = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities", Map.<String, Object>of());
        for (Object value : entities.values()) {
            if (!(value instanceof Map<?, ?> entity)) {
                continue;
            }
            if (Boolean.TRUE.equals(JSONPath.get(entity, "$.options.aggregate", false))) {
                String entityName = JSONPath.get(entity, "$.name", (String) null);
                if (entityName != null) {
                    roots.add(entityName);
                }
            }
        }
        return new ArrayList<>(roots);
    }

    private static boolean isEntityWithinAggregateBoundary(Map<String, Object> zdlModel,
                                                           String currentEntityName,
                                                           String targetEntityName,
                                                           Set<String> visited) {
        if (zdlModel == null || currentEntityName == null || targetEntityName == null || !visited.add(currentEntityName)) {
            return false;
        }
        if (currentEntityName.equals(targetEntityName)) {
            return true;
        }
        var entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + currentEntityName);
        if (entity == null) {
            return false;
        }
        var nestedTypes = new LinkedHashSet<String>();
        nestedTypes.addAll(JSONPath.get(entity, "fields[*][?(@.isEntity==true)].type", List.<String>of()));
        nestedTypes.addAll(JSONPath.get(entity, "relationships[?(@.fieldName)].otherEntityName", List.<String>of()));
        for (String nestedType : nestedTypes) {
            if (targetEntityName.equals(nestedType)) {
                return true;
            }
            if (Boolean.TRUE.equals(JSONPath.get(zdlModel, "$.entities." + nestedType + ".options.aggregate", false))) {
                continue;
            }
            if (isEntityWithinAggregateBoundary(zdlModel, nestedType, targetEntityName, visited)) {
                return true;
            }
        }
        return false;
    }

    private static void addAssociation(String sourceName,
                                       String linkType,
                                       Map<String, Object> target,
                                       List<Map<String, Object>> associations,
                                       Set<String> visitedLinks) {
        if (target == null) {
            return;
        }
        String targetName = JSONPath.get(target, "$.name", (String) null);
        if (targetName == null) {
            return;
        }
        String key = sourceName + "|" + linkType + "|" + targetName;
        if (visitedLinks.add(key)) {
            associations.add(Map.of("source", sourceName, "linkType", linkType, "entity", target));
        }
    }

    private static Set<String> normalizeExcludedNames(Object excludedNames) {
        if (excludedNames == null) {
            return Set.of();
        }
        if (excludedNames instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of(excludedNames.toString());
    }
}
