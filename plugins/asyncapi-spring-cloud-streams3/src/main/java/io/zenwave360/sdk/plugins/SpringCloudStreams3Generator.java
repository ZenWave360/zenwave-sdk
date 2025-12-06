package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.utils.JSONPath;

public class SpringCloudStreams3Generator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public enum TransactionalOutboxType {
        none, modulith, mongodb, jdbc
    }

    @DocumentedOption(description = "Programming style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Transactional outbox type for message producers.")
    public TransactionalOutboxType transactionalOutbox = TransactionalOutboxType.none;

    @DocumentedOption(description = "Include ApplicationEvent listener for consuming messages within the modulith.")
    public boolean includeApplicationEventListener = false;

    @DocumentedOption(description = "Generate only the producer interface and skip the implementation.")
    public boolean skipProducerImplementation = false;

    @DocumentedOption(description = "Whether to expose underlying spring Message to consumers or not.")
    public boolean exposeMessage = false;

    @DocumentedOption(description = "Include support for enterprise envelop wrapping/unwrapping.")
    public boolean useEnterpriseEnvelope = false;

    @DocumentedOption(description = "AsyncAPI Message extension name for the envelop java type for wrapping/unwrapping.")
    public String envelopeJavaTypeExtensionName = "x-envelope-java-type";

    @DocumentedOption(description = "To avoid method erasure conflicts, when exposeMessage or reactive style this character will be used as separator to append message payload type to method names in consumer interfaces.")
    public String methodAndMessageSeparator = "$";

    @DocumentedOption(description = "SC Streams Binding Name Prefix (used in @Component name)"  )
    public String bindingPrefix = "";

    @DocumentedOption(description = "SC Streams Binder class prefix")
    public String consumerPrefix = "";

    @DocumentedOption(description = "SC Streams Binder class suffix")
    public String consumerSuffix = "Consumer";

    @DocumentedOption(description = "Business/Service interface prefix")
    public String consumerServicePrefix = "I";

    @DocumentedOption(description = "Business/Service interface suffix")
    public String consumerServiceSuffix = "ConsumerService";

    @DocumentedOption(description = "Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. `<operation-id>-in-0`")
    public String bindingSuffix = "-0";

    @DocumentedOption(description = "AsyncAPI extension property name for runtime auto-configuration of headers.")
    public String runtimeHeadersProperty = "x-runtime-expression";

    @DocumentedOption(description = "Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports.")
    public String generatedAnnotationClass;

    private final HandlebarsEngine handlebarsEngine = (HandlebarsEngine) getTemplateEngine();
    {
        handlebarsEngine.getHandlebars().registerHelper("producerInterfaceName", (serviceName, options) -> {
            AbstractAsyncapiGenerator.OperationRoleType operationRoleType = options.param(0);
            return getApiClassName((String) serviceName, operationRoleType);
        });
        handlebarsEngine.getHandlebars().registerHelper("producerClassName", (serviceName, options) -> {
            AbstractAsyncapiGenerator.OperationRoleType operationRoleType = options.param(0);
            return "Default" + getApiClassName((String) serviceName, operationRoleType);
        });
        handlebarsEngine.getHandlebars().registerHelper("producerInMemoryName", (serviceName, options) -> {
            AbstractAsyncapiGenerator.OperationRoleType operationRoleType = options.param(0);
            return "InMemory" + getApiClassName((String) serviceName, operationRoleType);
        });
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
        handlebarsEngine.getHandlebars().registerHelper("consumerServiceInterfaceName", (context, options) -> {
            return String.format("%s%s%s", consumerServicePrefix, context, consumerServiceSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("consumerServiceName", (context, options) -> {
            return String.format("%s%s", context, consumerServiceSuffix);
        });
        handlebarsEngine.getHandlebars().registerHelper("testDoubleName", (context, options) -> {
            return String.format("%s%s%s", context, consumerServiceSuffix, "TestDouble");
        });
        handlebarsEngine.getHandlebars().registerHelper("methodSuffix", (context, options) -> {
            boolean doExposeMessage = "true".equals(String.valueOf(options.hash.get("exposeMessage")));
            boolean isProducer = "true".equals(String.valueOf(options.hash.get("producer")));;
            if (doExposeMessage || exposeMessage || style == ProgrammingStyle.reactive) {
                int messagesCount = JSONPath.get(options.param(0), "$.x--messages.length()", 0);
                if (messagesCount > 1) {
                    String messageJavaType = JSONPath.get(context, "$.x--javaTypeSimpleName");
                    return String.format("%s%s", methodAndMessageSeparator, messageJavaType);
                }
            }
            return null;
        });
        handlebarsEngine.getHandlebars().registerHelper("hasRuntimeHeaders", (context, options) -> {
            // operations[] or message
            var path = context instanceof List? "$[*].x--messages[*].headers.properties[*]" : "$.headers.properties[*]";
            return !JSONPath.get(context, path + runtimeHeadersProperty, Collections.emptyList()).isEmpty();
        });
        handlebarsEngine.getHandlebars().registerHelper("runtimeHeadersMap", (message, options) -> {
            List<String> runtimeHeaders = new ArrayList<>();
            Map<String, Map> headers = JSONPath.get(message, "$.headers.properties");
            for (String header : headers.keySet()) {
                String location = JSONPath.get(headers.get(header), "$." + runtimeHeadersProperty);
                if(location != null) {
                    runtimeHeaders.add("\"" + header + "\"");
                    runtimeHeaders.add("\"" + StringEscapeUtils.escapeJava(location) + "\"");
                }
            }
            return runtimeHeaders.stream().collect(Collectors.joining(", "));
        });
        handlebarsEngine.getHandlebars().registerHelper("propertyType", (context, options) -> {
            Map property = (Map) context;
            String type = (String) property.get("type");
            String format = (String) property.get("format");
            if ("date".equals(format)) {
                return "LocalDate";
            }
            if ("date-time".equals(format)) {
                return "Instant";
            }
            if ("integer".equals(type) && "int32".equals(format)) {
                return "Integer";
            }
            if ("integer".equals(type) && "int64".equals(format)) {
                return "Long";
            }
            if ("number".equals(type)) {
                return "BigDecimal";
            }
            if ("boolean".equals(type)) {
                return "Boolean";
            }
            if("string".equals(type)) {
                return "String";
            }
            return "Object";
        });
    }

    protected String templatesPath = "io/zenwave360/sdk/plugins/SpringCloudStream3Generator";

    protected Templates configureTemplates() {
        var ts = new Templates(templatesPath);

        ts.addTemplate(ts.producerTemplates, "producer/mocks/EventsProducerInMemoryContext.java", "src/test/java/{{asPackageFolder producerApiPackage}}/EventsProducerInMemoryContext.java");

        ts.addTemplate(ts.producerByServiceTemplates, "producer/IProducer.java", "src/main/java/{{asPackageFolder producerApiPackage}}/{{producerInterfaceName serviceName operationRoleType}}.java");
        ts.addTemplate(ts.producerByServiceTemplates, "producer/outbox/{{transactionalOutbox}}/Producer.java", "src/main/java/{{asPackageFolder producerApiPackage}}/{{producerClassName serviceName operationRoleType}}.java", JAVA, (context) -> skipProducerImplementation, false);
        ts.addTemplate(ts.producerByServiceTemplates, "producer/mocks/InMemoryEventsProducer.java", "src/test/java/{{asPackageFolder producerApiPackage}}/{{producerInMemoryName serviceName operationRoleType}}.java");

        ts.addTemplate(ts.consumerByOperationTemplates, "consumer/{{style}}/Consumer.java", "src/main/java/{{asPackageFolder consumerApiPackage}}/{{consumerName operation.x--operationIdCamelCase}}.java");
        ts.addTemplate(ts.consumerByOperationTemplates, "consumer/{{style}}/IService.java", "src/main/java/{{asPackageFolder consumerApiPackage}}/{{consumerServiceInterfaceName operation.x--operationIdCamelCase}}.java");
        return ts;
    }

    public static String getApiClassName(String serviceName, AbstractAsyncapiGenerator.OperationRoleType operationRoleType) {
        return operationRoleType != null? serviceName + operationRoleType.getServiceSuffix() : serviceName;
    }

}
