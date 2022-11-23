package io.zenwave360.generator.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractOpenAPIGenerator;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

public class SpringWebTestClientGenerator extends AbstractOpenAPIGenerator {

    enum GroupByType {
        service, operation, partial
    }

    public String sourceProperty = "api";

    @DocumentedOption(description = "The package to generate REST Controllers")
    public String controllersPackage = "{{basePackage}}.adapters.web";

    @DocumentedOption(description = "Generate test classes grouped by", required = true)
    public GroupByType groupBy = GroupByType.service;

    @DocumentedOption(description = "Class name suffix for generated test classes")
    public String testSuffix = "IT";

    public SpringWebTestClientGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String prefix = "io/zenwave360/generator/plugins/SpringWebTestClientGenerator/";
    private final TemplateInput partialTemplate = new TemplateInput(prefix + "partials/Operation.java", "{{asPackageFolder controllersPackage}}/Operation.java");
    private final TemplateInput testSetTemplate = new TemplateInput(prefix + "ControllersTestSet.java", "{{asPackageFolder controllersPackage}}/ControllersTestSet.java");
    private final TemplateInput serviceTestTemplate = new TemplateInput(prefix + "ServiceIT.java", "{{asPackageFolder controllersPackage}}/{{serviceName}}{{testSuffix}}.java");
    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "OperationIT.java", "{{asPackageFolder controllersPackage}}/{{operation.operationId}}{{testSuffix}}.java");

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    Model getApiModel(Map<String, Object> contextModel) {
        return (Model) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> operationsByService = getOperationsGroupedByTag(apiModel);
        if (groupBy == GroupByType.service) {
            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, operationsByService.keySet()));
            for (Map.Entry<String, List<Map<String, Object>>> entry : operationsByService.entrySet()) {
                templateOutputList.add(generateTemplateOutput(contextModel, serviceTestTemplate, entry.getKey(), entry.getValue()));
            }
        }
        if (groupBy == GroupByType.operation) {
            List<Map<String, Object>> operations = operationsByService.values().stream().flatMap(List::stream).collect(Collectors.toList());
            List<String> operationNames = operations.stream().map(o -> "" + o).collect(Collectors.toList());
            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, operationNames));
            for (Map<String, Object> operation : operations) {
                templateOutputList.add(generateTemplateOutput(contextModel, operationTestTemplate, null, List.of(operation)));
            }
        }
        if (groupBy == GroupByType.partial) {
            List<Map<String, Object>> operations = operationsByService.values().stream().flatMap(List::stream).collect(Collectors.toList());
            templateOutputList.add(generateTemplateOutput(contextModel, partialTemplate, null, operations));
        }
        return templateOutputList;
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("asDtoName", (context, options) -> {
            return StringUtils.isNotBlank((String) context) ? openApiModelNamePrefix + context + openApiModelNameSuffix : null;
        });
    }

    public TemplateOutput generateTestSet(Map<String, Object> contextModel, TemplateInput template, Collection<String> includedTestsNames) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("includedTestsNames", includedTestsNames);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, template).get(0);
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, String serviceName, List<Map<String, Object>> operations) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("serviceName", serviceName + "ApiController");
        model.put("operations", operations);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, template).get(0);
    }
}
