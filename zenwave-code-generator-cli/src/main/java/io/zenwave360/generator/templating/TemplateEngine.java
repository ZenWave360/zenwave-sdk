package io.zenwave360.generator.templating;

import java.util.List;
import java.util.Map;

public interface TemplateEngine {
    List<TemplateOutput> processTemplate(Map<String, Object> model, TemplateInput templateInput);

    List<TemplateOutput> processTemplates(Map<String, Object> model, List<TemplateInput> templateInputs);

    List<TemplateOutput> processTemplates(String modelPrefix, Map<String, Object> apiModel, List<TemplateInput> templateInputs);
}
