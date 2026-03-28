package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

final class DiagramModelResolver {

    private DiagramModelResolver() {
    }

    static Map<String, Object> resolveAggregate(Map<String, Object> zdlModel, Object aggregateOrName) {
        if (aggregateOrName == null) {
            return null;
        }
        if (aggregateOrName instanceof Map) {
            var aggregate = (Map<String, Object>) aggregateOrName;
            return isNamedAggregate(aggregate) ? aggregate : null;
        }
        if (aggregateOrName instanceof String name && zdlModel != null) {
            var aggregate = (Map<String, Object>) JSONPath.get(zdlModel, "$.aggregates." + name);
            return isNamedAggregate(aggregate) ? aggregate : null;
        }
        return null;
    }

    static Map<String, Object> resolveAggregateRootEntity(Map<String, Object> zdlModel, Map<String, Object> aggregate) {
        if (zdlModel == null || aggregate == null) {
            return null;
        }
        String aggregateRoot = (String) aggregate.get("aggregateRoot");
        String aggregateName = (String) aggregate.get("name");
        String entityName = aggregateRoot != null ? aggregateRoot : aggregateName;
        if (entityName == null) {
            return null;
        }
        var entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + entityName);
        if (entity == null) {
            entity = (Map<String, Object>) JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + entityName);
        }
        return entity;
    }

    static Map<String, Object> resolveDiagramEntity(Map<String, Object> zdlModel, Object aggregateOrEntityName) {
        if (zdlModel == null || aggregateOrEntityName == null) {
            return null;
        }
        Map<String, Object> aggregate = resolveAggregate(zdlModel, aggregateOrEntityName);
        if (aggregate != null) {
            return resolveAggregateRootEntity(zdlModel, aggregate);
        }
        return (Map<String, Object>) JSONPath.get(zdlModel, "$.entities." + aggregateOrEntityName);
    }

    static boolean isNamedAggregate(Map<String, Object> aggregate) {
        if (aggregate == null) {
            return false;
        }
        String name = JSONPath.get(aggregate, "$.name", (String) null);
        String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", (String) null);
        return StringUtils.isNotBlank(name) && StringUtils.isNotBlank(aggregateRoot);
    }
}
