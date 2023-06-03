package io.zenwave360.sdk.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLBackendApplicationDefaultHelpers {

    private final JDLBackendApplicationDefaultGenerator generator;

    JDLBackendApplicationDefaultHelpers(JDLBackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public boolean isCrudMethod(String crudMethodPrefix, Options options) {
        var entity = (Map<String, Object>) options.hash("entity");
        var entityName = (String) entity.get("name");
        var entityNamePlural = (String) entity.get("classNamePlural");
        var method = (Map<String, Object>) options.hash("method");
        var methodName = (String) method.get("name");
        var isArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
        var entityMethodSuffix = isArray ? entityNamePlural : entityName;
        return methodName.equals(crudMethodPrefix + entityMethodSuffix);
    }

    public String returnType(Map<String, Object> method, Options options) {
        var methodName = (String) method.get("name");
        var returnType = method.get("returnType");
        var returnTypeIsArray = (Boolean) method.getOrDefault("returnTypeIsArray", false);
        if (returnType == null) {
            return "void";
        }
        if(methodName.startsWith("create")) {
            return (String) returnType;
        }
        if(returnTypeIsArray) {
            if(JSONPath.get(method, "options.pageable", false)) {
                return String.format("Page<%s>", returnType);
            }
            return String.format("List<%s>", returnType);
        }
        return String.format("Optional<%s>", returnType);
    }

    public String fieldType(Object context, Options options) {
        Map field = (Map) context;
        String type = javaType(field, options);
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        if (field.get("isArray") == Boolean.TRUE) {
            return String.format("List<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public String fieldTypeInitializer(Object context, Options options) {
        Map field = (Map) context;
        if (field.get("isArray") == Boolean.TRUE) {
            return "= new ArrayList<>()";
        }
        return "";
    };

    public String javaType(Map field, Options options) {
        return (String) field.get("type");
    }

    public Object findEntity(String entityName, Options options) {
        var jdl = options.param(0, Collections.emptyMap());
        return JSONPath.get(jdl, "entities." + entityName, Collections.emptyMap());
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
        } else if (JDLParser.blobTypes.contains(field.get("type"))) {
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
            // filtering with lowerFirst and upperFirst for forward jdl compatibility
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
            if(generator.persistence == PersistenceType.mongodb) {
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
            annotations.add(String.format("@Pattern(regexp = \"%s\")", pattern));
        }

        return annotations.stream().collect(Collectors.joining(" "));
    };

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
