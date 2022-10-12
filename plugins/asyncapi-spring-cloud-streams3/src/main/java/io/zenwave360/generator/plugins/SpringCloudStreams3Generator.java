package io.zenwave360.generator.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractAsyncapiGenerator;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;

public class SpringCloudStreams3Generator extends AbstractAsyncapiGenerator {

    public enum ProgrammingStyle {
        IMPERATIVE, REACTIVE
    }

    public enum TransactionalOutboxType {
        none, mongodb, jdbc
    }

    public String sourceProperty = "api";
    @DocumentedOption(description = "Programming style")
    public ProgrammingStyle style = ProgrammingStyle.IMPERATIVE;

    @DocumentedOption(description = "Transactional outbox type for message producers.")
    public TransactionalOutboxType transactionalOutbox = TransactionalOutboxType.none;

    @DocumentedOption(description = "Whether to expose underlying spring Message to consumers or not. Default: false")
    public boolean exposeMessage = false;

    @DocumentedOption(description = "To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces.")
    public String methodAndMessageSeparator = "$";

    @DocumentedOption(description = "SC Streams Binder class prefix")
    public String consumerPrefix = "";

    @DocumentedOption(description = "SC Streams Binder class suffix")
    public String consumerSuffix = "Consumer";

    @DocumentedOption(description = "Business/Service interface prefix")
    public String servicePrefix = "I";

    @DocumentedOption(description = "Business/Service interface suffix")
    public String serviceSuffix = "ConsumerService";

    @DocumentedOption(description = "Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. <operation-id>-in-0")
    public String bindingSuffix = "-0";

    public SpringCloudStreams3Generator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();
    {
        handlebarsEngine.getHandlebars().registerHelper("consumerName", (context, options) -> {
            return String.format("%s%s%s", consumerPrefix, context, consumerSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("messageType", (operation, options) -> {
            List<String> messageTypes = JSONPath.get(operation, "$.x--messages[*].x--javaType");
            return messageTypes.size() == 1 ? messageTypes.get(0) : "Object";
        });
        handlebarsEngine.getHandlebars().registerHelper("serviceName", (context, options) -> {
            return String.format("%s%s%s", servicePrefix, context, serviceSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("methodSuffix", (context, options) -> {
            if (exposeMessage || style == ProgrammingStyle.REACTIVE) {
                int messagesCount = JSONPath.get(options.param(0), "$.x--messages.length()", 0);
                if (messagesCount > 1) {
                    String messageJavaType = JSONPath.get(context, "$.x--javaType");
                    return String.format("%s%s", methodAndMessageSeparator, messageJavaType);
                }
            }
            return null;
        });
    }

    protected List<TemplateInput> producerTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/common/Header.java", "src/main/java/{{apiPackageFolder}}/Header.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/outbox/{{transactionalOutbox}}/IProducer.java", "src/main/java/{{apiPackageFolder}}/I{{apiClassName}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/producer/outbox/{{transactionalOutbox}}/Producer.java", "src/main/java/{{apiPackageFolder}}/{{apiClassName}}.java"));
    protected List<TemplateInput> consumerReactiveTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/Consumer.java", "src/main/java/{{apiPackageFolder}}/{{consumerName operation.x--operationIdCamelCase}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/reactive/IService.java", "src/main/java/{{apiPackageFolder}}/{{serviceName operation.x--operationIdCamelCase}}.java"));
    protected List<TemplateInput> consumerImperativeTemplates = Arrays.asList(
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/Consumer.java", "src/main/java/{{apiPackageFolder}}/{{consumerName operation.x--operationIdCamelCase}}.java"),
            new TemplateInput("io/zenwave360/generator/plugins/SpringCloudStream3Generator/consumer/imperative/IService.java", "src/main/java/{{apiPackageFolder}}/{{serviceName operation.x--operationIdCamelCase}}.java"));

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
        return (Model) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> subscribeOperations = getSubscribeOperationsGroupedByTag(apiModel);
        Map<String, List<Map<String, Object>>> publishOperations = getPublishOperationsGroupedByTag(apiModel);
        for (Map.Entry<String, List<Map<String, Object>>> entry : subscribeOperations.entrySet()) {
            // boolean isProducer = isProducer(role, OperationType.SUBSCRIBE);
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, OperationType.SUBSCRIBE);
            templateOutputList.addAll(generateTemplateOutput(contextModel, entry.getKey(), entry.getValue(), operationRoleType));
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : publishOperations.entrySet()) {
            // boolean isProducer = isProducer(role, OperationType.PUBLISH);
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
        model.putAll(this.asConfigurationMap());
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
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operations", operations);
        model.put("apiPackageFolder", getApiPackageFolder());
        model.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        return getTemplateEngine().processTemplates(model, templates);
    }
}
