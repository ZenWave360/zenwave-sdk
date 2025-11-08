package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.templates.SpringCloudStreamTemplates;
import io.zenwave360.sdk.plugins.templates.AsyncAPIHandlebarsHelpers;
import io.zenwave360.sdk.plugins.templates.SpringKafkaTemplates;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class AsyncAPIGenerator extends AbstractAsyncapiGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public enum TransactionalOutboxType {
        none, modulith
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

    @DocumentedOption(description = "Spring-Boot binding suffix. It will be appended to the operation name kebab-cased. E.g. <operation-id>-in-0")
    public String bindingSuffix = "-0";

    @DocumentedOption(description = "AsyncAPI extension property name for runtime auto-configuration of headers.")
    public String runtimeHeadersProperty = "x-runtime-expression";

    @DocumentedOption(description = "Annotation class to mark generated code (e.g. `org.springframework.aot.generate.Generated`). When retained at runtime, this prevents code coverage tools like Jacoco from including generated classes in coverage reports.")
    public String generatedAnnotationClass;

    @DocumentedOption(description = "Templates to use for code generation.", values = {"SpringCloudStream", "SpringKafka", "FQ Class Name"})
    public String templates = "SpringCloudStream";

    protected Templates configureTemplates() {
        if("SpringCloudStream".equals(templates)) {
            return new SpringCloudStreamTemplates(this);
        }
        if("SpringKafka".equals(templates)) {
            return new SpringKafkaTemplates(this);
        }
        // Instantiate FQ class name
        try {
            return (Templates) Class.forName(templates).getConstructor(AsyncAPIGenerator.class).newInstance(this);
        } catch (Exception e) {
            try {
                return (Templates) Class.forName(templates).getConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getApiClassName(String serviceName, OperationRoleType operationRoleType) {
        return operationRoleType != null? serviceName + operationRoleType.getServiceSuffix() : serviceName;
    }

}
