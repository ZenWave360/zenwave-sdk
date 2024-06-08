package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.generators.JsonSchemaToJsonFaker;
import io.zenwave360.sdk.options.WebFlavorType;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractOpenAPIGenerator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class SpringWebTestClientGenerator extends AbstractOpenAPIGenerator {

    public enum GroupByType {
        service, operation, partial, businessFlow
    }

    public enum RequestPayloadType {
        json, dto
    }

    public String apiProperty = "api";

    @DocumentedOption(description = "Package name for generated tests")
    public String testsPackage;
    public String baseTestClassName = "BaseWebTestClientTest";
    public String baseTestClassPackage;

    @DocumentedOption(description = "Generate test classes grouped by", required = true)
    public GroupByType groupBy = GroupByType.service;

    public WebFlavorType webFlavor = WebFlavorType.mvc;

    @DocumentedOption(description = "Class name suffix for generated test classes")
    public String testSuffix = "IntegrationTest";

    @DocumentedOption(description = "Business Flow Test name")
    public String businessFlowTestName;

    @DocumentedOption(description = "Annotate tests as @Transactional")
    public boolean transactional = true;

    @DocumentedOption(description = "@Transactional annotation class name")
    public String transactionalAnnotationClass = "org.springframework.transaction.annotation.Transactional";

    @DocumentedOption(description = "Whether to use a JSON string or instantiate a java DTO as request payload")
    public RequestPayloadType requestPayloadType = RequestPayloadType.json;

    private final HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final JsonSchemaToJsonFaker jsonSchemaToJsonFaker = new JsonSchemaToJsonFaker();

    public SpringWebTestClientTemplates templates = new SpringWebTestClientTemplates();

    @Override
    public void onPropertiesSet() {
        super.onPropertiesSet();
        if (layout != null) {
            if (this.testsPackage == null) {
                this.testsPackage = layout.adaptersWebPackage;
            }
            if(layout.adaptersWebCommonPackage != null && !layout.adaptersWebCommonPackage.contains("{{")) {
                this.baseTestClassPackage = layout.adaptersWebCommonPackage;
            } else {
                this.baseTestClassPackage = testsPackage;
            }
        }
        templates.getTemplateHelpers(this)
                .forEach(helper -> handlebarsEngine.getHandlebars().registerHelpers(helper));
    }

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(apiProperty);
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> operationsByTag = getOperationsGroupedByTag(apiModel);

        if (groupBy == GroupByType.partial) {
            List<Map<String, Object>> operations = getOperationsByOperationIds(apiModel, operationIds);
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, templates.partialTemplate(), null, operations));
        }

        if (groupBy == GroupByType.businessFlow) {
            List<Map<String, Object>> operations = operationsByTag.values().stream().flatMap(List::stream).collect(Collectors.toList());
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, templates.businessFlowTestTemplate(), null, operations));
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, templates.baseTestClassTemplate(), null, null));
        }

        if (groupBy == GroupByType.service || groupBy == GroupByType.operation) {

            List<String> includedTestNames = new ArrayList<>();
            List<String> includedImports = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : operationsByTag.entrySet()) {
                String serviceName = apiServiceName(entry.getKey());
                if(groupBy == GroupByType.service) {
                    includedTestNames.add(serviceName);
                    generatedProjectFiles.services.addAll(serviceName, List.of(generateTemplateOutput(contextModel, templates.serviceTestTemplate(), serviceName, entry.getValue())));
                } else {
                    List<Map<String, Object>> operations = entry.getValue();
                    includedTestNames.addAll(operations.stream().map(o -> NamingUtils.asJavaTypeName((String) o.get("operationId"))).collect(Collectors.toList()));
                    includedImports.add(serviceName);
                    for (Map<String, Object> operation : operations) {
                        generatedProjectFiles.services.addAll(serviceName, List.of(generateTemplateOutput(contextModel, templates.operationTestTemplate(), serviceName, List.of(operation))));
                    }
                }
            }

//            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, includedImports, includedTestNames));
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, templates.baseTestClassTemplate(), null, null));
        }

        return generatedProjectFiles;
    }

    private String apiServiceName(String tag) {
        return NamingUtils.asJavaTypeName(tag) + "Api";
    }

    public TemplateOutput generateTestSet(Map<String, Object> contextModel, TemplateInput template, Collection<String> includedImports, Collection<String> includedTestNames) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("includedImports", includedImports);
        model.put("includedTestNames", includedTestNames);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, template);
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, String serviceName, List<Map<String, Object>> operations) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("serviceName", serviceName);
        model.put("operations", operations);
        if(operations != null && operations.size() == 1) {
            model.put("operationId", operations.get(0).get("operationId"));
        }
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, template);
    }
}
