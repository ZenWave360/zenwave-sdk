package io.zenwave360.sdk.plugins;

import java.util.*;

import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.templating.*;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.Model;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class SpringCloudStreams3TestsGenerator extends SpringCloudStreams3Generator {

    @DocumentedOption(description = "Package name for generated tests")
    public String testsPackage = "{{consumerApiPackage}}";

    @DocumentedOption(description = "Class name suffix for generated test classes")
    public String testSuffix = "IT";

    public String baseTestClassName = "BaseConsumerTest";

    public String baseTestClassPackage = "{{consumerApiPackage}}";

    @DocumentedOption(description = "Annotate tests as @Transactional")
    public boolean transactional = true;

    @DocumentedOption(description = "@Transactional annotation class name")
    public String transactionalAnnotationClass = "org.springframework.transaction.annotation.Transactional";

    private String prefix = templatesPath + "/consumer/tests/";

    private final TemplateInput baseTestClassTemplate = new TemplateInput(prefix + "BaseConsumerTest.java", "src/test/java/{{asPackageFolder baseTestClassPackage}}/{{baseTestClassName}}.java").withMimeType(JAVA).withSkipOverwrite(true);

    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "{{style}}/ConsumerTest.java", "src/test/java/{{asPackageFolder testsPackage}}/{{serviceName operation.x--operationIdCamelCase}}{{testSuffix}}.java").withMimeType(JAVA);

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);

        Map<String, List<Map<String, Object>>> subscribeOperations = getSubscribeOperationsGroupedByTag(apiModel);
        Map<String, List<Map<String, Object>>> publishOperations = getPublishOperationsGroupedByTag(apiModel);

        List<Map<String, Object>> consumerOperations = new ArrayList<>();
        consumerOperations.addAll(filterConsumerOperations(subscribeOperations, AsyncapiOperationType.subscribe));
        consumerOperations.addAll(filterConsumerOperations(publishOperations, AsyncapiOperationType.publish));

        List<TemplateOutput> templateOutputList = new ArrayList<>();
        templateOutputList.addAll(generateTemplateOutput(contextModel, baseTestClassTemplate, null));
        for (Map<String, Object> consumerOperation : consumerOperations) {
            templateOutputList.addAll(generateTemplateOutput(contextModel, operationTestTemplate, consumerOperation));
        }
        return templateOutputList;
    }

    protected List<Map<String, Object>> filterConsumerOperations(Map<String, List<Map<String, Object>>> operationsByTags, AsyncapiOperationType publishOrSubscribe) {
        List<Map<String, Object>> consumerOperations = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> operationsByTag : operationsByTags.entrySet()) {
            OperationRoleType operationRoleType = OperationRoleType.valueOf(role, publishOrSubscribe);
            if(!operationRoleType.isProducer()) {
                operationsByTag.getValue().forEach(operation -> {
                    operation.put("apiClassName", getApiClassName(operationsByTag.getKey(), operationRoleType));
                    consumerOperations.add(operation);
                });
            }
        }

        return consumerOperations;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> operation) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("operation", operation);
        return getTemplateEngine().processTemplate(model, template);
    }
}
