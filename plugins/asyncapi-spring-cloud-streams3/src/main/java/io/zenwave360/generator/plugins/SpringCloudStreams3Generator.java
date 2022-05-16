package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.utils.StringInterpolator;
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

    public String targetProperty = "api";

    public SpringCloudStreams3Generator() {
    }

    public SpringCloudStreams3Generator(String targetProperty) {
        this.targetProperty = targetProperty;
    }

    @DocumentedOption(description = "Programming style: IMPERATIVE\\|REACTIVE")
    public ProgrammingStyle style = ProgrammingStyle.IMPERATIVE;

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private List<TemplateInput> producerTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/common/Header", "${apiPackageFolder}/Header.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/IProducer", "${apiPackageFolder}/I${apiClassName}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/Producer", "${apiPackageFolder}/${apiClassName}.java"));
    private List<TemplateInput> consumerReactiveTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/IConsumer", "${apiPackageFolder}/I${operation.x--operationIdCamelCase}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/Consumer", "${apiPackageFolder}/${operation.x--operationIdCamelCase}.java"));
    private List<TemplateInput> consumerImperativeTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/IConsumer", "${apiPackageFolder}/I${operation.x--operationIdCamelCase}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/Consumer", "${apiPackageFolder}/${operation.x--operationIdCamelCase}.java"));

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

    Model getApiModel(Map<String, ?> contextModel) {
        return(Model) contextModel.get(targetProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
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

    public List<TemplateOutput> generateTemplateOutput(Map<String, ?> contextModel, String serviceName, List<Map<String, Object>> operations, OperationRoleType operationRoleType) {
        boolean isProducer = OperationRoleType.COMMAND_PRODUCER == operationRoleType || OperationRoleType.EVENT_PRODUCER == operationRoleType;
        if (isProducer) {
            return generateTemplateOutput(contextModel, getTemplates(isProducer), serviceName, operations, operationRoleType);
        } else {
            return operations.stream().flatMap(operation -> generateTemplateOutput(contextModel, getTemplates(isProducer), serviceName, operation, operationRoleType).stream()).collect(Collectors.toList());
        }
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, ?> contextModel, List<TemplateInput> templates, String serviceName, Map<String, Object> operation, OperationRoleType operationRoleType) {
        Map<String, Object> model = new HashMap<>();
        model.put("generator", asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operation", operation);
        model.put("apiPackageFolder", getApiPackageFolder());
        model.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        return getTemplateEngine().processTemplates(model, processTemplateFilenames(model, templates));
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, ?> contextModel, List<TemplateInput> templates, String serviceName, List<Map<String, Object>> operations, OperationRoleType operationRoleType) {
        Map<String, Object> model = new HashMap<>();
        model.put("generator", asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operations", operations);
        model.put("apiPackageFolder", getApiPackageFolder());
        model.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        return getTemplateEngine().processTemplates(model, processTemplateFilenames(model, templates));
    }

    public List<TemplateInput> processTemplateFilenames(Map<String, Object> model, List<TemplateInput> templateInputs) {
        return templateInputs.stream().map(t -> new TemplateInput(interpolate(t.getTemplateLocation(), model), interpolate(t.getTargetFile(), model), t.getSkip())).collect(Collectors.toList());
    }

    public String interpolate(String template, Map<String, Object> model) {
        return StringInterpolator.interpolate(template, model);
    }
}
