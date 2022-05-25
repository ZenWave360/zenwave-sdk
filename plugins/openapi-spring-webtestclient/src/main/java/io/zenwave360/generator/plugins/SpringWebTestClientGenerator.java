package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.utils.StringInterpolator;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringWebTestClientGenerator extends AbstractOpenAPIGenerator {

    enum GroupByType {
        SERVICE, OPERATION, PARTIAL
    }

    public String targetProperty = "api";
    public GroupByType groupBy = GroupByType.SERVICE;

    public String testSuffix = "IT";

    public SpringWebTestClientGenerator withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String prefix = "io/zenwave360/generator/plugins/SpringWebTestClientGenerator/";
    private final TemplateInput partialTemplate = new TemplateInput(prefix + "partials/Operation.java", "${apiPackageFolder}/Operation.java");
    private final TemplateInput testSetTemplate = new TemplateInput(prefix + "ControllersTestSet.java", "${apiPackageFolder}/ControllersTestSet.java");
    private final TemplateInput serviceTestTemplate = new TemplateInput(prefix + "ServiceIT.java", "${apiPackageFolder}/${serviceName}${generator.testSuffix}.java");
    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "OperationIT.java", "${apiPackageFolder}/${operation.operationId}${generator.testSuffix}.java");

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    Model getApiModel(Map<String, ?> contextModel) {
        return(Model) contextModel.get(targetProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Model apiModel = getApiModel(contextModel);
        Map<String, List<Map<String, Object>>> operationsByService = getOperationsGroupedByTag(apiModel);
        if(groupBy == GroupByType.SERVICE) {
            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, operationsByService.keySet()));
            for (Map.Entry<String, List<Map<String, Object>>> entry : operationsByService.entrySet()) {
                templateOutputList.add(generateTemplateOutput(contextModel, serviceTestTemplate, entry.getKey(), entry.getValue()));
            }
        }
        if(groupBy == GroupByType.OPERATION) {
            List<Map<String, Object>> operations = operationsByService.values().stream().flatMap(List::stream).collect(Collectors.toList());
            List<String> operationNames = operations.stream().map(o -> interpolate("", o)).collect(Collectors.toList());
            templateOutputList.add(generateTestSet(contextModel, testSetTemplate, operationNames));
            for (Map<String, Object> operation : operations) {
                templateOutputList.add(generateTemplateOutput(contextModel, operationTestTemplate, null, List.of(operation)));
            }
        }
        if(groupBy == GroupByType.PARTIAL) {
            List<Map<String, Object>> operations = operationsByService.values().stream().flatMap(List::stream).collect(Collectors.toList());
            templateOutputList.add(generateTemplateOutput(contextModel, partialTemplate, null, operations));
        }
        return templateOutputList;
    }

    public TemplateOutput generateTestSet(Map<String, ?> contextModel, TemplateInput template, Collection<String> includedTestsNames) {
        Map<String, Object> model = new HashMap<>();
        model.put("generator", asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("includedTestsNames", includedTestsNames);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, processTemplateFilename(model, template));
    }

    public TemplateOutput generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, String serviceName, List<Map<String, Object>> operations) {
        Map<String, Object> model = new HashMap<>();
        model.put("generator", asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("serviceName", serviceName + "Service");
        model.put("operations", operations);
        model.put("apiPackageFolder", getApiPackageFolder());
        return getTemplateEngine().processTemplate(model, processTemplateFilename(model, template));
    }

    public TemplateInput processTemplateFilename(Map<String, Object> model, TemplateInput templateInput) {
        return new TemplateInput(templateInput.getTemplateLocation(), interpolate(templateInput.getTargetFile(), model), templateInput.getSkip());
    }

    public String interpolate(String template, Map<String, Object> model) {
        return StringInterpolator.interpolate(template, model);
    }
}
