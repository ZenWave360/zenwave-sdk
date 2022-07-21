package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.Utils;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringCloudStreams3Generator extends AbstractAsyncapiGenerator {

    public enum ProgrammingStyle {
        IMPERATIVE, REACTIVE
    }

    public String sourceProperty = "api";
    @DocumentedOption(description = "Programming style: IMPERATIVE\\|REACTIVE")
    public ProgrammingStyle style = ProgrammingStyle.IMPERATIVE;

    @DocumentedOption(description = "Whether to expose underlying spring Message to consumers or not. Default: false")
    public boolean exposeMessage = false;

    public String consumerSuffix = "Consumer";
    public String serviceSuffix = "Service";

    public SpringCloudStreams3Generator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private List<TemplateInput> producerTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/common/Header.java", "{{apiPackageFolder}}/Header.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/IProducer.java", "{{apiPackageFolder}}/I{{apiClassName}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/Producer.java", "{{apiPackageFolder}}/{{apiClassName}}.java"));
    private List<TemplateInput> consumerReactiveTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/Consumer.java", "{{apiPackageFolder}}/{{operation.x--operationIdCamelCase}}{{consumerSuffix}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/Service.java", "{{apiPackageFolder}}/{{operation.x--operationIdCamelCase}}{{serviceSuffix}}.java"));
    private List<TemplateInput> consumerImperativeTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/Consumer.java", "{{apiPackageFolder}}/{{operation.x--operationIdCamelCase}}{{consumerSuffix}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/Service.java", "{{apiPackageFolder}}/{{operation.x--operationIdCamelCase}}{{serviceSuffix}}.java"));

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    public List<TemplateInput> getTemplates(boolean isProducer) {
        return isProducer ? getProducerTemplates() : getConsumerTemplates();
    }

    public List<TemplateInput> getProducerTemplates() {
        return producerTemplates;
    }

    public List<TemplateInput> getConsumerTemplates() {
        return style == ProgrammingStyle.IMPERATIVE ? consumerImperativeTemplates : consumerReactiveTemplates;
    }

    public String getApiClassName(String serviceName, OperationRoleType operationRoleType) {
        return serviceName + operationRoleType.getServiceSuffix();
    }

    Model getApiModel(Map<String, Object> contextModel) {
        return(Model) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> subscribeOperations = getSubscribeOperationsGroupedByTag(apiModel);
        Map<String, List<Map<String, Object>>> publishOperations = getPublishOperationsGroupedByTag(apiModel);
        for (Map.Entry<String, List<Map<String, Object>>> entry : subscribeOperations.entrySet()) {
//            boolean isProducer = isProducer(role, OperationType.SUBSCRIBE);
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, OperationType.SUBSCRIBE);
            templateOutputList.addAll(generateTemplateOutput(contextModel, entry.getKey(), entry.getValue(), operationRoleType));
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : publishOperations.entrySet()) {
//            boolean isProducer = isProducer(role, OperationType.PUBLISH);
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, OperationType.PUBLISH);
            templateOutputList.addAll(generateTemplateOutput(contextModel, entry.getKey(), entry.getValue(), operationRoleType));
        }
        return templateOutputList;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, String serviceName, List<Map<String, Object>> operations, OperationRoleType operationRoleType) {
        boolean isProducer = OperationRoleType.COMMAND_PRODUCER == operationRoleType || OperationRoleType.EVENT_PRODUCER == operationRoleType;
        if (isProducer) {
            return generateTemplateOutput(contextModel, getTemplates(isProducer), serviceName, operations, operationRoleType);
        } else {
            return operations.stream().flatMap(operation -> generateTemplateOutput(contextModel, getTemplates(isProducer), serviceName, operation, operationRoleType).stream()).collect(Collectors.toList());
        }
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, List<TemplateInput> templates, String serviceName, Map<String, Object> operation, OperationRoleType operationRoleType) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(Utils.asConfigurationMap(this));
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operation", operation);
        model.put("apiPackageFolder", getApiPackageFolder());
        model.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        return getTemplateEngine().processTemplates(model, templates);
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, List<TemplateInput> templates, String serviceName, List<Map<String, Object>> operations, OperationRoleType operationRoleType) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(Utils.asConfigurationMap(this));
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operations", operations);
        model.put("apiPackageFolder", getApiPackageFolder());
        model.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        return getTemplateEngine().processTemplates(model, templates);
    }
}
