package io.zenwave360.sdk.generators;

import java.util.*;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Lists;
import io.zenwave360.sdk.utils.Maps;

public class JDLEntitiesToSchemasConverter {
    private static final List blobTypes = List.of("Blob", "AnyBlob", "ImageBlob");

    public String jdlBusinessEntityProperty = "x-business-entity";

    public String idType = "string";
    public String idTypeFormat = null;

    public boolean includeVersion = true;

    public JDLEntitiesToSchemasConverter withIdType(String idType) {
        this.idType = idType;
        return this;
    }

    public JDLEntitiesToSchemasConverter withIdType(String idType, String idTypeFormat) {
        this.idType = idType;
        this.idTypeFormat = idTypeFormat;
        return this;
    }

    protected Map idTypeMap() {
        Map idType = new LinkedHashMap();
        idType.put("type", this.idType);
        if (this.idTypeFormat != null) {
            idType.put("format", this.idTypeFormat);
        }
        return idType;
    }

    public JDLEntitiesToSchemasConverter withJdlBusinessEntityProperty(String jdlBusinessEntityProperty) {
        this.jdlBusinessEntityProperty = jdlBusinessEntityProperty;
        return this;
    }

    public Map<String, Object> convertToSchema(Map<String, Object> entityOrEnum, Map<String, Object> jdlModel) {
        boolean isEnum = entityOrEnum.get("values") != null;
        return isEnum ? convertEnumToSchema(entityOrEnum, jdlModel) : convertEntityToSchema(entityOrEnum, jdlModel);
    }

    public Map<String, Object> convertEnumToSchema(Map<String, Object> enumValue, Map<String, Object> jdlModelv) {
        Map<String, Object> enumSchema = new LinkedHashMap<>();
        enumSchema.put("type", "string");
        enumSchema.put(jdlBusinessEntityProperty, enumValue.get("name"));
        if (enumValue.get("comment") != null) {
            enumSchema.put("description", enumValue.get("comment"));
        }
        List<String> values = JSONPath.get(enumValue, "$.values..name");
        enumSchema.put("enum", values);
        return enumSchema;
    }

    public Map<String, Object> convertEntityToSchema(Map<String, Object> entity, Map<String, Object> jdlModel) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put(jdlBusinessEntityProperty, entity.get("name"));
        if (entity.get("comment") != null) {
            schema.put("description", entity.get("comment"));
        }
        List<String> requiredProperties = new ArrayList<>();
        schema.put("required", requiredProperties);
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.put("properties", properties);

        if (!JSONPath.get(entity, "options.embedded", false)) {
            properties.put("id", idTypeMap());
            if (includeVersion) {
                properties.put("version", Maps.of("type", "integer"));
            }
        }

        List<Map<String, Object>> fields = (List) JSONPath.get(entity, "$.fields[*]");
        String superClassName = JSONPath.get(entity, "$.options.extends");
        if (superClassName != null) {
            List superClassFields = (List) JSONPath.get(jdlModel, "$.entities['" + superClassName + "'].fields[*]");
            fields = Lists.concat(superClassFields, fields);
        }
        for (Map<String, Object> field : fields) {
            Map<String, Object> property = new LinkedHashMap<>();

            if ("String".equals(field.get("type")) || "TextBlob".equals(field.get("type"))) {
                property.put("type", "string");
            } else if ("Enum".equals(field.get("type"))) {
                property.put("type", "string");
            } else if ("LocalDate".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date");
            } else if ("ZonedDateTime".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            } else if ("Instant".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "date-time");
            } else if ("Duration".equals(field.get("type"))) {
                property.put("type", "string");
                // property.put("format", "date-time");
            } else if ("Integer".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int32");
            } else if ("Long".equals(field.get("type"))) {
                property.put("type", "integer");
                property.put("format", "int64");
            } else if ("Float".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "float");
            } else if ("Double".equals(field.get("type")) || "BigDecimal".equals(field.get("type"))) {
                property.put("type", "number");
                property.put("format", "double");
            } else if ("Boolean".equals(field.get("type"))) {
                property.put("type", "boolean");
            } else if ("UUID".equals(field.get("type"))) {
                property.put("type", "string");
                property.put("pattern", "^[a-f\\d]{4}(?:[a-f\\d]{4}-){4}[a-f\\d]{12}$");
            } else if (blobTypes.contains(field.get("type"))) {
                property.put("type", "string");
                property.put("format", "binary");
            } else {
                property.put("$ref", "#/components/schemas/" + field.get("type"));
            }

            String required = JSONPath.get(field, "$.validations.required.value");
            if (required != null) {
                requiredProperties.add((String) field.get("name"));
            }
            String minlength = JSONPath.get(field, "$.validations.minlength.value");
            if (minlength != null) {
                property.put("minLength", asNumber(minlength));
            }
            String maxlength = JSONPath.get(field, "$.validations.maxlength.value");
            if (maxlength != null) {
                property.put("maxLength", asNumber(maxlength));
            }
            String pattern = JSONPath.get(field, "$.validations.pattern.value");
            if (pattern != null) {
                property.put("pattern", pattern);
            }
            if (field.get("comment") != null) {
                property.put("description", field.get("comment"));
            }

            if (field.get("isArray") == Boolean.TRUE) {
                property = Maps.of("type", "array", "items", property);
            }

            properties.put((String) field.get("name"), property);
        }

        List<Map<String, Object>> relationships = JSONPath.get(entity, "$.relationships[*]", Collections.emptyList());
        if (superClassName != null) {
            List superClassRelationships = (List) JSONPath.get(jdlModel, "$.entities['" + superClassName + "'].relationships[*]");
            relationships = Lists.concat(superClassRelationships, relationships);
        }
        for (Map<String, Object> relationship : relationships) {
            if((relationship.get("fieldName") != null) && (boolean) relationship.getOrDefault("ownerSide", false)) {
                var fieldName = (String) relationship.get("fieldName");
                var isAddRelationshipById = isAddRelationshipById(relationship);
                var isCollection = relationship.get("isCollection") == Boolean.TRUE;
                if(isAddRelationshipById) {
                    if(isCollection) {
                        properties.put(fieldName + "Id", Map.of("type", "array", "items", idTypeMap()));
                    } else {
                        properties.put(fieldName + "Id", idTypeMap());
                    }
                }
                Map<String, Object> property = new LinkedHashMap<>();
                if (relationship.get("comment") != null || isAddRelationshipById) {
                    var readOnlyWarning = isAddRelationshipById ? "(read-only) " : "";
                    // TODO desc+$ref: property.put("description", readOnlyWarning + relationship.getOrDefault("comment", ""));
                }
                property.put("$ref", "#/components/schemas/" + relationship.get("otherEntityName"));
                if (isCollection) {
                    property = Maps.of("type", "array", "items", property);
                }
                properties.put(fieldName, property);
            }
        }

        if (requiredProperties.size() == 0) {
            schema.remove("required");
        }

        return schema;
    }

    public boolean isAddRelationshipById(Object relationship) {
        boolean isOwnerSide = JSONPath.get(relationship, "ownerSide", false);
        String relationType = JSONPath.get(relationship, "type");
        return "ManyToOne".contentEquals(relationType) && isOwnerSide;
    }

    private static Object asNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return number;
        }
    }
}
