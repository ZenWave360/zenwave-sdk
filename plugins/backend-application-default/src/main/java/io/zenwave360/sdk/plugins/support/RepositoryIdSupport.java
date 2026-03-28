package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RepositoryIdSupport {

    private RepositoryIdSupport() {
    }

    public static String findById(Map<String, Object> zdl, Map<String, Object> method) {
        if (JSONPath.get(method, "$.naturalId", false)) {
            var entity = (Map<String, Object>) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            return ZDLJavaSignatureUtils.naturalIdsRepoMethodCallSignature(entity);
        }
        return "findById(id)";
    }

    public static String idFieldInitialization(Map<String, Object> zdl,
                                               Map<String, Object> method,
                                               String defaultIdJavaType) {
        if (JSONPath.get(method, "$.naturalId", false)) {
            var entity = (Map<String, Object>) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            List<Map> fields = ZDLFindUtils.naturalIdFields(entity);
            return fields.stream()
                    .map(field -> String.format("var %s = %s;", field.get("name"), ZDLJavaSignatureUtils.populateField(field)))
                    .collect(Collectors.joining("\n"));
        }
        return defaultIdJavaType + " id = null;";
    }

    public static String idParamsCallSignature(Map<String, Object> zdl, Map<String, Object> method) {
        if (JSONPath.get(method, "$.naturalId", false)) {
            var entity = (Map<String, Object>) JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("entity"));
            var fields = ZDLFindUtils.naturalIdFields(entity);
            return ZDLJavaSignatureUtils.fieldsParamsCallSignature(fields);
        }
        return "id";
    }
}
