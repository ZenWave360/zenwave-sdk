package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;

public class BackendApplicationDefaultHelpers {

    private final BackendApplicationDefaultGenerator generator;

    BackendApplicationDefaultHelpers(BackendApplicationDefaultGenerator generator) {
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
        var parameterName = (String) method.get("parameter");
        var zdl = options.get("zdl");
        var isEntity = JSONPath.get(zdl, "$.entities." + parameterName) != null;
        return String.format("%s%s", parameterName, isEntity? generator.inputDTOSuffix : "");
    }

    public Map<String, Object> methodEntity(Map<String, Object> method, Options options) {
        var returnType = (String) method.get("returnType");
        var service = options.get("service");
        var aggregates = JSONPath.get(service, "aggregates", Collections.emptyList());
        if(aggregates.size() == 1 && StringUtils.equals(returnType, aggregates.get(0).toString())) {
            var zdl = options.get("zdl");
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
        var instanceName = (String) entity.get("instanceName");
        if (Objects.equals(entity.get("name"), returnType.get("name"))) {
            if(JSONPath.get(method, "options.paginated", false)) {
                return "page";
            }
            return instanceName;
        } else {
            var returnTypeIsArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
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
        var methodName = (String) method.get("name");
        var returnType = method.get("returnType");
        var returnTypeIsArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
        if (returnType == null) {
            return "void";
        }
//        if(methodName.startsWith("create")) {
//            return (String) returnType;
//        }
        if(returnTypeIsArray) {
            if(JSONPath.get(method, "options.paginated", false)) {
                return String.format("Page<%s>", returnType);
            }
            return String.format("List<%s>", returnType);
        }
        var isOptional = "true".equals(String.valueOf(method.get("returnTypeIsOptional")));
        if(isOptional) {
            return String.format("Optional<%s>", returnType);
        }
        return (String) returnType;
    }

    public String fieldType(Object context, Options options) {
        Map field = (Map) context;
        String type = javaType(field, options);
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        if (field.get("isArray") == Boolean.TRUE) {
            if("byte".equalsIgnoreCase(type)) {
                return "byte[]";
            }
            return String.format("List<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public String fieldTypeInitializer(Object context, Options options) {
        Map field = (Map) context;
        if (field.get("isArray") == Boolean.TRUE) {
            if("byte".equalsIgnoreCase(String.valueOf(field.get("type")))) {
                return "";
            }
            return "= new ArrayList<>()";
        }
        return "";
    };

    public String javaType(Map field, Options options) {
        return (String) field.get("type");
    }

    public Object findEntity(String entityName, Options options) {
        var zdl = options.param(0, Collections.emptyMap());
        return JSONPath.get(zdl, "entities." + entityName, Collections.emptyMap());
    }

    public String inputDTOSuffix(Object entity, Options options) {
        var isInput = JSONPath.get(entity, "options.input.value", false);
        return isInput? "" : generator.inputDTOSuffix;
    }

    public String populateField(Map field, Options options) {
        String value;
        if ("String".equals(field.get("type")) || "TextBlob".equals(field.get("type"))) {
            int min = Integer.valueOf(JSONPath.get(field, "validations.minlength.value", "0"));
            int max = Integer.valueOf(JSONPath.get(field, "validations.minlength.value", "0"));
            int middle = min + (max - min) / 2;
            value = "\"" + StringUtils.repeat("a", middle) + "\"";
        } else if (JSONPath.get(field,"isEnum", false)) {
            value = field.get("type") + ".values()[0]";
        } else if ("LocalDate".equals(field.get("type"))) {
            value = "LocalDate.now()";
        } else if ("ZonedDateTime".equals(field.get("type"))) {
            value = "ZonedDateTime.now()";
        } else if ("Instant".equals(field.get("type"))) {
            value = "Instant.now()";
        } else if ("Duration".equals(field.get("type"))) {
            value = "Duration.ofSeconds(0)";
        } else if ("Integer".equals(field.get("type"))) {
            value = "0";
        } else if ("Long".equals(field.get("type"))) {
            value = "0L";
        } else if ("Float".equals(field.get("type"))) {
            value = "0.0F";
        } else if ("Double".equals(field.get("type"))) {
            value = "0.0";
        } else if("BigDecimal".equals(field.get("type"))) {
            value = "BigDecimal.valueOf(0)";
        } else if ("Boolean".equals(field.get("type"))) {
            value = "false";
        } else if ("UUID".equals(field.get("type"))) {
            value = "UUID.randomUUID()";
        } else if (ZDLParser.blobTypes.contains(field.get("type"))) {
            value = "null";
        } else {
            value = "new " + field.get("type") + "()";
        }

        return value;
    }

    public String relationshipFieldType(Object context, Options options) {
        Map field = (Map) context;
        String type = (String) field.get("otherEntityName");
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        if (field.get("isCollection") == Boolean.TRUE) {
            return String.format("Set<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public String relationshipFieldTypeInitializer(Object context, Options options) {
        Map field = (Map) context;
        if (field.get("isCollection") == Boolean.TRUE) {
            return "= new HashSet<>()";
        }
        return "";
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

    public String criteriaClassName(Object context, Options options) {
        Map entity = (Map) context;
        Object criteria = JSONPath.get(entity, "$.options.searchCriteria");
        if (criteria instanceof String) {
            return (String) criteria;
        }
        if (criteria == Boolean.TRUE) {
            return String.format("%s%s", entity.get("className"), generator.criteriaDTOSuffix);
        }
        return "Pageable";
    };

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

}
