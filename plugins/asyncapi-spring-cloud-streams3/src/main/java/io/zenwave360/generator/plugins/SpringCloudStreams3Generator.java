package io.zenwave360.generator.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractAsyncapiGenerator;
import io.zenwave360.generator.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.generator.options.ProgrammingStyle;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCloudStreams3Generator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public enum TransactionalOutboxType {
        none, mongodb, jdbc
    }

    public String sourceProperty = "api";
    @DocumentedOption(description = "Programming style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Transactional outbox type for message producers.")
    public TransactionalOutboxType transactionalOutbox = TransactionalOutboxType.none;

    @DocumentedOption(description = "Whether to expose underlying spring Message to consumers or not.")
    public boolean exposeMessage = false;

    @DocumentedOption(description = "Include support for enterprise envelop wrapping/unwrapping.")
    public boolean useEnterpriseEnvelope = false;

    @DocumentedOption(description = "AsyncAPI Message extension name for the envelop java type for wrapping/unwrapping.")
    public String envelopeJavaTypeExtensionName = "x-envelope-java-type";

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
            List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + envelopeJavaTypeExtensionName);
            String operationEnvelop = JSONPath.get(operation, "$." + envelopeJavaTypeExtensionName);
            if(operationEnvelop != null) {
                envelopTypes.add(operationEnvelop);
            }
            if(useEnterpriseEnvelope && !envelopTypes.isEmpty()) {
                return envelopTypes.size() == 1 ? envelopTypes.get(0) : "Object";
            }
            return messageTypes.size() == 1 ? messageTypes.get(0) : "Object";
        });
        handlebarsEngine.getHandlebars().registerHelper("hasEnterpriseEnvelope", (operation, options) -> {
            List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + envelopeJavaTypeExtensionName);
            String operationEnvelop = JSONPath.get(operation, "$." + envelopeJavaTypeExtensionName);
            if(operationEnvelop != null) {
                envelopTypes.add(operationEnvelop);
            }
            return useEnterpriseEnvelope && !envelopTypes.isEmpty();
        });
        handlebarsEngine.getHandlebars().registerHelper("serviceInterfaceName", (context, options) -> {
            return String.format("%s%s%s", servicePrefix, context, serviceSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("serviceName", (context, options) -> {
            return String.format("%s%s", context, serviceSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("testDoubleName", (context, options) -> {
            return String.format("%s%s%s", context, serviceSuffix, "TestDouble");
        });
        handlebarsEngine.getHandlebars().registerHelper("methodSuffix", (context, options) -> {
            boolean doExposeMessage = "true".equals(String.valueOf(options.hash.get("exposeMessage")));
            if (doExposeMessage || exposeMessage || style == ProgrammingStyle.reactive) {
                int messagesCount = JSONPath.get(options.param(0), "$.x--messages.length()", 0);
                if (messagesCount > 1) {
                    String messageJavaType = JSONPath.get(context, "$.x--javaTypeSimpleName");
                    return String.format("%s%s", methodAndMessageSeparator, messageJavaType);
                }
            }
            return null;
        });
    }

    protected String templatesPath = "io/zenwave360/generator/plugins/SpringCloudStream3Generator";

    protected List<TemplateInput> producerTemplates = Arrays.asList(
            new TemplateInput(templatesPath + "/producer/IProducer.java", "src/main/java/{{apiPackageFolder}}/I{{apiClassName}}.java"),
            new TemplateInput(templatesPath + "/producer/outbox/{{transactionalOutbox}}/Producer.java", "src/main/java/{{apiPackageFolder}}/{{apiClassName}}.java"),
            new TemplateInput(templatesPath + "/producer/mocks/ProducerCaptor.java", "src/test/java/{{apiPackageFolder}}/{{apiClassName}}Captor.java"));

    protected List<TemplateInput> producerByServicesTemplates = Arrays.asList(
            new TemplateInput(templatesPath + "/producer/mocks/ProducerInMemoryContext.java", "src/test/java/{{apiPackageFolder}}/ProducerInMemoryContext.java"));
    protected List<TemplateInput> consumerTemplates = Arrays.asList(
            new TemplateInput(templatesPath + "/consumer/{{style}}/Consumer.java", "src/main/java/{{apiPackageFolder}}/{{consumerName operation.x--operationIdCamelCase}}.java"),
            new TemplateInput(templatesPath + "/consumer/{{style}}/IService.java", "src/main/java/{{apiPackageFolder}}/{{serviceInterfaceName operation.x--operationIdCamelCase}}.java"));

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    public List<TemplateInput> getTemplates(boolean isProducer) {
        return isProducer ? producerTemplates : consumerTemplates;
    }

    public static String getApiClassName(String serviceName, OperationRoleType operationRoleType) {
        return operationRoleType != null? serviceName + operationRoleType.getServiceSuffix() : serviceName;
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
        Map<String, Map<String, Object>> producerServicesMap = new HashMap<>();

        for (Map.Entry<String, List<Map<String, Object>>> operationsByTag : subscribeOperations.entrySet()) {
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, AsyncapiOperationType.subscribe);
            templateOutputList.addAll(generateTemplateOutput(contextModel, operationsByTag.getKey(), operationsByTag.getValue(), operationRoleType));

            // prepare for producerMocksTemplates
            if(operationRoleType.isProducer()) {
                producerServicesMap.put(operationsByTag.getKey(), Map.of("operations", operationsByTag.getValue(), "apiClassName", getApiClassName(operationsByTag.getKey(), operationRoleType)));
            }
        }
        for (Map.Entry<String, List<Map<String, Object>>> operationsByTag : publishOperations.entrySet()) {
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, AsyncapiOperationType.publish);
            templateOutputList.addAll(generateTemplateOutput(contextModel, operationsByTag.getKey(), operationsByTag.getValue(), operationRoleType));

            // prepare for producerMocksTemplates
            if(operationRoleType.isProducer()) {
                producerServicesMap.put(operationsByTag.getKey(), Map.of("operations", operationsByTag.getValue(), "apiClassName", getApiClassName(operationsByTag.getKey(), operationRoleType)));
            }
        }

        templateOutputList.addAll(generateTemplateOutput(contextModel, producerByServicesTemplates, producerServicesMap));

        return templateOutputList;
    }

    protected void populateApiClassName(List<Map<String, Object>> operations, String serviceName, OperationRoleType operationRoleType) {
        for (Map<String, Object> operation : operations) {
            operation.put("apiClassName", getApiClassName(serviceName, operationRoleType));
        }
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, String serviceName, List<Map<String, Object>> operations, OperationRoleType operationRoleType) {
        boolean isProducer = operationRoleType.isProducer();
        if (isProducer) {
            // producer operations are grouped by tag
            return generateTemplateOutput(contextModel, getTemplates(isProducer), serviceName, operations, operationRoleType);
        } else {
            // consumer templates are grouped by operationId
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
        model.put("headersPartial", templatesPath + "/common/Headers");
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
        model.put("headersPartial", templatesPath + "/common/Headers");
        return getTemplateEngine().processTemplates(model, templates);
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, List<TemplateInput> templates, Map<String, Map<String, Object>> servicesMap) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("services", servicesMap);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplates(model, templates);
    }
}
