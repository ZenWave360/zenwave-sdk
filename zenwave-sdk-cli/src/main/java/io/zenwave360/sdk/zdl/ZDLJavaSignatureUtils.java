package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class ZDLJavaSignatureUtils {

    public static String javaType(Map field) {
        return (String) field.get("type");
    }

    public static String methodParameterType(Map method, Map zdl, String inputDTOSuffix) {
        var parameterName = (String) method.get("parameter");
        var isEntity = JSONPath.get(zdl, "$.entities." + parameterName) != null;
        return String.format("%s%s", parameterName, isEntity? inputDTOSuffix : "");
    }

    public static String methodReturnType(Map method) {
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

    public static String fieldType(Map field, String prefix, String suffix) {
        String type = javaType(field);
        if (field.get("isArray") == Boolean.TRUE) {
            if("byte".equalsIgnoreCase(type)) {
                return "byte[]";
            }
            return String.format("List<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public static String fieldTypeInitializer(Map field) {
        if (field.get("isArray") == Boolean.TRUE) {
            if("byte".equalsIgnoreCase(String.valueOf(field.get("type")))) {
                return "";
            }
            return "= new ArrayList<>()";
        }
        return "";
    }

    public static String populateField(Map field) {
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

    public static String relationshipFieldType(Map relationship, String prefix, String suffix) {
        String type = (String) relationship.get("otherEntityName");
        if (relationship.get("isCollection") == Boolean.TRUE) {
            return String.format("Set<%s%s%s>", prefix, type, suffix);
        }
        return String.format("%s%s%s", prefix, type, suffix);
    };

    public static String relationshipFieldTypeInitializer(Map relationship) {
        if (relationship.get("isCollection") == Boolean.TRUE) {
            return "= new HashSet<>()";
        }
        return "";
    };

}
