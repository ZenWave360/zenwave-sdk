package io.zenwave360.sdk.plugins;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractJDLGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;

public class ZdlToMarkdownGenerator extends AbstractJDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Target file")
    public String targetFile = "zdl-model-glossary.md";

    public ZdlToMarkdownGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput template = new TemplateInput("io/zenwave360/sdk/plugins/ZdlToMarkdownGenerator/ZdlToMarkdownGenerator.md", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Map<String, Object> zdlModel = (Map) contextModel.get(sourceProperty);

        return List.of(generateTemplateOutput(contextModel, template, zdlModel));
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }
}
