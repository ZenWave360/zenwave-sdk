package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.processors.utils.StringInterpolator;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAPIToJDLGenerator extends AbstractJDLGenerator {

    public String targetProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();


    public OpenAPIToJDLGenerator withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput openAPIToJDLTemplate = new TemplateInput("io/zenwave360/generator/plugins/OpenAPIToJDLGenerator/OpenAPIToJDL.jdl", "${generator.targetFile}").withMimeType("text/jdl");

    protected Map<String, ?> getOpenAPIModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(targetProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        Map<String, ?> openAPIModel = getOpenAPIModel(contextModel);
        Map<String, ?> jdlModel = new HashMap<>();


        return List.of(generateTemplateOutput(contextModel, openAPIToJDLTemplate, jdlModel));
    }

    public TemplateOutput generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, Map<String, ?> jdlModel) {
        Map<String, Object> model = new HashMap<>();
        model.put("generator", asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdlModel", jdlModel);
        return getTemplateEngine().processTemplate(model, processTemplateFilename(model, template));
    }

    protected TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    public TemplateInput processTemplateFilename(Map<String, Object> model, TemplateInput templateInput) {
        return new TemplateInput(templateInput).withTemplateLocation(interpolate(templateInput.getTemplateLocation(), model)).withTargetFile(interpolate(templateInput.getTargetFile(), model));
    }

    public String interpolate(String template, Map<String, Object> model) {
        return StringInterpolator.interpolate(template, model);
    }
}
