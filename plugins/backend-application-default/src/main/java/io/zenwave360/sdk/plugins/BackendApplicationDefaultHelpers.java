package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.plugins.support.CrudMethodSupport;
import io.zenwave360.sdk.plugins.support.LifecycleSupport;
import io.zenwave360.sdk.plugins.support.MapperSupport;
import io.zenwave360.sdk.plugins.support.RepositoryIdSupport;
import io.zenwave360.sdk.plugins.support.ServiceEventPlanner;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;
import io.zenwave360.sdk.plugins.support.MethodBodyPlan;
import io.zenwave360.sdk.plugins.support.ServiceMethodBodyPlanner;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.utils.JSONPath;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;
import static io.zenwave360.sdk.zdl.utils.ZDLFindUtils.is;
import static java.lang.String.format;
import static java.lang.String.join;

public class BackendApplicationDefaultHelpers {

    private final BackendApplicationDefaultGenerator generator;

    public BackendApplicationDefaultHelpers(BackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public String logMethodCall(Map method, Options options) {
        var zdl = options.get("zdl");
        var methodName = (String) method.get("name");
        var parameters = ZDLJavaSignatureUtils.methodParametersCallSignature(method, (Map) zdl);
        var parameterPlaceHolders = Arrays.stream(parameters.split(", ")).map(p -> "{}").collect(Collectors.joining(" "));
        if(parameters.isEmpty()) {
            return String.format("log.debug(\"Request %s\");", methodName);
        }
        return String.format("log.debug(\"Request %s: %s\", %s);", methodName, parameterPlaceHolders, parameters);
    }

    public Map findAggregateCommandsForMethod(Map method, Options options) {
        var zdl = options.get("zdl");
        var aggregatesCommandsForMethod = ZDLFindUtils.findAggregateCommandsForMethod((Map) zdl, method);
        if(aggregatesCommandsForMethod.isEmpty()) {
            return Map.of("templateFile", "aggregates-void-methodBody");
        }
        if(aggregatesCommandsForMethod.size() == 1) {
            var aggregate = JSONPath.get(aggregatesCommandsForMethod, "$[0].aggregate");
            var entity = JSONPath.get(aggregatesCommandsForMethod, "$[0].entity");
            var command = JSONPath.get(aggregatesCommandsForMethod, "$[0].command");
            var crudMethod = JSONPath.get(aggregatesCommandsForMethod, "$[0].crudMethod");
            if(aggregate != null && command != null) {
                return Map.of("templateFile", "aggregates-commands-methodBody", "aggregatesCommandsForMethod", aggregatesCommandsForMethod);
            }
            if (aggregate != null && crudMethod != null) {
                // TODO: return Map.of("templateFile", "aggregates-crud-methodBody", "aggregatesCommandsForMethod", aggregatesCommandsForMethod);
            }
            if(entity != null && crudMethod != null) {
                return Map.of("templateFile", "entities-crud-methodBody", "entity", entity, "crudMethod", crudMethod);
            }
            if(entity != null) {
                return Map.of("templateFile", "entities-methodBody", "entity", entity);
            }
            return Map.of("templateFile", "entities-methodBody");
        }
        return Map.of("templateFile", "aggregates-commands-methodBody", "aggregatesCommandsForMethod", aggregatesCommandsForMethod);
    }

    @Deprecated
    public boolean isCrudMethod(String crudMethodPrefix, Options options) {
        var entity = (Map<String, Object>) options.hash("entity");
        var method = (Map<String, Object>) options.hash("method");
        var isCrudMethod = CrudMethodSupport.isCrudMethod(crudMethodPrefix, entity, method);
        if(isCrudMethod) {
            method.put("isCrudMethod", isCrudMethod);
        }
        return isCrudMethod;
    }

    public List<Map> serviceAggregates(Map service, Options options) {
        var zdl = options.get("zdl");
        var aggregateNames = JSONPath.get(service, "aggregates", Collections.emptyList());
        return aggregateNames.stream()
                .map(aggregateName -> JSONPath.get(zdl, "$.allEntitiesAndEnums." + aggregateName, Map.of()))
                .filter(aggregate -> "aggregates".equals(aggregate.get("type")))
                .collect(Collectors.toList());
    }

    public boolean includeDomainEvents(Object service, Options options) {
        var zdl = options.get("zdl");
        var hasAggregateEvents = !JSONPath.get(zdl, "$.aggregates[*].commands[*].withEvents", List.of()).isEmpty();
        var needEventBus = needsEventBus((Map) service, options);
        return hasAggregateEvents || needEventBus;
    }

    public List<Map> aggregateEvents(Map<String, Object> aggregate, Options options) {
        var zdl = options.get("zdl");
        return ZDLFindUtils.aggregateEvents(aggregate).stream().map(event -> (Map) JSONPath.get(zdl, "$.events." + event)).toList();
    }

    public Collection<String> findAggregateInputs(Map aggregate, Options options) {
        return new HashSet<>(JSONPath.get(aggregate, "$.commands[*].parameter", List.of()));
    }

    public Collection<Map> findAggregates(Collection<Map> entities, Options options) {
        return entities.stream().filter(entity -> isAggregate((String) entity.get("name"), options)).collect(Collectors.toList());
    }

    public boolean isAggregate(String entityName, Options options) {
        var zdl = options.get("zdl");
        var isAggregateRoot = JSONPath.get(zdl, "$.entities." + entityName + "[?(@.options.aggregate == true)]", List.of());
        var isLifecycleEntity = JSONPath.get(zdl, "$.entities." + entityName + "[?(@.options.lifecycle)]", List.of());
        var aggregateName = findEntityAggregate(entityName, options);
        return !isAggregateRoot.isEmpty() || !isLifecycleEntity.isEmpty() || aggregateName != null;
    }

    public String findEntityAggregate(String entityName, Options options) {
        var zdl = options.get("zdl");
        var aggregateNames = JSONPath.get(zdl, "$.aggregates[*][?(@.aggregateRoot == '" + entityName + "')].name", List.of());
        return aggregateNames.isEmpty()? null : (String) aggregateNames.get(0);
    }

    public List<Map> naturalIdFields(Map entity, Options options) {
        return ZDLFindUtils.naturalIdFields(entity);
    }

    public String naturalIdsRepoMethodSignature(Map entity, Options options) {
        return ZDLJavaSignatureUtils.naturalIdsRepoMethodSignature(entity);
    }

    public String naturalIdsRepoMethodCallSignature(Map entity, Options options) {
        return ZDLJavaSignatureUtils.naturalIdsRepoMethodCallSignature(entity);
    }

    public String findById(Map method, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return RepositoryIdSupport.findById(zdl, method);
    }

    public String idFieldInitialization(Map method, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return RepositoryIdSupport.idFieldInitialization(zdl, method, generator.getIdJavaType());
    }

    public String idParamsCallSignature(Map method, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return RepositoryIdSupport.idParamsCallSignature(zdl, method);
    }

    public Map<String, Object> serviceParameterEntityPairs(Map service, Options options) {
        var zdl = (Map) options.get("zdl");
        var inputDTOSuffix = (String) options.get("inputDTOSuffix");
        return MapperSupport.serviceParameterEntityPairs(zdl, service, inputDTOSuffix);
    }

    public Map<String, Object> serviceEntityReturnTypePairs(Map service, Options options) {
        var zdl = (Map) options.get("zdl");
        return MapperSupport.serviceEntityReturnTypePairs(zdl, service);
    }

    public String methodParameterType(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParameterType(method, zdl);
    }

    public String methodParametersSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParametersSignature(generator.getIdJavaType(), method, zdl);
    }

