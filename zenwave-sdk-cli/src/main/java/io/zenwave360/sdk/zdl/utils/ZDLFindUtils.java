package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ZDLFindUtils {

    public static boolean is(Map<String, Object> model, String... annotations) {
        String annotationsFilter = Arrays.stream(annotations).map(a -> "@." + a).collect(Collectors.joining(" || "));
        return !(JSONPath.get(model, "$.entity.options[?(" + annotationsFilter + ")]", List.of())).isEmpty();
    }

    public static List<String> findAllServiceFacingEntities(Map<String, Object> model) {
        var serviceEntities = ZDLFindUtils.findMethodParameterAndReturnTypes(model);
        return ZDLFindUtils.findDependentEntities(model, serviceEntities);
    }

    public static List<Map> naturalIdFields(Map<String, Object> entity) {
        return JSONPath.get(entity, "$.fields[*][?(@.options.naturalId)]", List.<Map>of());
    }

    public static Map<String, Object> methodEntity(Map<String, Object> method, Map zdl) {
        var returnType = (String) method.get("returnType");
        var service = JSONPath.get(zdl, "$.services." + method.get("serviceName"));
        var aggregates = JSONPath.get(service, "aggregates", Collections.emptyList());
        if(aggregates.size() == 1 && StringUtils.equals(returnType, aggregates.get(0).toString())) {
            return JSONPath.get(zdl, "$.entities." + returnType);
        }
        return null;
    }

    public static List<Map<String, Object>> methodsWithEvents(Map<String, Object> model) {
        return JSONPath.get(model, "$.services[*].methods[*][?(@.withEvents.length() > 0)]", Collections.<Map<String, Object>>emptyList());
    }

    public static List<String> findAllPaginatedEntities(Map<String, Object> model) {
        return JSONPath.get(model, "$.services[*].methods[*][?(@.options.paginated == true)].returnType", List.<String>of());
    }

    public static boolean isMethodPaginated(Map<String, Object> method) {
        return JSONPath.get(method, "$.options.paginated", false);
    }

    public static List<String> findAllEntitiesReturnedAsList(Map<String, Object> model) {
        return JSONPath.get(model, "$.services[*].methods[*][?(@.returnTypeIsArray == true && @.options.paginated != true)].returnType", List.<String>of());
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
        var entities = new ArrayList<String>();
        entities.addAll(JSONPath.get(model, "$.services[*].methods[*]['parameter']"));
        entities.addAll(JSONPath.get(model, "$.services[*].methods[*]['returnType']"));
        return entities.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    public static String findServiceName(String entityName, Map<String, Object> model) {
        var entity = (Map) JSONPath.get(model, "$.allEntitiesAndEnums." + entityName, Map.of());
        var allServices = JSONPath.get(model, "$.services[*]", List.<Map>of());
        if ("entities".equals(entity.get("type"))) {
            var aggregateService = _findServiceName(allServices, entityName, "$.aggregates");
            var parameterService = _findServiceName(allServices, entityName, "$.methods[*].parameter");
            return ObjectUtils.firstNonNull(aggregateService, parameterService);
        }
        if ("inputs".equals(entity.get("type"))) {
            return _findServiceName(allServices, entityName, "$.methods[*].parameter");
        }
        if ("outputs".equals(entity.get("type"))) {
            return _findServiceName(allServices, entityName, "$.methods[*].returnType");
        }
        return null;
    }

    private static String _findServiceName(List<Map> services, String entityName, String jsonPath) {
        return services.stream().filter(service -> JSONPath.get(service, jsonPath, List.of()).contains(entityName))
                .map(service -> (String) service.get("name")).findFirst().orElse(null);
    }

    public static Map<String, Object> findServiceMethod(String operationId, Map<String, Object> model) {
        var methods = JSONPath.get(model, "$.services[*].methods[*]", List.<Map>of());
        return methods.stream()
                .filter(method -> operationId.equals(JSONPath.get(method, "$.name")) || operationId.equals(JSONPath.get(method, "$.options[*].operationId"))
                ).findFirst().orElse(null);
    }

    public static boolean isAggregateRoot(Map zdl, String entityName) {
        var aggregateNames = JSONPath.get(zdl, "$.aggregates[*][?(@.aggregateRoot == '" + entityName + "')].name", List.of());
        return !aggregateNames.isEmpty();
    }

    public static Set<String> aggregateEvents(Map<String, Object> aggregate) {
        var allEvents = new HashSet<String>();
        var methods = JSONPath.get(aggregate, "$.commands[*]", List.<Map>of());
        for (var method : methods) {
            allEvents.addAll(methodEventsFlatList(method));
        }
        return allEvents;
    }

    public static  List<String> methodEventsFlatList(Map<String, Object> method) {
        var events = (List) method.getOrDefault("withEvents", List.of());
        List<String> allEvents = new ArrayList<>();
        for (Object event : events) {
            if(event instanceof String) {
                allEvents.add((String) event);
            } else if(event instanceof List) {
                allEvents.addAll((Collection<? extends String>) event);
            }
        }
        return allEvents;
    }


    public static List<Map<String, Object>> findAggregateCommandsForMethod(Map zdl, Map<String, Object> method) {
        var serviceAggregateNames = JSONPath.get(zdl, "$.services." + method.get("serviceName") + ".aggregates", List.<String>of());
        var returnType = JSONPath.get(method, "$.returnType");

        String aggregateName = (String) method.get("aggregate");
        String entityName = (String) method.get("entity");
        String commandName = findAggregateCommand(zdl, method, aggregateName);
        String crudMethod = findCrudMethod(zdl, method, entityName);

        return List.of(methodAggregateCommand(zdl, aggregateName, commandName, entityName, crudMethod));
    }

    private static Map<String, Object> methodAggregateCommand(Map zdl, String aggregateName, String commandName, String entityName, String crudMethod) {
        var aggregate = JSONPath.get(zdl, "$.allEntitiesAndEnums." + aggregateName);
        var entity = JSONPath.get(zdl, "$.allEntitiesAndEnums." + entityName);
        var command = JSONPath.get(aggregate, "$.commands." + commandName);
        return Maps.of("aggregate", aggregate, "entity", entity, "command", command, "crudMethod", crudMethod);
    }

    private static String findAggregateCommand(Map zdl, Map method, String aggregate) {
        return JSONPath.get(zdl, "$.allEntitiesAndEnums." + aggregate + ".commands." + method.get("name") + ".name");
    }

    private static String findCrudMethod(Map zdl, Map method, String entityName) {
        var entity = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + entityName, Map.of());
        return findCrudMethod(method, entity);
    }

    private static String findCrudMethod(Map method, Map entity) {
        var entityName = (String) entity.get("name");
        var entityNamePlural = (String) entity.get("classNamePlural");
        var methodName = (String) method.get("name");
        var isArray = "true".equals(String.valueOf(method.get("returnTypeIsArray")));
        var isOptional = "true".equals(String.valueOf(method.get("returnTypeIsOptional")));
        var entityMethodSuffix = isArray ? entityNamePlural : entityName;

        for (String crudPrefix : List.of("create", "delete", "get")) {
            var isCrudMethod = methodName.equals(crudPrefix + entityMethodSuffix);
            if (isCrudMethod) {
                return crudPrefix + entityMethodSuffix;
            }
        }

        return null;
    }
}
