package io.zenwave360.sdk.generators;

import java.util.*;
import java.util.function.Function;

import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.apache.commons.lang3.ObjectUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.utils.JSONPath;

import javax.xml.transform.Templates;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public abstract class AbstractAsyncapiGenerator extends Generator {

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

    public String sourceProperty = "api";

    @DocumentedOption(description = "Java API package, if `producerApiPackage` and `consumerApiPackage` are not set.")
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

    @DocumentedOption(description = "Operation ids to exclude in code generation. Skips code generation if is not included or is excluded.")
    public List<String> excludeOperationIds = new ArrayList<>();

    private final HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(sourceProperty);
    }

    protected abstract Templates configureTemplates();

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Templates templates = configureTemplates();

        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> subscribeOperations = getSubscribeOperationsGroupedByTag(apiModel);
        Map<String, List<Map<String, Object>>> publishOperations = getPublishOperationsGroupedByTag(apiModel);
        Map<String, Map<String, Object>> producerServicesMap = new HashMap<>();
        Map<String, Map<String, Object>> consumerServicesMap = new HashMap<>();

        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.addAll(generateTemplateOutput(contextModel, templates.commonTemplates, Map.of()));

        for (Map.Entry<String, List<Map<String, Object>>> operationsByTag : subscribeOperations.entrySet()) {
            AbstractAsyncapiGenerator.OperationRoleType operationRoleType = AbstractAsyncapiGenerator.OperationRoleType.valueOf(role, AsyncapiOperationType.subscribe);
            generatedProjectFiles.singleFiles.addAll(processServiceOperations(contextModel, operationsByTag, operationRoleType, templates));
            addToServicesMap(operationRoleType.isProducer()? producerServicesMap : consumerServicesMap, operationsByTag, operationRoleType);
        }
        for (Map.Entry<String, List<Map<String, Object>>> operationsByTag : publishOperations.entrySet()) {
            AbstractAsyncapiGenerator.OperationRoleType operationRoleType = AbstractAsyncapiGenerator.OperationRoleType.valueOf(role, AsyncapiOperationType.publish);
            generatedProjectFiles.singleFiles.addAll(processServiceOperations(contextModel, operationsByTag, operationRoleType, templates));
            addToServicesMap(operationRoleType.isProducer()? producerServicesMap : consumerServicesMap, operationsByTag, operationRoleType);
        }

        if (!producerServicesMap.isEmpty()) {
            generatedProjectFiles.singleFiles.addAll(generateTemplateOutput(contextModel, templates.producerTemplates, Map.of("services", producerServicesMap)));
        }

        if (!consumerServicesMap.isEmpty()) {
            generatedProjectFiles.singleFiles.addAll(generateTemplateOutput(contextModel, templates.consumerTemplates, Map.of("services", consumerServicesMap)));
        }

        return generatedProjectFiles;
    }

    public List<TemplateOutput> processServiceOperations(Map<String, Object> contextModel, Map.Entry<String, List<Map<String, Object>>> operationsByTag, AbstractAsyncapiGenerator.OperationRoleType operationRoleType, Templates templates) {
        boolean isProducer = operationRoleType.isProducer();
        var serviceName = operationsByTag.getKey();
        var operations = operationsByTag.getValue();
        var messages = new HashSet(JSONPath.get(operations, "$[*].x--messages[*]"));

        List<TemplateOutput> templateOutputList = new ArrayList<>();
        templateOutputList.addAll(generateTemplateOutput(contextModel, isProducer? templates.producerByServiceTemplates : templates.consumerByServiceTemplates,
                Map.of("serviceName", serviceName, "operations", operations, "messages", messages, "operationRoleType", operationRoleType)));

        Map<String, List<Map<String, Object>>> operationsByChannel = new HashMap<>();
        for (Map<String, Object> operation : operations) {
            messages = new HashSet(JSONPath.get(operation, "$.x--messages[*]"));
            templateOutputList.addAll(generateTemplateOutput(contextModel, isProducer? templates.producerByOperationTemplates : templates.consumerByOperationTemplates,
                    Map.of("serviceName", serviceName, "operation", operation, "messages", messages, "operationRoleType", operationRoleType)));

            String channelName = (String) JSONPath.get(operation, "$.x--channel");
            operationsByChannel.computeIfAbsent(channelName, k -> new ArrayList<>()).add(operation);
        }

        operationsByChannel.forEach((channelName, channelOperations) -> {
            var channel = JSONPath.get(getApiModel(contextModel), "$.channels['" + channelName + "']");
//            var messageList = JSONPath.getFirst(channel, "$[*].x--messages[*]", "$.x--messages[*]");
            templateOutputList.addAll(generateTemplateOutput(contextModel, isProducer? templates.producerByChannelTemplates : templates.consumerByChannelTemplates,
                    Map.of("serviceName", serviceName, "channelName", channelName, "channel", channel, "operations", channelOperations,"operationRoleType", operationRoleType)));

        });


        return templateOutputList;
    }

    public void addToServicesMap(Map<String, Map<String, Object>> servicesMap, Map.Entry<String, List<Map<String, Object>>> operationsByTag, AbstractAsyncapiGenerator.OperationRoleType operationRoleType) {
        var serviceName = operationsByTag.getKey();
        var operations = operationsByTag.getValue();
        servicesMap.put(serviceName, Map.of("operations", operations, "operationRoleType", operationRoleType));
    }

    public Map<String, List<Map<String, Object>>> getPublishOperationsGroupedByTag(Model apiModel) {
        return getOperationsGroupedByTag(apiModel, AsyncapiOperationType.publish);
    }

    public Map<String, List<Map<String, Object>>> getSubscribeOperationsGroupedByTag(Model model) {
        return getOperationsGroupedByTag(model, AsyncapiOperationType.subscribe);
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTag(Model apiModel, AsyncapiOperationType operationType) {
        boolean isV2 = AsyncapiVersionType.isV2(apiModel);
        boolean isV3 = AsyncapiVersionType.isV3(apiModel);
        if(isV2) {
            return getOperationsGroupedByTagV2(apiModel, operationType);
        } else if(isV3) {
            return getOperationsGroupedByTagV3(apiModel, operationType);
        }
        return null;
    }

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTagV2(Model apiModel, AsyncapiOperationType operationType) {
        Map<String, List<Map<String, Object>>> operationsByTag = new HashMap<>();
        List operations = JSONPath.get(apiModel, "$.channels[*].*");
        for (Object operationObject : operations) {
            if(operationObject != null && JSONPath.get(operationObject, "$.operationId") == null) {
                continue;
            }
            Map<String, Object> operation = (Map) operationObject;
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

    public Map<String, List<Map<String, Object>>> getOperationsGroupedByTagV3(Model apiModel, AsyncapiOperationType operationType) {
        Map<String, List<Map<String, Object>>> operationsByTag = new HashMap<>();
        List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.operations[*]");
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
        var operationOperationType = getOperationType(operation);
        return operationType.isEquivalent(operationOperationType) && matchesBindingTypes(operation, bindingTypes) && !isSkipOperation(operation);
    }

    private AsyncapiOperationType getOperationType(Map<String, Object> operation) {
        var v2OperationType = operation.get("x--operationType");
        var v3Action = operation.get("action");
        var operationType = ObjectUtils.firstNonNull(v2OperationType, v3Action);
        return operationType != null? AsyncapiOperationType.valueOf(operationType.toString()) : null;
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

    public boolean isSkipOperation(Map<String, Object> operation) {
        boolean isIncluded = true;
        if(operationIds != null && !operationIds.isEmpty()) {
            isIncluded = operationIds.contains((String) operation.get("operationId"));
        }
        boolean isExcluded = false;
        if(excludeOperationIds != null && !excludeOperationIds.isEmpty()) {
            isExcluded = excludeOperationIds.contains((String) operation.get("operationId"));
        }
        return !isIncluded || isExcluded;
    }


    /**
     * Returns true for a provider and operation is an event(operationType=publish) or a client and operation is a command(operationType=subscribe).
     *
     * @param roleType
     * @param operationType
     * @return
     */
    public boolean isProducer(AsyncapiRoleType roleType, AsyncapiOperationType operationType) {
        if ((AsyncapiRoleType.provider == roleType && AsyncapiOperationType.publish.isEquivalent(operationType)) || (AsyncapiRoleType.client == roleType && AsyncapiOperationType.subscribe.isEquivalent(operationType))) {
            return true;
        }
        return false;
    }

    protected List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, List<TemplateInput> templates, Map<String, Object> extModel) {
        Map<String, Object> baseModel = new HashMap<>();
        baseModel.putAll(this.asConfigurationMap());
        baseModel.put("context", contextModel);
        baseModel.put("asyncapi", getApiModel(contextModel));
        // baseModel.putAll(this.templates.getDocumentedOptions());

        var templateOutputList = new ArrayList<TemplateOutput>();
        for (TemplateInput template : templates) {
            var model = new HashMap<>(baseModel);
            model.putAll(extModel);
            templateOutputList.addAll(getTemplateEngine().processTemplates(model, List.of(template)));
        }
        return templateOutputList;
    }

    public static class Templates {

        public final String templatesFolder;

        public Templates(String templatesFolder) {
            this.templatesFolder = templatesFolder;
        }


        public List<TemplateInput> commonTemplates = new ArrayList<>();

        public List<TemplateInput> producerTemplates = new ArrayList<>();
        public List<TemplateInput> producerByServiceTemplates = new ArrayList<>();
        public List<TemplateInput> producerByOperationTemplates = new ArrayList<>();
        public List<TemplateInput> producerByChannelTemplates = new ArrayList<>();

        public List<TemplateInput> consumerTemplates = new ArrayList<>();
        public List<TemplateInput> consumerByServiceTemplates = new ArrayList<>();
        public List<TemplateInput> consumerByOperationTemplates = new ArrayList<>();
        public List<TemplateInput> consumerByChannelTemplates = new ArrayList<>();

        public void addTemplate(List<TemplateInput> templates, String templateLocation, String targetFile) {
            addTemplate(templates, templateLocation, targetFile, JAVA, null, false);
        }

        public void addTemplate(List<TemplateInput> templates, String templateLocation, String targetFile, OutputFormatType mimeType, Function<Map<String, Object>, Boolean> skip, boolean skipOverwrite) {
            var template = new TemplateInput()
                    .withTemplateLocation(templatesFolder + "/" + templateLocation)
                    .withTargetFile(targetFile)
                    .withMimeType(mimeType)
                    .withSkipOverwrite(skipOverwrite)
                    .withSkip(skip);
            templates.add(template);
        }
    }
}
