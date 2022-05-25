package io.zenwave360.generator.templating;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class __NoOperationTemplateEngine implements TemplateEngine {
    @Override
    public TemplateOutput processTemplate(Map<String, Object> model, TemplateInput templateInput) {
        return new TemplateOutput(templateInput.getTargetFile(), null);
    }

    @Override
    public List<TemplateOutput> processTemplates(Map<String, Object> model, List<TemplateInput> templateInputs) {
        return templateInputs.stream().map(t -> new TemplateOutput(t.getTargetFile(), null)).collect(Collectors.toList());
    }

    @Override
    public List<TemplateOutput> processTemplates(String modelPrefix, Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        return processTemplates(apiModel, templateInputs);
    }
}
