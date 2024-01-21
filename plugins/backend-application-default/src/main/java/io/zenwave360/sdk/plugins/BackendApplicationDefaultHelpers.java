package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.ZDLFindUtils;
import io.zenwave360.sdk.zdl.ZDLJavaSignatureUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.utils.JSONPath;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;

public class BackendApplicationDefaultHelpers {

    private final BackendDefaultApplicationGenerator generator;

    BackendApplicationDefaultHelpers(BackendDefaultApplicationGenerator generator) {
        this.generator = generator;
    }

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

    public Collection<String> findAggregateInputs(Map aggregate, Options options) {
        var zdl = options.get("zdl");
        var aggregateName = (String) aggregate.get("name");
        var inputDTOSuffix = (String) options.get("inputDTOSuffix");
        Set<String> inputs = new HashSet<String>();
        inputs.addAll(JSONPath.get(zdl, "$.services[*][?('" + aggregateName + "' in @.aggregates)].methods[*].parameter"));
        // inputs.addAll(JSONPath.get(zdl, "$.services[*][?('" + aggregateName + "' in @.aggregates)].methods[*].returnType"));
        // inputs.add(aggregateName + inputDTOSuffix);
        inputs = inputs.stream().filter(Objects::nonNull).collect(Collectors.toSet());

        var entities = JSONPath.get(zdl, "$.entities", Collections.emptyMap());

        inputs = inputs.stream().map(input -> entities.get(input) != null? input + inputDTOSuffix : input).collect(Collectors.toSet());

        return inputs;
    }

    public Collection<String> findAggregateOutputs(Map aggregate, Options options) {
        var zdl = options.get("zdl");
        var aggregateName = (String) aggregate.get("name");
        Set<String> outputs = new HashSet<String>();
        outputs.addAll(JSONPath.get(zdl, "$.services[*][?('" + aggregateName + "' in @.aggregates)].methods[*].returnType"));
        outputs = outputs.stream().filter(input -> input != null && !aggregateName.equals(input)).collect(Collectors.toSet());
        return outputs;
    }

    public String methodParameterType(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParameterType(method, zdl, generator.inputDTOSuffix);
    }

    public String methodParametersSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParametersSignature(generator.getIdJavaType(), method, zdl, generator.inputDTOSuffix);
    }

    public String methodParametersCallSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.methodParametersCallSignature(method, zdl, generator.inputDTOSuffix);
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

    public Collection<Map<String, Object>> listOfPairEventEntity(Map<String, Object> zdl, Options options) {
        var result = new HashMap<String, Object>();
        var methods = ZDLFindUtils.methodsWithEvents(zdl);
        for (Map<String, Object> method : methods) {
            var entity = methodEntity(method, options);
            var methodEvents = methodEvents(method, options);
            for (Map<String, Object> event : methodEvents) {
                if (entity == null) {
                    var key = JSONPath.get(event, "name") + "-method-" + ZDLJavaSignatureUtils.methodParametersCallSignature(method, zdl, generator.inputDTOSuffix);
                    result.put(key, Map.of("event", event, "method", method));
                } else {
                    var key = JSONPath.get(event, "name") + "-" + entity.get("name");
                    result.put(key, Map.of("event", event, "entity", entity));
                    result.putAll(extraMappingsFromEventFields(event, entity, zdl));
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
                result.put(key, Map.of("event",targetEntity, "entity", targetEntity));
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
        return ZDLJavaSignatureUtils.mapperInputSignature(inputType, zdl, generator.inputDTOSuffix);
    }

    public String mapperInputCallSignature(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.mapperInputCallSignature(inputType, zdl);
    }

    public String inputFieldInitializer(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.inputFieldInitializer(inputType, zdl, generator.inputDTOSuffix);
    }

    public Map<String, Object> methodEntity(Map<String, Object> method, Options options) {
        var returnType = (String) method.get("returnType");
        var zdl = options.get("zdl");
        var service = JSONPath.get(zdl, "$.services." + method.get("serviceName"));
        var aggregates = JSONPath.get(service, "aggregates", Collections.emptyList());
        if(aggregates.size() == 1 && StringUtils.equals(returnType, aggregates.get(0).toString())) {
            return JSONPath.get(zdl, "$.entities." + returnType);
        }
        return null;
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
        var instanceName = returnTypeIsArray? entity.get("instanceNamePlural") : entity.get("instanceName");
        if (Objects.equals(entity.get("name"), returnType.get("name"))) {
            if(JSONPath.get(method, "options.paginated", false)) {
                return "page";
            }
            return (String) instanceName;
        } else {
            if(returnTypeIsArray) {
                if(JSONPath.get(method, "options.paginated", false)) {
                    return String.format("%sMapper.as%sPage(%s)", instanceName, returnType.get("className"), "page");
                }
                return String.format("%sMapper.as%sList(%s)", instanceName, returnType.get("className"), instanceName);
            } else {
                return String.format("%sMapper.as%s(%s)", instanceName, returnType.get("className"), instanceName);
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
        return JSONPath.get(zdl, "entities." + entityName);
    }

    public String inputDTOSuffix(Object entity, Options options) {
        var isInput = JSONPath.get(entity, "options.input.value", false);
        return isInput? "" : generator.inputDTOSuffix;
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
        if (minlength != null || maxlength != null) {
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

    public Object skipEntityRepository(Object context, Options options) {
        Map entity = (Map) context;
        return generator.skipEntityRepository.apply(Map.of("entity", entity));
    };

    public Object skipEntityId(Object context, Options options) {
        Map entity = (Map) context;
        return generator.skipEntityId.apply(Map.of("entity", entity));
    };

    public Object addExtends(Object entity, Options options) {
        String superClassName = JSONPath.get(entity, "options.extends");
        String suffix = options.hash("suffix", "");
        if(superClassName != null) {
            return String.format("extends %s%s", superClassName, suffix);
        }
        return "";
    };

    public Object eventsProducerInterface(String serviceName, Options options) {
        return String.format("I%sEventsProducer", serviceName.replaceAll("(Service|UseCases)", ""));
    }

    public Object eventsProducerInstance(String serviceName, Options options) {
        return NamingUtils.asInstanceName(serviceName.replaceAll("(Service|UseCases)", "") + "EventsProducer");
    }
}
