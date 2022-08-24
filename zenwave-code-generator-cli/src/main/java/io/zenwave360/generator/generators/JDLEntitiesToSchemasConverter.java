package io.zenwave360.generator.generators;

import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JDLEntitiesToSchemasConverter {

    private static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob");

    public static Map<String, Object> convertEnumToSchema(Map<String, Object> enumValue) {
        Map<String, Object> enumSchema = new LinkedHashMap<>();
        enumSchema.put("type", "string");
        if(enumValue.get("comment") != null) {
            enumSchema.put("description", enumValue.get("comment"));
        }
        List<String> values = JSONPath.get(enumValue, "$.values..name");
        enumSchema.put("enum", values);
        return enumSchema;
    }

    public static Map<String, Object> convertEntityToSchema(Map<String, Object> entity, String jdlBusinessEntityProperty) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put(jdlBusinessEntityProperty, entity.get("name"));
        if(entity.get("comment") != null) {
            schema.put("description", entity.get("comment"));
        }
        List<String> requiredProperties = new ArrayList<>();
        schema.put("required", requiredProperties);
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.put("properties", properties);

        if(!JSONPath.get(entity, "options.embedded", false)) {
            properties.put("id", Map.of("type", "string"));
        }

        List<Map<String, Object>> fields = (List) JSONPath.get(entity, "$.fields[*]");
        for (Map<String, Object> field : fields) {
            Map<String, Object> property = new LinkedHashMap<>();

            if("String".equals(field.get("type")) || "TextBlob".equals(field.get("type"))) {
                property.put("type", "string");
            }
            else if("Enum".equals(field.get("type"))) {
                property.put("type", "string");
            }
            else if("LocalDate".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date");
            }
            else if("ZonedDateTime".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            }
            else if("Instant".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            }
            else if("Duration".equals(field.get("type"))) {
                property.put("type", "string");
                //                property.put("format", "date-time");
            }
            else if("Integer".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int32");
            }
            else if("Long".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int64");
            }
            else if("Float".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "float");
            }
            else if("Double".equals(field.get("type")) || "BigDecimal".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "double");
            }
            else if("Boolean".equals(field.get("type"))) {
                property.put("type", "boolean");
            }
            else if("UUID".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("pattern", "^[a-f\\d]{4}(?:[a-f\\d]{4}-){4}[a-f\\d]{12}$");
            }
            else if(blobTypes.contains(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "binary");
            } else {
                property.put("$ref", "#/components/schemas/" + field.get("type"));
            }

            String required = JSONPath.get(field, "$.validations.required.value");
            if(required != null) {
                requiredProperties.add((String) field.get("name"));
            }
            String minlength = JSONPath.get(field, "$.validations.minlength.value");
            if(minlength != null) {
                property.put("minLength", asNumber(minlength));
            }
            String maxlength = JSONPath.get(field, "$.validations.maxlength.value");
            if(maxlength != null) {
                property.put("maxLength", asNumber(maxlength));
            }
            String pattern = JSONPath.get(field, "$.validations.pattern.value");
            if(pattern != null) {
                property.put("pattern", pattern);
            }
            if(field.get("comment") != null){
                property.put("description", field.get("comment"));
            }

            if(field.get("isArray") == Boolean.TRUE) {
                property = Maps.of("type", "array", "items", property);
            }

            properties.put((String) field.get("name"), property);
        }

        if(requiredProperties.size() == 0) {
            schema.remove("required");
        }

        return schema;
    }

    private static Object asNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return number;
        }
    }
}
