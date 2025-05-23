package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.generators.JsonSchemaToJsonFaker;
import io.zenwave360.sdk.options.WebFlavorType;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
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

    enum GroupByType {
        service, operation, partial, businessFlow
    }

    enum RequestPayloadType {
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

    private final String prefix = "io/zenwave360/sdk/plugins/SpringWebTestClientGenerator/";
    private final TemplateInput partialTemplate = new TemplateInput(prefix + "partials/Operation.java", "src/test/java/{{asPackageFolder testsPackage}}/Operation.java");
//    private final TemplateInput testSetTemplate = new TemplateInput(prefix + "ControllersTestSet.java", "{{asPackageFolder testsPackage}}/ControllersTestSet.java").withMimeType(JAVA);

    private final TemplateInput baseTestClassTemplate = new TemplateInput(prefix + "BaseWebTestClientTest.java", "src/test/java/{{asPackageFolder baseTestClassPackage}}/{{baseTestClassName}}.java").withMimeType(JAVA).withSkipOverwrite(true);

    private final TemplateInput businessFlowTestTemplate = new TemplateInput(prefix + "BusinessFlowTest.java", "src/test/java/{{asPackageFolder testsPackage}}/{{businessFlowTestName}}.java").withMimeType(JAVA);
    private final TemplateInput serviceTestTemplate = new TemplateInput(prefix + "ServiceIT.java", "src/test/java/{{asPackageFolder testsPackage}}/{{serviceName}}{{testSuffix}}.java").withMimeType(JAVA);
    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "OperationIT.java", "src/test/java/{{asPackageFolder testsPackage}}/{{serviceName}}/{{asJavaTypeName operationId}}{{testSuffix}}.java").withMimeType(JAVA);

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
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, partialTemplate, null, operations));
        }

        if (groupBy == GroupByType.businessFlow) {
            List<Map<String, Object>> operations = operationsByTag.values().stream().flatMap(List::stream).collect(Collectors.toList());
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, businessFlowTestTemplate, null, operations));
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, baseTestClassTemplate, null, null));
        }

        if (groupBy == GroupByType.service || groupBy == GroupByType.operation) {

            List<String> includedTestNames = new ArrayList<>();
            List<String> includedImports = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : operationsByTag.entrySet()) {
                String serviceName = apiServiceName(entry.getKey());
                if(groupBy == GroupByType.service) {
                    includedTestNames.add(serviceName);
                    generatedProjectFiles.services.addAll(serviceName, List.of(generateTemplateOutput(contextModel, serviceTestTemplate, serviceName, entry.getValue())));
                } else {
                    List<Map<String, Object>> operations = entry.getValue();
                    includedTestNames.addAll(operations.stream().map(o -> NamingUtils.asJavaTypeName((String) o.get("operationId"))).collect(Collectors.toList()));
                    includedImports.add(serviceName);
                    for (Map<String, Object> operation : operations) {
                        generatedProjectFiles.services.addAll(serviceName, List.of(generateTemplateOutput(contextModel, operationTestTemplate, serviceName, List.of(operation))));
                    }
                }
            }

//            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, includedImports, includedTestNames));
            generatedProjectFiles.singleFiles.add(generateTemplateOutput(contextModel, baseTestClassTemplate, null, null));
        }

        return generatedProjectFiles;
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("requestExample", (schema, options) -> {
            return jsonSchemaToJsonFaker.generateExampleAsJson((Map) schema);
        });
        handlebarsEngine.getHandlebars().registerHelper("asDtoName", (context, options) -> {
            return asDtoName((String) context);
        });
        handlebarsEngine.getHandlebars().registerHelper("newPropertyObject", (context, options) -> {
            Map<String, Object> property = (Map<String, Object>) context;
            boolean isObject = "object".equals(property.get("type"));
            boolean isArray = "array".equals(property.get("type"));
            String schemaName = (String) property.get("x--schema-name");
            return isArray? "new java.util.ArrayList<>()" : isObject? String.format("new %s()", asDtoName(schemaName)) : "null";
        });

        handlebarsEngine.getHandlebars().registerHelper("queryParams", (operation, options)
                -> JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream().filter(p -> "query" .equals(p.get("in"))).collect(Collectors.toList()));

        handlebarsEngine.getHandlebars().registerHelper("pathParams", (operation, options)
                -> JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream().filter(p -> "path" .equals(p.get("in"))).collect(Collectors.toList()));
    }

    private String asDtoName(String name) {
        return StringUtils.isNotBlank(name) ? openApiModelNamePrefix + name + openApiModelNameSuffix : null;
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
