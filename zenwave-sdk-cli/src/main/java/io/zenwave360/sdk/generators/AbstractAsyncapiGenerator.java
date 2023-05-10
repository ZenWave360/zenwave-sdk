package io.zenwave360.sdk.generators;

import java.util.*;

import io.zenwave360.sdk.utils.AsyncAPIUtils;
import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.ObjectUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.sdk.parsers.Model;

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

        public boolean isProducer() {
            return this == EVENT_PRODUCER || this == COMMAND_PRODUCER;
        }
    }

    @DocumentedOption(description = "Java API package name for producerApiPackage and consumerApiPackage if not specified.")
    public String apiPackage;
    @DocumentedOption(description = "Java API package name for outbound (producer) services. It can override apiPackage for producers.")
    public String producerApiPackage = "{{apiPackage}}";
    @DocumentedOption(description = "Java API package name for inbound (consumer) services. It can override apiPackage for consumer.")
    public String consumerApiPackage = "{{apiPackage}}";
    @DocumentedOption(description = "Java Models package name")
    public String modelPackage;
    @DocumentedOption(description = "Binding names to include in code generation. Generates code for ALL bindings if left empty")
    public List<String> bindingTypes;
    @DocumentedOption(description = "Project role: provider/client")
    public AsyncapiRoleType role = AsyncapiRoleType.provider;

    @DocumentedOption(description = "Operation ids to include in code generation. Generates code for ALL if left empty")
    public List<String> operationIds = new ArrayList<>();

    public Map<String, List<Map<String, Object>>> getPublishOperationsGroupedByTag(Model apiModel) {
        return getOperationsGroupedByTag(apiModel, AsyncapiOperationType.publish);
    }

    public Map<String, List<Map<String, Object>>> getSubscribeOperationsGroupedByTag(Model model) {
        return getOperationsGroupedByTag(model, AsyncapiOperationType.subscribe);
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTag(Model apiModel, AsyncapiOperationType operationType) {
        Map<String, List<Map<String, Object>>> operationsByTag = new HashMap<>();
        boolean isV3 = AsyncAPIUtils.isV3(apiModel);
        List<Map<String, Object>> operations = isV3? JSONPath.get(apiModel, "$.operations[*]") : JSONPath.get(apiModel, "$.channels[*].*");
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
        var action = ObjectUtils.firstNonNull(operation.get("action"), operation.get("x--operationType"));
        var operationOperationType = AsyncapiOperationType.valueOf((String) action);
        return operationOperationType.isEquivalent(operationType) && matchesBindingTypes(operation, bindingTypes) && !isSkipOperation(operation);
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

    public boolean isSkipOperation(Map<String, Object> operation) {
        if(operationIds == null || operationIds.isEmpty()) {
            return false;
        }
        return !operationIds.contains((String) operation.get("operationId"));
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
