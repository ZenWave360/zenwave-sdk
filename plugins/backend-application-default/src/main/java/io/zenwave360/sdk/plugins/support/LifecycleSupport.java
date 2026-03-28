package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;

public final class LifecycleSupport {

    private LifecycleSupport() {
    }

    public static String lifecycleFieldType(Map<String, Object> zdl, Map<String, Object> aggregate) {
        if (aggregate == null) {
            return null;
        }
        var lifecycle = (Map<String, Object>) JSONPath.get(aggregate, "lifecycle");
        if (lifecycle == null) {
            return null;
        }
        var rootEntity = aggregateRootEntity(zdl, aggregate);
        if (rootEntity == null) {
            return null;
        }
        var field = (String) lifecycle.get("field");
        return JSONPath.get(rootEntity, "$.fields." + field + ".type");
    }

    public static boolean hasLifecycle(Map<String, Object> zdl, Map<String, Object> aggregate) {
        return lifecycleFieldType(zdl, aggregate) != null;
    }

    public static boolean hasStateTransitions(Map<String, Object> aggregate) {
        if (aggregate == null) {
            return false;
        }
        var commands = JSONPath.get(aggregate, "$.commands[*]", List.<Map<String, Object>>of());
        return commands.stream().anyMatch(cmd -> JSONPath.get(cmd, "$.transition.from") != null || JSONPath.get(cmd, "$.transition.to") != null);
    }

    public static String transitionMethodName(Map<String, Object> method) {
        return "ensureCan" + asJavaTypeName((String) method.get("name"));
    }

    public static String aggregateTransitionsClassName(Map<String, Object> aggregate) {
        return asJavaTypeName((String) aggregate.get("aggregateRoot")) + "AggregateTransitions";
    }

    public static Map<String, Object> aggregateRootEntity(Map<String, Object> zdl, Map<String, Object> aggregate) {
        if (aggregate == null) {
            return null;
        }
        return JSONPath.get(zdl, "$.allEntitiesAndEnums." + aggregate.get("aggregateRoot"));
    }

    public static List<Map<String, Object>> aggregateTransitionMethods(Map<String, Object> zdl, Map<String, Object> aggregate) {
        if (aggregate == null || !hasLifecycle(zdl, aggregate)) {
            return Collections.emptyList();
        }
        var commands = JSONPath.get(aggregate, "$.commands[*]", List.<Map<String, Object>>of());
        return commands.stream()
                .filter(command -> JSONPath.get(command, "$.transition.from") != null)
                .toList();
    }

    public static String commandFromStatesSignature(Map<String, Object> zdl,
                                                    Map<String, Object> command,
                                                    Map<String, Object> aggregate) {
        var fromStates = (List<String>) JSONPath.get(command, "$.transition.from");
        if (fromStates == null || fromStates.isEmpty()) {
            return "";
        }
        var fieldType = lifecycleFieldType(zdl, aggregate);
        return fromStates.stream()
                .map(state -> fieldType + "." + state)
                .collect(Collectors.joining(", "));
    }

    public static boolean hasEntityLifecycle(Map<String, Object> entity) {
        return entity != null && JSONPath.get(entity, "lifecycle") != null;
    }

    public static String entityLifecycleFieldType(Map<String, Object> entity) {
        var lifecycle = (Map<String, Object>) JSONPath.get(entity, "lifecycle");
        if (lifecycle == null) {
            return "";
        }
        var field = (String) lifecycle.get("field");
        return JSONPath.get(entity, "$.fields." + field + ".type");
    }

    public static String entityCommandFromStatesSignature(Map<String, Object> method, Map<String, Object> entity) {
        var fromStates = (List<String>) JSONPath.get(method, "$.transition.from");
        if (fromStates == null || fromStates.isEmpty()) {
            return "";
        }
        var fieldType = entityLifecycleFieldType(entity);
        return fromStates.stream()
                .map(state -> fieldType + "." + state)
                .collect(Collectors.joining(", "));
    }

    public static String entityServiceTransitionsClassName(Map<String, Object> entity) {
        return asJavaTypeName((String) entity.get("name")) + "Transitions";
    }

    public static List<Map<String, Object>> entityServiceTransitionMethods(Map<String, Object> zdl, Map<String, Object> entity) {
        if (entity == null || !hasEntityLifecycle(entity)) {
            return Collections.emptyList();
        }
        var services = JSONPath.get(zdl, "$.services[*]", List.<Map<String, Object>>of());
        var entityName = (String) entity.get("name");
        var methodsByName = new LinkedHashMap<String, Map<String, Object>>();
        for (var service : services) {
            var methods = JSONPath.get(service, "$.methods[*]", List.<Map<String, Object>>of());
            for (var method : methods) {
                if (entityName.equals(method.get("entity")) && JSONPath.get(method, "$.transition.from") != null) {
                    methodsByName.putIfAbsent((String) method.get("name"), method);
                }
            }
        }
        return new ArrayList<>(methodsByName.values());
    }

    public static boolean serviceHasEntityStateTransitions(Map<String, Object> service) {
        var methods = JSONPath.get(service, "$.methods[*]", List.<Map<String, Object>>of());
        for (var method : methods) {
            if (JSONPath.get(method, "$.transition.from") != null || JSONPath.get(method, "$.transition.to") != null) {
                return true;
            }
        }
        return false;
    }
}
