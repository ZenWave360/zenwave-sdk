package io.zenwave360.sdk.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.plugins.ConfigurationProvider;
import io.zenwave360.sdk.zdl.layout.DefaultProjectLayout;
import io.zenwave360.sdk.zdl.layout.ProjectLayout;
import org.apache.commons.lang3.ObjectUtils;

import com.jayway.jsonpath.JsonPath;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.Model;

public abstract class AbstractOpenAPIGenerator implements Generator {

    public enum OperationType {
        GET, PUT, POST, DELETE, PATCH, HEAD, PARAMETERS
    }

    @DocumentedOption(description = "OpenAPI specification file. Overrides `specFile`")
    public String openapiFile;

    @DocumentedOption(description = "Applications base package")
    public String basePackage;

    @DocumentedOption(description = "The package to used by OpenAPI-Generator for generated api objects/classes")
    public String openApiApiPackage;

    @DocumentedOption(description = "The package to used by OpenAPI-Generator for generated model objects/classes")
    public String openApiModelPackage = "{{openApiApiPackage}}";

    @DocumentedOption(description = "Project layout")
    public ProjectLayout layout;

    @DocumentedOption(description = "Sets the prefix for model enums and classes used by OpenAPI-Generator")
    public String openApiModelNamePrefix = "";

    @DocumentedOption(description = "Sets the suffix for model enums and classes used by OpenAPI-Generator")
    public String openApiModelNameSuffix = "";

    @DocumentedOption(description = "OpenAPI operationIds to generate code for")
    public List<String> operationIds = new ArrayList<>();

    @DocumentedOption(description = "Status codes to generate code for")
    public List<String> statusCodes = List.of("200", "201", "202", "400");

    @Override
    public void onPropertiesSet() {
        if (openApiApiPackage != null && layout == null) {
            layout = new DefaultProjectLayout();
            layout.processLayoutPlaceHolders(this.asConfigurationMap());
        }
        if (layout != null) {
            if (basePackage == null) {
                basePackage = layout.basePackage;
            }
            if (this.openApiApiPackage == null) {
                this.openApiApiPackage = layout.openApiApiPackage;
            }
            if (this.openApiModelPackage == null) {
                this.openApiModelPackage = layout.openApiModelPackage;
            }
        }
    }


    public String getApiPackageFolder() {
        return this.openApiApiPackage.replaceAll("\\.", "/");
    }

    public String getModelPackageFolder() {
        return this.openApiModelPackage.replaceAll("\\.", "/");
    }

    public List<Map<String, Object>> getOperationsByOperationIds(Model apiModel, List<String> operationIds) {
        List<Map<String, Object>> operations = JsonPath.read(apiModel, "$.paths[*].*");
        return operationIds.stream().map(operationId -> operations.stream()
                .filter(operation -> operation.get("operationId").equals(operationId)).findFirst().orElse(Map.of("operationId", operationId)))
                .toList();
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTag(Model apiModel, OperationType... operationTypes) {
        Map<String, List<Map<String, Object>>> operationsByTag = new HashMap<>();
        List<Map<String, Object>> operations = JsonPath.read(apiModel, "$.paths[*].*");
        for (Map<String, Object> operation : operations) {
            if (matchesFilters(operation, operationTypes)) {
                String tag = (String) ObjectUtils.firstNonNull(operation.get("x--normalizedTagName"), "Default");
                if (!operationsByTag.containsKey(tag)) {
                    operationsByTag.put(tag, new ArrayList<>());
                }
                operationsByTag.get(tag).add(operation);
            }
        }
        return operationsByTag;
    }

    public boolean matchesFilters(Map<String, Object> operation, OperationType... operationTypes) {
        var operationOperationType = OperationType.valueOf(operation.get("x--httpVerb").toString().toUpperCase());
        return matchedOperationTypes(operation, operationOperationType, operationTypes) && matchesOperationIds(operation, operationIds);
    }

    protected boolean matchedOperationTypes(Map<String, Object> operation, OperationType operationOperationType, OperationType... operationTypes) {
        return operationTypes == null || operationTypes.length == 0 || Arrays.stream(operationTypes).anyMatch(operationType -> operationOperationType == operationType);
    }

    protected boolean matchesOperationIds(Map<String, Object> operation, List<String> operationIds) {
        if (operationIds == null || operationIds.isEmpty()) {
            return true;
        }
        String operationId = (String) operation.get("operationId");
        return operationIds.contains(operationId);
    }
}
