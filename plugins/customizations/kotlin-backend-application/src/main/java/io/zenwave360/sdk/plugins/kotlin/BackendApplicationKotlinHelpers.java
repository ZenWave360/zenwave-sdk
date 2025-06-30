package io.zenwave360.sdk.plugins.kotlin;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultGenerator;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.util.*;
import java.util.stream.Collectors;

public class BackendApplicationKotlinHelpers {

    private final BackendApplicationDefaultGenerator generator;

    BackendApplicationKotlinHelpers(BackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public String javaType(Map field, Options options) {
        return ZDLJavaSignatureUtils.javaType(field).replace("byte[]", "ByteArray");
    }

    public String naturalIdsRepoMethodSignature(Map entity, Options options) {
        return ZDLJavaSignatureUtils.naturalIdsKotlinRepoMethodSignature(entity);
    }

    public String methodParametersSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.kotlinMethodParametersSignature(generator.getIdJavaType(), method, zdl);
    }

    public String mapperInputSignature(String inputType, Options options) {
        var zdl = (Map) options.get("zdl");
        var signature = ZDLJavaSignatureUtils.mapperInputSignature(inputType, zdl);
        signature = signature.replace("java.util.Map input", "java.util.Map<String,Any?> input");
        return ZDLJavaSignatureUtils.toKotlinMethodSignature(signature);
    }

    public String returnType(Map<String, Object> method, Options options) {
        return ZDLJavaSignatureUtils.methodReturnType(method).replace("void", "Unit");
    }

    public String fieldType(Map field, Options options) {
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        return fixKotlinCollectionTypes(ZDLJavaSignatureUtils.fieldType(field, prefix, suffix));
    };

    public String relationshipFieldType(Map field, Options options) {
        String prefix = (String) options.hash.getOrDefault("prefix", "");
        String suffix = (String) options.hash.getOrDefault("suffix", "");
        String nullable = !JSONPath.get(field, "isCollection", false) ? "?" : "";
        return ZDLJavaSignatureUtils.relationshipFieldType(field, prefix, suffix) + nullable;
    };

    public String relationshipFieldTypeInitializer(Map field, Options options) {
        var typeInitializer = ZDLJavaSignatureUtils.relationshipFieldTypeInitializer(field);
        if(typeInitializer.trim().isEmpty()) {
            return " = null";
        }
        return fixKotlinTypeInitializers(typeInitializer);
    };

    public String fieldTypeInitializer(Map field, Options options) {
        var typeInitializer = fixKotlinTypeInitializers(ZDLJavaSignatureUtils.fieldTypeInitializer(field));
        if(typeInitializer.trim().isEmpty()) {
            return " = null";
        }
        return typeInitializer;
    };

    public String idFieldInitialization(Map method, Options options) {
        var zdl = options.get("zdl");
        var hasNaturalId = JSONPath.get(method, "$.naturalId", false);
        if(hasNaturalId) {
            var entity = (Map) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            List<Map> fields = ZDLFindUtils.naturalIdFields(entity);
            return fields.stream().map(field -> String.format("val %s = %s;", field.get("name"), ZDLJavaSignatureUtils.populateField(field)))
                    .collect(Collectors.joining("\n"));
        }
        return "val id: " + generator.getIdJavaType() + " = " + idInitialization(generator.getIdJavaType());
    }

    public String populateField(Map field, Options options) {
        return ZDLJavaSignatureUtils.populateField(field).replace("new ", "");
    }

    private String idInitialization(String idJavaType) {
        return switch (idJavaType) {
            case "Long" -> "1L";
            case "Integer" -> "1";
            case "String" -> "\"1\"";
            default -> "";
        };
    }

    private String fixKotlinTypeInitializers(String type) {
        return type
                .replaceAll("new HashSet<>\\(\\)", "mutableSetOf()")
                .replaceAll("new ArrayList<>\\(\\)", "mutableListOf()")
                .replaceAll("new HashMap<>\\(\\)", "mutableMapOf()");
    }

    private String fixKotlinCollectionTypes(String type) {
        return type
                .replaceAll("byte\\[]", "ByteArray")
                .replaceAll("Set<", "MutableSet<")
                .replaceAll("List<", "MutableList<")
                .replaceAll("Map<", "MutableMap<");
    }
}
