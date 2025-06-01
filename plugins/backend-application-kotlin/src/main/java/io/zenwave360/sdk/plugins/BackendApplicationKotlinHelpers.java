package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.util.*;

public class BackendApplicationKotlinHelpers {

    private final BackendApplicationDefaultGenerator generator;

    BackendApplicationKotlinHelpers(BackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }

    public String methodParametersSignature(Map<String, Object> method, Options options) {
        var zdl = (Map) options.get("zdl");
        return ZDLJavaSignatureUtils.kotlinMethodParametersSignature(generator.getIdJavaType(), method, zdl);
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
        return fixKotlinTypeInitializers(ZDLJavaSignatureUtils.relationshipFieldTypeInitializer(field));
    };

    public String fieldTypeInitializer(Map field, Options options) {
        return fixKotlinTypeInitializers(ZDLJavaSignatureUtils.fieldTypeInitializer(field));
    };

    private String fixKotlinTypeInitializers(String type) {
        return type
                .replaceAll("new HashSet<>\\(\\)", "mutableSetOf()")
                .replaceAll("new ArrayList<>\\(\\)", "mutableListOf()")
                .replaceAll("new HashMap<>\\(\\)", "mutableMapOf()");
    }

    private String fixKotlinCollectionTypes(String type) {
        return type
                .replaceAll("Set<", "MutableSet<")
                .replaceAll("List<", "MutableList<")
                .replaceAll("Map<", "MutableMap<");
    }
}
