package io.zenwave360.sdk.generators;

import java.util.*;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Lists;
import io.zenwave360.sdk.utils.Maps;

public class EntitiesToAvroConverter {

    public String idType = "string";
    public String namespace = "com.example.please.update";

    public EntitiesToAvroConverter withIdType(String idType) {
        this.idType = idType;
        return this;
    }

    public EntitiesToAvroConverter withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Map<String, Object> convertToAvro(Map<String, Object> entityOrEnum, Map<String, Object> zdlModel) {
        boolean isEnum = entityOrEnum.get("values") != null;
        return isEnum ? convertEnumToAvro(entityOrEnum) : convertEntityToAvro(entityOrEnum, zdlModel);
    }

    public Map<String, Object> convertEnumToAvro(Map<String, Object> enumValue) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "enum");
        schema.put("name", enumValue.get("name"));
        schema.put("namespace", namespace);
        if (enumValue.get("comment") != null) {
            schema.put("doc", enumValue.get("comment"));
        }
        List<String> values = JSONPath.get(enumValue, "$.values..name");
        schema.put("symbols", values);
        return schema;
    }

    public Map<String, Object> convertEntityToAvro(Map<String, Object> entity, Map<String, Object> zdlModel) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "record");
        schema.put("name", entity.get("name"));
        schema.put("namespace", namespace);
        if (entity.get("comment") != null) {
            schema.put("doc", entity.get("comment"));
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        schema.put("fields", fields);

        if (includeIdAndVersion(entity)) {
            fields.add(Maps.of("name", "id", "type", idType));
        }

        List<Map<String, Object>> entityFields = (List) JSONPath.get(entity, "$.fields[*]");
        String superClassName = JSONPath.get(entity, "$.options.extends");
        if (superClassName != null) {
            List superClassFields = (List) JSONPath.get(zdlModel, "$.entities['" + superClassName + "'].fields[*]");
            fields = Lists.concat(superClassFields, fields);
        }
        for (Map<String, Object> entityField : entityFields) {
            boolean isRequired = JSONPath.get(entityField, "$.validations.required.value") != null;
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", entityField.get("name"));

            if ("String".equals(entityField.get("type")) || "TextBlob".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
            } else if ("Enum".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
            } else if ("LocalDate".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
                // field.put("format", "date");
            } else if ("ZonedDateTime".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
                // field.put("format", "date-time");
            } else if ("Instant".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
                // field.put("format", "date-time");
            } else if ("Duration".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
                // property.put("format", "date-time");
            } else if ("Integer".equals(entityField.get("type"))) {
                field.put("type", typeList("int", isRequired));
                // field.put("format", "int32");
            } else if ("Long".equals(entityField.get("type"))) {
                field.put("type", typeList("long", isRequired));
                // field.put("format", "int64");
            } else if ("Float".equals(entityField.get("type"))) {
                field.put("type", typeList("float", isRequired));
                // field.put("format", "float");
            } else if ("Double".equals(entityField.get("type")) || "BigDecimal".equals(entityField.get("type"))) {
                field.put("type", typeList("double", isRequired));
                // field.put("format", "double");
            } else if ("Boolean".equals(entityField.get("type"))) {
                field.put("type", typeList("boolean", isRequired));
            } else if ("UUID".equals(entityField.get("type"))) {
                field.put("type", typeList("string", isRequired));
                // field.put("pattern", "^[a-f\\d]{4}(?:[a-f\\d]{4}-){4}[a-f\\d]{12}$");
            } else if (ZDLParser.blobTypes.contains(entityField.get("type"))) {
                field.put("type", typeList("bytes", isRequired));
                // field.put("format", "binary");
            } else {
                field.put("type", entityField.get("type")); // TODO consider embedding
            }

            // String minlength = JSONPath.get(entityField, "$.validations.minlength.value");
            // if(minlength != null) {
            // field.put("minLength", asNumber(minlength));
            // }
            // String maxlength = JSONPath.get(entityField, "$.validations.maxlength.value");
            // if(maxlength != null) {
            // field.put("maxLength", asNumber(maxlength));
            // }
            // String pattern = JSONPath.get(entityField, "$.validations.pattern.value");
            // if(pattern != null) {
            // field.put("pattern", pattern);
            // }
            if (entityField.get("comment") != null) {
                field.put("doc", entityField.get("comment"));
            }

            if (entityField.get("isArray") == Boolean.TRUE) {
                field = Maps.of("name", field.get("name"), "type", Maps.of("type", "array", "items", field.get("type"), "java-class", "java.util.List"));
            }

            fields.add(field);
        }

        List<Map<String, Object>> relationships = JSONPath.get(entity, "$.relationships[*]", Collections.emptyList());
        if (superClassName != null) {
            List superClassRelationships = (List) JSONPath.get(zdlModel, "$.entities['" + superClassName + "'].relationships[*]");
            relationships = Lists.concat(superClassRelationships, relationships);
        }
        for (Map<String, Object> relationship : relationships) {
            if((relationship.get("fieldName") != null) && (boolean) relationship.getOrDefault("ownerSide", false)) {
                var isCollection = relationship.get("isCollection") == Boolean.TRUE;
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("name", relationship.get("name"));
                if (relationship.get("comment") != null) {
                    field.put("doc", relationship.get("comment"));
                }
                field.put("type", relationship.get("otherEntityName"));
                if (isCollection) {
                    field = Maps.of("type", "array", "items", field);
                }
                fields.add(field);
            }
        }


        return schema;
    }

    private Object typeList(String type, boolean isRequired) {
        return isRequired ? type : Arrays.asList("null", type);
    }

    private static boolean includeIdAndVersion(Map<String, Object> entity) {
        return "entities".equals(entity.get("type")) && !JSONPath.get(entity, "options.embedded", false);
    }
    private static Object asNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return number;
        }
    }
}
