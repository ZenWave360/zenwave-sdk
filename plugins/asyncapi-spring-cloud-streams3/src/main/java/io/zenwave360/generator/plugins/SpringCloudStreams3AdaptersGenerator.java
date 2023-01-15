package io.zenwave360.generator.plugins;

import static io.zenwave360.generator.templating.OutputFormatType.JAVA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.options.ProgrammingStyle;
import io.zenwave360.generator.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;
import io.zenwave360.generator.utils.Maps;

public class SpringCloudStreams3AdaptersGenerator extends SpringCloudStreams3Generator {

    @DocumentedOption(description = "Applications base package")
    public String basePackage;

    @DocumentedOption(description = "Unique identifier of each AsyncAPI that you consume as a client or provider. It will become the last package token for generated adapters")
    public String apiId = "provider";

    @DocumentedOption(description = "The package to generate Async Inbound Adapters in")
    public String adaptersPackage = "{{basePackage}}.adapters.events.{{apiId}}";

    @DocumentedOption(description = "Package where your inbound dtos are")
    public String inboundDtosPackage = "{{basePackage}}.core.inbound.dtos";

    @DocumentedOption(description = "Package where your domain services/usecases interfaces are")
    public String servicesPackage = "{{basePackage}}.core.inbound";

    @DocumentedOption(description = "Suffix for CRUD operations DTOs (default: Input)")
    public String inputDTOSuffix = "Input";


    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    private String prefix = templatesPath + "/adapters/";

    private TemplateInput mapperTemplate = new TemplateInput(prefix + "{{style}}/Mapper.java", "src/main/java/{{asPackageFolder adaptersPackage}}/EventEntityMapper.java", JAVA);
    private TemplateInput adapterTemplate = new TemplateInput(prefix + "{{style}}/Adapter.java", "src/main/java/{{asPackageFolder adaptersPackage}}/{{serviceName operation.x--operationIdCamelCase}}Adapter.java", JAVA);

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
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

        List<TemplateOutput> templateOutputList = new ArrayList<>();

        templateOutputList.addAll(generateMapperTemplates(contextModel, mapperTemplate, dtoEntityMap));
        for (Map<String, Object> operation : consumerOperations) {
            templateOutputList.addAll(generateOperationTemplates(contextModel, adapterTemplate, operation));
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

    public List<TemplateOutput> generateMapperTemplates(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> dtoEntityMap) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("dtoEntityMap", dtoEntityMap);
        return getTemplateEngine().processTemplate(model, template);
    }

    public List<TemplateOutput> generateOperationTemplates(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> operation) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("asyncapi", getApiModel(contextModel));
        model.put("operation", operation);
        return getTemplateEngine().processTemplate(model, template);
    }
}
