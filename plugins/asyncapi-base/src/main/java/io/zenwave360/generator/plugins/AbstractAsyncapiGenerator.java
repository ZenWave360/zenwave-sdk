package io.zenwave360.generator.plugins;

import com.jayway.jsonpath.JsonPath;
import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.parsers.Model;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractAsyncapiGenerator implements GeneratorPlugin {

    enum OperationType {
        PUBLISH, SUBSCRIBE
    }

    enum OperationRoleType {
        EVENT_PRODUCER("EventsProducer"),
        EVENT_CONSUMER("EventsConsumer"),
        COMMAND_PRODUCER("CommandsProducer"),
        COMMAND_CONSUMER("CommandsConsumer");

        private String serviceSuffix;

        OperationRoleType(String serviceSuffix) {
            this.serviceSuffix = serviceSuffix;
        }

        public String getServiceSuffix() {
            return serviceSuffix;
        }

        public static OperationRoleType valueOf(RoleType roleType, OperationType operationType) {

            if(operationType == OperationType.PUBLISH) {
                return roleType == RoleType.PROVIDER? EVENT_PRODUCER : EVENT_CONSUMER;
            } else if (operationType == OperationType.SUBSCRIBE) {
                return roleType == RoleType.PROVIDER? COMMAND_CONSUMER : COMMAND_PRODUCER;
            }
            return null;
        }
    }

    @DocumentedOption(description = "Java API package name")
    public String apiPackage = "io.example.api";
    @DocumentedOption(description = "Java Models package name")
    public String modelPackage = "io.example.api.model";
    @DocumentedOption(description = "Binding names to include in code generation. Generates code for ALL bindings if left empty")
    public List<String> bindingTypes;
    @DocumentedOption(description = "Project role: PROVIDER\\|CLIENT")
    public RoleType role = RoleType.PROVIDER;

    public String getApiPackageFolder() {
        return this.apiPackage.replaceAll("\\.", "/");
    }

    public String getModelPackageFolder() {
        return this.apiPackage.replaceAll("\\.", "/");
    }

    public Map<String, List<Map<String, Object>>> getPublishOperationsGroupedByTag(Model apiModel) {
        return getOperationsGroupedByTag(apiModel, OperationType.PUBLISH);
    }

    public Map<String, List<Map<String, Object>>> getSubscribeOperationsGroupedByTag(Model model) {
        return getOperationsGroupedByTag(model, OperationType.SUBSCRIBE);
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTag(Model apiModel, OperationType operationType) {
        Map<String, List<Map<String, Object>>> operationsByTag = new HashMap<>();
        List<Map<String, Object>> operations = JsonPath.read(apiModel, "$.channels[*].*");
        for (Map<String, Object> operation : operations) {
            if (matchesFilters(operation, operationType)) {
                String tag = (String) ObjectUtils.firstNonNull(operation.get("x--normalizedTagName"), "DefaultService");
                if (!operationsByTag.containsKey(tag)) {
                    operationsByTag.put(tag, new ArrayList<>());
                }
                operationsByTag.get(tag).add(operation);
            }
        }
        return operationsByTag;
    }

    public boolean matchesFilters(Map<String, Object> operation, OperationType operationType) {
        var operationOperationType = OperationType.valueOf(operation.get("x--operationType").toString().toUpperCase());
        return operationOperationType == operationType && matchesBindingTypes(operation, bindingTypes);
    }

    /**
     * Returns true if bindingTypes is set and operation bindings contains any of bindingTypes.
     *
     * @param operation
     * @param bindingTypes
     * @return
     */
    public boolean matchesBindingTypes(Map<String, Object> operation, List<String> bindingTypes) {
        if (bindingTypes == null || bindingTypes.isEmpty()) {
            return true;
        }
        Map<String, Object> bindings = ObjectUtils.defaultIfNull((Map<String, Object>) operation.get("bindings"), Collections.emptyMap());
        Set<String> bindingNames = bindings.keySet();
        for (String bindingName : bindingNames) {
            if(bindingTypes.contains(bindingName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if generating code for a PROVIDER and operation is an event(operationType=publish) or project is a CLIENT and operation is a command(operationType=subscribe).
     *
     * @param operation
     * @return
     */
    public boolean isProducer(Map<String, Object> operation) {
        var operationType = OperationType.valueOf(operation.get("x--operationType").toString().toUpperCase());
        return isProducer(this. role, operationType);
    }

    /**
     * Returns true for a PROVIDER and operation is an event(operationType=publish) or a CLIENT and operation is a command(operationType=subscribe).
     *
     * @param roleType
     * @param operationType
     * @return
     */
    public boolean isProducer(RoleType roleType, OperationType operationType) {
        if (RoleType.PROVIDER == role && OperationType.PUBLISH == operationType || RoleType.CLIENT == role && OperationType.SUBSCRIBE == operationType) {
            return true;
        }
        return false;
    }
}
