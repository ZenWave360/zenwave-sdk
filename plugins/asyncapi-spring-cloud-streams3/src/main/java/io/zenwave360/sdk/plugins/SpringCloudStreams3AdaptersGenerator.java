package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public class SpringCloudStreams3AdaptersGenerator extends SpringCloudStreams3Generator {

    @DocumentedOption(description = "Applications base package")
    public String basePackage;

    @DocumentedOption(description = "Unique identifier of each AsyncAPI that you consume as a client or provider. It will become the last package token for generated adapters")
    public String apiId = "commands";

    @DocumentedOption(description = "The package to generate Async Inbound Adapters in")
    public String adaptersPackage = "{{basePackage}}.adapters.events.{{apiId}}";

    @DocumentedOption(description = "Package where your inbound dtos are")
    public String inboundDtosPackage = "{{basePackage}}.core.inbound.dtos";

    @DocumentedOption(description = "Package where your domain services/usecases interfaces are")
    public String servicesPackage = "{{basePackage}}.core.inbound";

//    @DocumentedOption(description = "Should use same value configured in BackendApplicationDefaultPlugin. Whether to use an input DTO for entities used as command parameter.")
//    public String inputDTOSuffix = "";

    @DocumentedOption(description = "BaseConsumerTest class name")
    public String baseTestClassName = "BaseConsumerTest";

    @DocumentedOption(description = "BaseConsumerTest package")
    public String baseTestClassPackage = "{{basePackage}}.adapters.events";

    @DocumentedOption(description = "Annotate tests as @Transactional")
    public boolean transactional = true;

    @DocumentedOption(description = "@Transactional annotation class name")
    public String transactionalAnnotationClass = "org.springframework.transaction.annotation.Transactional";


    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    private String prefix = templatesPath + "/adapters/";

    private TemplateInput mapperTemplate = new TemplateInput(prefix + "{{style}}/EventsMapper.java", "src/main/java/{{asPackageFolder adaptersPackage}}/EventsMapper.java", JAVA);
    private TemplateInput adapterTemplate = new TemplateInput(prefix + "{{style}}/ConsumerService.java", "src/main/java/{{asPackageFolder adaptersPackage}}/{{consumerServiceName operation.x--operationIdCamelCase}}.java", JAVA);

    private TemplateInput baseTestTemplate = new TemplateInput(prefix + "{{style}}/BaseConsumerTest.java", "src/test/java/{{asPackageFolder baseTestClassPackage}}/{{baseTestClassName}}.java", JAVA).withSkipOverwrite(true);
    private TemplateInput testTemplate = new TemplateInput(prefix + "{{style}}/ConsumerTest.java", "src/test/java/{{asPackageFolder adaptersPackage}}/{{consumerServiceName operation.x--operationIdCamelCase}}{{testSuffix}}.java", JAVA);

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Model apiModel = getApiModel(contextModel);

        Map<String, List<Map<String, Object>>> subscribeOperations = getSubscribeOperationsGroupedByTag(apiModel);
        Map<String, List<Map<String, Object>>> publishOperations = getPublishOperationsGroupedByTag(apiModel);

        List<Map<String, Object>> consumerOperations = new ArrayList<>();
        consumerOperations.addAll(filterConsumerOperations(subscribeOperations, AsyncapiOperationType.subscribe));
        consumerOperations.addAll(filterConsumerOperations(publishOperations, AsyncapiOperationType.publish));

        List<Map<String, Object>> payloadSchemas = JSONPath.get(consumerOperations, "$.[*]message..payload..[?(@.x--entity)]");

        Map<String, Object> dtoEntityMap = new HashMap<>();
        payloadSchemas.stream().filter(m -> m.get("x--entity") != null).forEach(schema -> {
            String schemaName = (String) schema.get("x--schema-name");
            dtoEntityMap.put(schemaName, Maps.of("schemaName", schemaName, "entity", JSONPath.get(schema, ("x--entity"))));
        });

        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();

        generatedProjectFiles.singleFiles.add(generateMapperTemplates(contextModel, mapperTemplate, dtoEntityMap));
        generatedProjectFiles.singleFiles.add(generateMapperTemplates(contextModel, baseTestTemplate, dtoEntityMap));
        for (Map<String, Object> operation : consumerOperations) {
            generatedProjectFiles.singleFiles.add(generateOperationTemplates(contextModel, adapterTemplate, operation));
            generatedProjectFiles.singleFiles.add(generateOperationTemplates(contextModel, testTemplate, operation));
        }

        return generatedProjectFiles;
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

    public TemplateOutput generateMapperTemplates(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> dtoEntityMap) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("dtoEntityMap", dtoEntityMap);
        return getTemplateEngine().processTemplate(model, template);
    }

    public TemplateOutput generateOperationTemplates(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> operation) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("operation", operation);
        return getTemplateEngine().processTemplate(model, template);
    }
}
