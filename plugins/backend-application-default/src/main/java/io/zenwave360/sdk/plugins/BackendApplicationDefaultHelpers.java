package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;
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
    public boolean isWriteMethod(Map method, Options options) {
        var methodName = (String) method.get("name");
        var hasId = method.get("paramId") != null;
        return hasId || methodName.startsWith("create") || methodName.startsWith("update") || methodName.startsWith("delete");
    }

    @Deprecated
    public boolean isCrudMethod(String crudMethodPrefix, Options options) {
        var entity = (Map<String, Object>) options.hash("entity");
        var entityName = (String) entity.get("name");
        var entityNamePlural = (String) entity.get("classNamePlural");
        var method = (Map<String, Object>) options.hash("method");
        var methodName = (String) method.get("name");
        var isArray = "true".equals(String.valueOf(method.get("returnTypeIsArray")));
        var isOptional = "true".equals(String.valueOf(method.get("returnTypeIsOptional")));
        var entityMethodSuffix = isArray ? entityNamePlural : entityName;
        var isCrudMethod = methodName.equals(crudMethodPrefix + entityMethodSuffix);
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
        var aggregateName = findEntityAggregate(entityName, options);
        return !isAggregateRoot.isEmpty() || aggregateName != null;
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
        var zdl = options.get("zdl");
        var hasNaturalId = JSONPath.get(method, "$.naturalId", false);
        if(hasNaturalId) {
            var entity = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            return ZDLJavaSignatureUtils.naturalIdsRepoMethodCallSignature(entity);
        }
        return "findById(id)";
    }

    public String idFieldInitialization(Map method, Options options) {
        var zdl = options.get("zdl");
        var hasNaturalId = JSONPath.get(method, "$.naturalId", false);
        if(hasNaturalId) {
            var entity = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            List<Map> fields = ZDLFindUtils.naturalIdFields(entity);
            return fields.stream().map(field -> String.format("var %s = %s;", field.get("name"), ZDLJavaSignatureUtils.populateField(field)))
                    .collect(Collectors.joining("\n"));
        }
        return generator.getIdJavaType() + " id = null;";
    }

    public String idParamsCallSignature(Map method, Options options) {
        var zdl = options.get("zdl");
        var hasNaturalId = JSONPath.get(method, "$.naturalId", false);
        if(hasNaturalId) {
            var entity = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            var fields = ZDLFindUtils.naturalIdFields(entity);
            return ZDLJavaSignatureUtils.fieldsParamsCallSignature(fields);
        }
        return "id";
    }

    public Map<String, Object> serviceParameterEntityPairs(Map service, Options options) {
        var zdl = (Map) options.get("zdl");
        var inputDTOSuffix = (String) options.get("inputDTOSuffix");
        var map = new HashMap<String, Object>();
        for (Map method : JSONPath.get(service, "methods[*]", List.<Map>of())) {
            var input = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("parameter"));
            var entity = (Map)JSONPath.get(zdl, "$.entities." + method.get("entity"));
            var isInput = input != null && "inputs".equals(input.get("type"));
            var isPatch = JSONPath.get(method, "options.patch") != null;

            if (entity != null) {
                if (isPatch) {
                    var key = "java.util.Map-" + entity.get("className");
                    map.put(key, Maps.of("input", Map.of("className","Map"), "entity", entity, "method", method));
                } else if (input != null) {
                    var key = input.get("className") + (isInput? inputDTOSuffix : "") + "-" + entity.get("className");
                    map.put(key, Maps.of("input", input, "entity", entity, "method", method));
                }
            }

        }
        return map;
    }

    public Map<String, Object> serviceEntityReturnTypePairs(Map service, Options options) {
        var zdl = (Map) options.get("zdl");
        var map = new HashMap<String, Object>();
        for (Map method : JSONPath.get(service, "methods[*]", List.<Map>of())) {
            var entity = (Map)JSONPath.get(zdl, "$.entities." + method.get("entity"));
            var output = (Map)JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("returnType"));
            var isArray = Boolean.TRUE.equals(method.get("returnTypeIsArray"));
            var isOptional = Boolean.TRUE.equals(method.get("returnTypeIsOptional"));
            var isPaginated = JSONPath.get(method, "options.paginated", false);

            if (entity != null && output != null) {
                if (entity.get("name").equals(output.get("name"))) {
                    continue;
                }
                var key = entity.get("className") + "-" + output.get("className");
                map.put(key, Maps.of("entity", entity, "output", output, "method", method, "isArray", isArray, "isOptional", isOptional, "isPaginated", isPaginated));
            }
        }
        return map;
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
        if(returnType == null) {
            return "";
        }
        var returnTypeIsArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
        var isReturnTypeOptional = (Boolean) method.getOrDefault("returnTypeIsOptional", false);
        var instanceName = returnTypeIsArray? entity.get("instanceNamePlural") : entity.get("instanceName");
        var serviceInstanceName = NamingUtils.asInstanceName((String) method.get("serviceName"));
        if (Objects.equals(entity.get("name"), returnType.get("name"))) {
//            if(JSONPath.get(method, "options.paginated", false)) {
//                return (String) instanceName;
//            }
            return (String) instanceName;
        } else {
            if(returnTypeIsArray) {
                if(JSONPath.get(method, "options.paginated", false)) {
                    return String.format("%sMapper.as%sPage(%s)", serviceInstanceName, returnType.get("className"), instanceName);
                }
                return String.format("%sMapper.as%sList(%s)", serviceInstanceName, returnType.get("className"), instanceName);
            } else {
                return String.format("%sMapper.as%s(%s)", serviceInstanceName, returnType.get("className"), instanceName);
            }
        }
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
        var methods = service != null?
                JSONPath.get(service, "methods[*]", List.<Map<String, Object>>of())
                : ZDLFindUtils.methodsWithEvents(zdl);
        var eventNamesExpr = methods.stream().map(ZDLFindUtils::methodEventsFlatList).flatMap(List::stream).collect(Collectors.joining("|"));
        var externalEvents = (List) JSONPath.get(zdl, "$.events[*][?(@.name =~ /(" + eventNamesExpr + ")/)].options.asyncapi");
        return externalEvents != null && !externalEvents.isEmpty();
    }

    public boolean needsEventBus(Map service, Options options) {
        Map<String, Object> zdl = options.get("zdl");
        var methods = service != null?
                JSONPath.get(service, "methods[*]", List.<Map<String, Object>>of())
                : ZDLFindUtils.methodsWithEvents(zdl);
        var eventNamesExpr = methods.stream().map(ZDLFindUtils::methodEventsFlatList).flatMap(List::stream).collect(Collectors.joining("|"));
        var domainEvents = JSONPath.get(zdl, "$.events[*][?(@.name =~ /(" + eventNamesExpr + ")/)]", List.<Map>of()).stream()
                .filter(event -> JSONPath.get(event, "options.asyncapi") == null).collect(Collectors.toSet());
        return domainEvents != null && !domainEvents.isEmpty();
    }

    public Object eventsProducerInterface(String serviceName, Options options) {
        return String.format("%sEventsProducer", serviceName.replaceAll("(Service|UseCases)", ""));
    }

    public Object eventsProducerInstance(String serviceName, Options options) {
        return NamingUtils.asInstanceName(serviceName.replaceAll("(Service|UseCases)", "") + "EventsProducer");
    }
}
