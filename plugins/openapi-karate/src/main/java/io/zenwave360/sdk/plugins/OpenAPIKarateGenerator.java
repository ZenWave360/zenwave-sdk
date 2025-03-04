package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.generators.JsonSchemaToJsonFaker;
import io.zenwave360.sdk.options.WebFlavorType;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractOpenAPIGenerator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;

import static io.zenwave360.sdk.templating.OutputFormatType.GERKIN;
import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class OpenAPIKarateGenerator extends AbstractOpenAPIGenerator {

    enum GroupByType {
        service, operation, businessFlow
    }

    public String apiProperty = "api";

    @DocumentedOption(description = "Package name for generated Karate tests")
    public String testsPackage;

    @DocumentedOption(description = "Generate features grouped by", required = true)
    public GroupByType groupBy = GroupByType.service;

    @DocumentedOption(description = "Business Flow Feature name")
    public String businessFlowTestName;

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private JsonSchemaToJsonFaker jsonSchemaToJsonFaker = new JsonSchemaToJsonFaker();

    private final String prefix = "io/zenwave360/sdk/plugins/OpenAPIKarateGenerator/";
    private final TemplateInput partialTemplate = new TemplateInput(prefix + "partials/Operation.feature", "src/test/resources/{{asPackageFolder testsPackage}}/Operation.feature");
//    private final TemplateInput testSetTemplate = new TemplateInput(prefix + "ControllersTestSet.java", "{{asPackageFolder testsPackage}}/ControllersTestSet.java").withMimeType(JAVA);

    private final TemplateInput businessFlowTestTemplate = new TemplateInput(prefix + "BusinessFlowTest.feature", "src/test/resources/{{asPackageFolder testsPackage}}/{{businessFlowTestName}}.feature").withMimeType(GERKIN);
    private final TemplateInput serviceTestTemplate = new TemplateInput(prefix + "Service.feature", "src/test/resources/{{asPackageFolder testsPackage}}/{{serviceName}}.feature").withMimeType(GERKIN);
    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "Operation.feature", "src/test/resources/{{asPackageFolder testsPackage}}/{{serviceName}}/{{asJavaTypeName operationId}}.feature").withMimeType(GERKIN);

    @Override
    public void onPropertiesSet() {
        super.onPropertiesSet();
        if (layout != null) {
            if (this.testsPackage == null) {
                this.testsPackage = layout.adaptersWebPackage;
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
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> operationsByTag = getOperationsGroupedByTag(apiModel);

        if (groupBy == GroupByType.businessFlow) {
            List<Map<String, Object>> operations = getOperationsByOperationIds(apiModel, operationIds);
            templateOutputList.add(generateTemplateOutput(contextModel, businessFlowTestTemplate, null, operations));
        }

        if (groupBy == GroupByType.service || groupBy == GroupByType.operation) {

            List<String> includedTestNames = new ArrayList<>();
            List<String> includedImports = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : operationsByTag.entrySet()) {
                String serviceName = apiServiceName(entry.getKey());
                if(groupBy == GroupByType.service) {
                    includedTestNames.add(serviceName);
                    templateOutputList.add(generateTemplateOutput(contextModel, serviceTestTemplate, serviceName, entry.getValue()));
                } else {
                    List<Map<String, Object>> operations = entry.getValue();
                    includedTestNames.addAll(operations.stream().map(o -> NamingUtils.asJavaTypeName((String) o.get("operationId"))).collect(Collectors.toList()));
                    includedImports.add(serviceName);
                    for (Map<String, Object> operation : operations) {
                        templateOutputList.add(generateTemplateOutput(contextModel, operationTestTemplate, serviceName, List.of(operation)));
                    }
                }
            }

        }

        return templateOutputList;
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("requestExample", (operation, options) -> {
            return jsonSchemaToJsonFaker.generateExampleAsJson((Map) operation);
        });
        handlebarsEngine.getHandlebars().registerHelper("karatePath", (operation, options) -> {
            String path = JSONPath.get(operation, "x--path");
            return String.join("", "'", path, "'")
                    .replace("{", "', pathParams.")
                    .replace("}", ", '")
                    .replace(", ''", "");
        });

        handlebarsEngine.getHandlebars().registerHelper("queryParams", (operation, options)
                -> JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream().filter(p -> "query" .equals(p.get("in"))).collect(Collectors.toList()));

        handlebarsEngine.getHandlebars().registerHelper("pathParams", (operation, options)
                -> JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream().filter(p -> "path" .equals(p.get("in"))).collect(Collectors.toList()));

        handlebarsEngine.getHandlebars().registerHelper("paramsExample", (params, options) -> {
            return ((Collection<Map>) params).stream()
                    .map(p -> p.get("name") + ": " + firstNonNull(p.get("example"), jsonSchemaToJsonFaker.generateExample((String) p.get("name"), (Map<String, Object>) p.get("schema"))))
                    .collect(Collectors.joining(", "));
        });

    }

    private String apiServiceName(String tag) {
        return NamingUtils.asJavaTypeName(tag) + "Api";
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
            model.put("operation", operations.get(0));
        }
        return getTemplateEngine().processTemplate(model, template).get(0);
    }
}
