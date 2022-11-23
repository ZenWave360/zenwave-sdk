package io.zenwave360.generator.plugins;

import java.util.ArrayList;
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

public class PactConsumerGenerator extends AbstractOpenAPIGenerator {

    enum GroupByType {
        service, operation, partial
    }

    public String sourceProperty = "api";

    @DocumentedOption(description = "The package to generate Consumer Contract Test")
    public String testsPackage = "{{basePackage}}.tests.contract";

    @DocumentedOption(description = "Generate test classes grouped by", required = true)
    public GroupByType groupBy = GroupByType.operation;

    @DocumentedOption(description = "Class name suffix for generated test classes")
    public String testSuffix = "IT";

    public PactConsumerGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String prefix = "io/zenwave360/generator/plugins/PactConsumerGenerator/";
    private final TemplateInput partialTemplate = new TemplateInput(prefix + "partials/Operation.java", "{{asPackageFolder testsPackage}}/{{asJavaTypeName operationId}}ConsumerContractTest.java");
    private final TemplateInput operationTestTemplate = new TemplateInput(prefix + "ConsumerContractTest.java", "{{asPackageFolder testsPackage}}/{{asJavaTypeName operationId}}ConsumerContractTest.java");

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
        if (groupBy == GroupByType.operation) {
            List<Map<String, Object>> operations = operationsByService.values().stream().flatMap(List::stream).collect(Collectors.toList());
            for (Map<String, Object> operation : operations) {
                templateOutputList.add(generateTemplateOutput(contextModel, operationTestTemplate, (String) operation.get("operationId"), List.of(operation)));
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

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, String operationId, List<Map<String, Object>> operations) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getApiModel(contextModel));
        model.put("operationId", operationId);
        model.put("operations", operations);
        return getTemplateEngine().processTemplate(model, template).get(0);
    }
}
