package io.zenwave360.generator.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.zenwave360.generator.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.generator.options.asyncapi.AsyncapiRoleType;
import org.apache.commons.lang3.ObjectUtils;

import com.jayway.jsonpath.JsonPath;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.parsers.Model;

public abstract class AbstractAsyncapiGenerator implements Generator {

    public enum OperationRoleType {
        EVENT_PRODUCER("EventsProducer"), EVENT_CONSUMER("EventsConsumer"), COMMAND_PRODUCER("CommandsProducer"), COMMAND_CONSUMER("CommandsConsumer");

        private String serviceSuffix;

        OperationRoleType(String serviceSuffix) {
            this.serviceSuffix = serviceSuffix;
        }

        public String getServiceSuffix() {
            return serviceSuffix;
        }

        public static OperationRoleType valueOf(AsyncapiRoleType roleType, AsyncapiOperationType operationType) {

            if (operationType == AsyncapiOperationType.publish) {
                return roleType == AsyncapiRoleType.provider ? EVENT_PRODUCER : EVENT_CONSUMER;
            } else if (operationType == AsyncapiOperationType.subscribe) {
                return roleType == AsyncapiRoleType.provider ? COMMAND_CONSUMER : COMMAND_PRODUCER;
            }
            return null;
        }
    }

    @DocumentedOption(description = "Java API package name")
    public String apiPackage;
    @DocumentedOption(description = "Java Models package name")
    public String modelPackage;
    @DocumentedOption(description = "Binding names to include in code generation. Generates code for ALL bindings if left empty")
    public List<String> bindingTypes;
    @DocumentedOption(description = "Project role: provider\\|client")
    public AsyncapiRoleType role = AsyncapiRoleType.provider;

    @DocumentedOption(description = "Operation ids to include in code generation. Generates code for ALL if left empty")
    public List<String> operationIds = new ArrayList<>();

    public String getApiPackageFolder() {
        return this.apiPackage.replaceAll("\\.", "/");
    }

    public String getModelPackageFolder() {
        return this.modelPackage.replaceAll("\\.", "/");
    }

    public Map<String, List<Map<String, Object>>> getPublishOperationsGroupedByTag(Model apiModel) {
        return getOperationsGroupedByTag(apiModel, AsyncapiOperationType.publish);
    }

    public Map<String, List<Map<String, Object>>> getSubscribeOperationsGroupedByTag(Model model) {
        return getOperationsGroupedByTag(model, AsyncapiOperationType.subscribe);
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTag(Model apiModel, AsyncapiOperationType operationType) {
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

    public boolean matchesFilters(Map<String, Object> operation, AsyncapiOperationType operationType) {
        var operationOperationType = AsyncapiOperationType.valueOf(operation.get("x--operationType").toString());
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
            if (bindingTypes.contains(bindingName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if generating code for a provider and operation is an event(operationType=publish) or project is a client and operation is a command(operationType=subscribe).
     *
     * @param operation
     * @return
     */
    public boolean isProducer(Map<String, Object> operation) {
        var operationType = AsyncapiOperationType.valueOf(operation.get("x--operationType").toString());
        return isProducer(this.role, operationType);
    }

    /**
     * Returns true for a provider and operation is an event(operationType=publish) or a client and operation is a command(operationType=subscribe).
     *
     * @param roleType
     * @param operationType
     * @return
     */
    public boolean isProducer(AsyncapiRoleType roleType, AsyncapiOperationType operationType) {
        if ((AsyncapiRoleType.provider == roleType && AsyncapiOperationType.publish == operationType) || (AsyncapiRoleType.client == roleType && AsyncapiOperationType.subscribe == operationType)) {
            return true;
        }
        return false;
    }
}