    public String methodParametersCallSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParametersCallSignature(method, zdl);
    }

    public MethodBodyPlan planEntityMethodBody(Map<String, Object> aggregateCommandsForMethod,
                                               Map<String, Object> method,
                                               Options options) {
        var entity = aggregateCommandsForMethod != null
                ? (Map<String, Object>) aggregateCommandsForMethod.get("entity")
                : null;
        var returnEntity = methodReturnEntity(method, options);
        return ServiceMethodBodyPlanner.planEntityMethodBody(method, entity, returnEntity);
    }

    public List<Map> methodParameterFields(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        var parameterType = (String) method.get("parameter");
        if (parameterType == null) {
            return List.of();
        }
        return JSONPath.get(zdl, "$.allEntitiesAndEnums." + parameterType + ".fields[*]", List.<Map>of());
    }

    public boolean includeEmitEventsImplementation(Map service, Options options) {
        if(service == null) {
            return generator.includeEmitEventsImplementation;
        }
        return generator.includeEmitEventsImplementation && !JSONPath.get(service, "methods[*].withEvents[*]", List.of()).isEmpty();
    }

    public List<Map<String, Object>> methodEvents(Map<String, Object> method, Options options) {
        var eventNames = ZDLFindUtils.methodEventsFlatList(method);
        var zdl = (Map) options.get("zdl");
        return (List) eventNames.stream().map(eventName -> JSONPath.get(zdl, "$.events." + eventName)).collect(Collectors.toList());
    }

    public List<Map<String, Object>> methodsWithEvents(Map<String, Object> zdl, Options options) {
        return ZDLFindUtils.methodsWithEvents(zdl); // TODO review usages
    }

    public Collection<Map> domainEventsWithAsyncapiAnnotation(Map<String, Object> zdl, Options options) {
        var domainEventNames = JSONPath.get(zdl, "$.aggregates[*].commands[*].withEvents", List.of()).stream()
                .flatMap(e -> e instanceof List? ((List<?>) e).stream() : Stream.of(e))
                .collect(Collectors.toSet());
        return domainEventNames.stream().map(eventName -> (Map) JSONPath.get(zdl, "$.events." + eventName))
                .filter(event -> JSONPath.get(event, "options.asyncapi") != null)
                .toList();
    }

    public Collection<Map<String, Object>> listOfPairEventEntity(Map<String, Object> zdl, Options options) {
        var result = new HashMap<String, Object>();
        var methods = ZDLFindUtils.methodsWithEvents(zdl);
        for (Map<String, Object> method : methods) {
            var entity = methodEntity(method, options);
            var methodEvents = methodEvents(method, options);
            for (Map<String, Object> event : methodEvents) {
                var isAsyncApi = JSONPath.get(event, "options.asyncapi") != null;
                if (entity == null) {
                    var key = JSONPath.get(event, "name") + "-method-" + ZDLJavaSignatureUtils.methodParametersCallSignature(method, zdl);
                    result.put(key, Map.of("event", event, "method", method, "isAsyncApi", isAsyncApi));
                } else {
                    var key = JSONPath.get(event, "name") + "-" + entity.get("name");
                    result.put(key, Map.of("event", event, "entity", entity, "isAsyncApi", isAsyncApi));
                    if (isAsyncApi) {
                        result.putAll(extraMappingsFromEventFields(event, entity, zdl));
                    }
                }
            }
        }
        return (Collection) result.values();
    }

    private Map<String, Map<String, Object>> extraMappingsFromEventFields(Map<String, Object> target, Map<String, Object> source, Map zdl) {
        var targetFields = (List<Map>) JSONPath.get(target, "$.fields[*][?(@.isComplexType == true)]");
        var result = new HashMap<String, Map<String, Object>>();
        for (Map<String, Object> targetField : targetFields) {
            var targetEntity = JSONPath.get(zdl, "$.allEntitiesAndEnums." + targetField.get("type"));
            if (targetEntity != null && !JSONPath.get(targetEntity, "options.skip", false)) {
                var key = targetField.get("type") + "-" + targetField.get("type");
                result.put(key, Map.of("event", targetEntity, "entity", targetEntity, "isAsyncApi", true));
            }
        }
        return result;
    }

    public String operationNameForEvent(String eventName, Options options) {
        return  "on" + asJavaTypeName(eventName);
    }

    /**
     * Returns the result record type name for an aggregate command (e.g., "PlaceOrderResult").
     */
    public String commandResultTypeName(Map<String, Object> command, Options options) {
        return asJavaTypeName((String) command.get("name")) + "Result";
    }

    /**
     * Returns the Java type name for the lifecycle status field of an aggregate.
     * Looks up the entity and finds the field type for the field defined in lifecycle.
     */
    public String lifecycleFieldType(Map<String, Object> aggregate, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.lifecycleFieldType(zdl, aggregate);
    }

    /**
     * Returns true if the aggregate has a lifecycle (state machine) defined.
     */
    public boolean hasLifecycle(Map<String, Object> aggregate, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.hasLifecycle(zdl, aggregate);
    }

    /**
     * Returns true if any command in the aggregate has from/to state transitions.
     */
    public boolean hasStateTransitions(Map<String, Object> aggregate, Options options) {
        return LifecycleSupport.hasStateTransitions(aggregate);
    }

    public String transitionMethodName(Map<String, Object> method, Options options) {
        return LifecycleSupport.transitionMethodName(method);
    }

    public String aggregateTransitionsClassName(Map<String, Object> aggregate, Options options) {
        return LifecycleSupport.aggregateTransitionsClassName(aggregate);
    }

    public Map<String, Object> aggregateRootEntity(Map<String, Object> aggregate, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.aggregateRootEntity(zdl, aggregate);
    }

    public List<Map<String, Object>> aggregateTransitionMethods(Map<String, Object> aggregate, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.aggregateTransitionMethods(zdl, aggregate);
    }

    public boolean hasAggregateTransitionMethods(Map<String, Object> aggregate, Options options) {
        return !aggregateTransitionMethods(aggregate, options).isEmpty();
    }

    /**
     * Returns the requireState call arguments for a command's from states, e.g.
     * "OrderStatus.DRAFT, OrderStatus.PLACED"
     */
    public String commandFromStatesSignature(Map<String, Object> command, Map<String, Object> aggregate, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.commandFromStatesSignature(zdl, command, aggregate);
    }

    // ==================== Entity Lifecycle Helpers ====================

    /**
     * Returns true if the entity has a @lifecycle annotation (for non-aggregate entities).
     */
    public boolean hasEntityLifecycle(Map<String, Object> entity, Options options) {
        return LifecycleSupport.hasEntityLifecycle(entity);
    }

    /**
     * Returns the Java enum type name for the lifecycle status field of an entity.
     * Looks up the field type from the entity's own field map.
     */
    public String entityLifecycleFieldType(Map<String, Object> entity, Options options) {
        return LifecycleSupport.entityLifecycleFieldType(entity);
    }

    /**
     * Returns the requireState call arguments for a service method's from states on an entity,
     * e.g. "OrderStatus.DRAFT, OrderStatus.PLACED"
     */
    public String entityCommandFromStatesSignature(Map<String, Object> method, Map<String, Object> entity, Options options) {
        return LifecycleSupport.entityCommandFromStatesSignature(method, entity);
    }

    public String entityServiceTransitionsClassName(Map<String, Object> entity, Options options) {
        return LifecycleSupport.entityServiceTransitionsClassName(entity);
    }

    public String transitionNaturalIdExpression(Map<String, Object> entity, Options options) {
        if (entity == null) {
            return "null";
        }
        var fields = naturalIdFields(entity, options);
        if (fields == null || fields.isEmpty()) {
            return "null";
        }
        return fields.stream()
                .map(field -> "\"" + field.get("name") + "=\" + entity.get" + asJavaTypeName((String) field.get("name")) + "()")
                .collect(Collectors.joining(" + \", \" + "));
    }

    public List<Map<String, Object>> entityServiceTransitionMethods(Map<String, Object> entity, Options options) {
        var zdl = (Map<String, Object>) options.get("zdl");
        return LifecycleSupport.entityServiceTransitionMethods(zdl, entity);
    }

    public boolean hasEntityServiceTransitionMethods(Map<String, Object> entity, Options options) {
        return !entityServiceTransitionMethods(entity, options).isEmpty();
    }

    /**
	 * Returns true if the service has at least one method with from/to state transitions.
	 *
	 * Used to decide whether to generate the generic {@code requireState(...)} helper method
	 * in the ServiceImpl templates.
     */
    public boolean serviceHasEntityStateTransitions(Map<String, Object> service, Options options) {
		return LifecycleSupport.serviceHasEntityStateTransitions(service);
    }

    /**
     * For a service method, returns the list of event publishing instructions.
     * Each entry contains: eventName, instanceName, producerCall, hasProducer, isAsyncApi, producedByAggregate.
     */
    public List<Map<String, Object>> serviceMethodEventPublications(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ServiceEventPlanner.serviceMethodEventPublications(zdl, method);
    }

    public List<Map> methodPolicies(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        var policies = JSONPath.get(method, "optionsList[?(@.name == 'policy')].value", Collections.emptyList());
        return policies.stream().map(policy -> JSONPath.get(zdl, "$.policies." + policy, Collections.emptyMap())).collect(Collectors.toList());
    }

    public String mapperInputSignature(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.mapperInputSignature(inputType, zdl);
    }

    public String mapperInputCallSignature(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.mapperInputCallSignature(inputType, zdl);
    }

    public String inputFieldInitializer(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.inputFieldInitializer(inputType, zdl);
    }

    public Map<String, Object> methodEntity(Map<String, Object> method, Options options) {
        var zdl = options.get("zdl");
        return ZDLFindUtils.methodEntity(method, (Map) zdl);
    }

    public Map<String, Object> methodReturnEntity(Map<String, Object> method, Options options) {
        var returnType = (String) method.get("returnType");
        var zdl = options.get("zdl");
        return JSONPath.get(zdl, "$.allEntitiesAndEnums." + returnType);
    }

    public String wrapWithMapper(Map<String, Object> entity, Options options) {
        var method = (Map) options.get("method");
        var returnType = methodReturnEntity(method, options);
        return MapperSupport.wrapWithMapper(method, entity, returnType);
    }

    public String returnType(Map<String, Object> method, Options options) {
        return ZDLJavaSignatureUtils.methodReturnType(method);
    }

    public String fieldType(Map field, Options options) {
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        return ZDLJavaSignatureUtils.fieldType(field, prefix, suffix);
    };

    public String fieldTypeInitializer(Map field, Options options) {
        return ZDLJavaSignatureUtils.fieldTypeInitializer(field);
    };

    public String javaType(Map field, Options options) {
        return ZDLJavaSignatureUtils.javaType(field);
    }

    public Object findEntity(String entityName, Options options) {
        var zdl = options.get("zdl");
        var entity = JSONPath.get(zdl, "$.allEntitiesAndEnums." + entityName);
        if("entities".equals(JSONPath.get(entity, "type"))) {
            return entity;
        }
        if("aggregates".equals(JSONPath.get(entity, "type"))) {
            return findEntity(JSONPath.get(entity, "$.aggregateRoot"), options);
        }
        return null;
    }

    public String populateField(Map field, Options options) {
        return ZDLJavaSignatureUtils.populateField(field);
    }

    public String relationshipFieldType(Map field, Options options) {
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        return ZDLJavaSignatureUtils.relationshipFieldType(field, prefix, suffix);
    };

    public String relationshipFieldTypeInitializer(Map field, Options options) {
        return ZDLJavaSignatureUtils.relationshipFieldTypeInitializer(field);
    };

    public String fieldPersistenceAnnotations(Object context, Options options) {
        Map field = (Map) context;
        if (generator.persistence == PersistenceType.mongodb) {
            // filtering with lowerFirst and upperFirst for forward zdl compatibility
//            int dbRef = ((List) JSONPath.get(field, "options[?(@.dBRef || @.DBRef)]")).size();
//            if (dbRef > 0) {
//                return "@DBRef";
//            }
            int documentedOptions = ((List) JSONPath.get(field, "options[?(@.ref || @.dbref)]")).size();
            if (documentedOptions > 0) {
                return "@DocumentReference";
            }
            if(JSONPath.get(field, "options.transient", false)) {
                return "@org.springframework.data.annotation.Transient";
            }
            return "@Field";
        }
        return "";
    };

    public String fieldValidationAnnotations(Object context, Options options) {

        Map field = (Map) context;
        var required = JSONPath.get(field, "validations.required.value");
        var min = JSONPath.get(field, "validations.min.value");
        var max = JSONPath.get(field, "validations.max.value");
        var minlength = JSONPath.get(field, "validations.minlength.value");
        var maxlength = JSONPath.get(field, "validations.maxlength.value");
        var pattern = JSONPath.get(field, "validations.pattern.value");
        var unique = JSONPath.get(field, "validations.unique.value");
        List<String> annotations = new ArrayList<>();
        if (unique != null) {
            if(generator.persistence == PersistenceType.mongodb && options.fn.filename().contains("/core/domain/mongodb/")) {
                annotations.add("@Indexed(unique = true)");
            }
        }
        if (required != null) {
            annotations.add("@NotNull");
        }
        if (min != null) {
            annotations.add(String.format("@Min(%s)", min));
        }
        if (max != null) {
            annotations.add(String.format("@Max(%s)", max));
        }
        if (minlength != null && maxlength != null) {
            annotations.add(String.format("@Size(min = %s, max = %s)", minlength, maxlength));
        } else if (maxlength != null) {
            annotations.add(String.format("@Size(max = %s)", maxlength));
        } else if (minlength != null) {
            annotations.add(String.format("@Size(min = %s)", minlength));
        }
        if (pattern != null) {
            annotations.add(String.format("@Pattern(regexp = \"%s\")", validationPatternJava((String) pattern, null)));
        }

        return annotations.stream().collect(Collectors.joining(" "));
    };

    public String validationPatternJava(String pattern, Options options) {
        return pattern.replace("\\", "").replace("\\", "\\\\");
    }

    public Object skipEntityRepository(Map entity, Options options) {
        var zdl = options.get("zdl");
        return is(Map.of("zdl", zdl, "entity", entity), "persistence");
    };

    public Object skipEntityId(Map entity, Options options) {
        var zdl = options.get("zdl");
        return is(Map.of("zdl", zdl, "entity", entity), "embedded", "vo", "input", "abstract");
    };

    public Object addExtends(Object entity, Options options) {
        String superClassName = JSONPath.get(entity, "options.extends");
        String suffix = options.hash("suffix", "");
        if(superClassName != null) {
            return String.format("extends %s%s", superClassName, suffix);
        }
        return "";
    };

    public String abstractClass(Map entity, Options options) {
        return JSONPath.get(entity, "options.abstract", false)? " abstract " : "";
    }

    public boolean needsEventsProducer(Map service, Options options) {
        Map<String, Object> zdl = options.get("zdl");
        return ServiceEventPlanner.needsEventsProducer(zdl, service);
    }

    public boolean needsEventBus(Map service, Options options) {
        Map<String, Object> zdl = options.get("zdl");
        return ServiceEventPlanner.needsEventBus(zdl, service);
    }

    public Object eventsProducerInterface(String serviceName, Options options) {
        return String.format("%sEventsProducer", serviceName.replaceAll("(Service|UseCases)", ""));
    }

    public Object eventsProducerInstance(String serviceName, Options options) {
        return NamingUtils.asInstanceName(serviceName.replaceAll("(Service|UseCases)", "") + "EventsProducer");
    }
}
