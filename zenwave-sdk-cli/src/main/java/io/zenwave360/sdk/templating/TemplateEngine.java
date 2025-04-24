package io.zenwave360.sdk.templating;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TemplateEngine {
    String processInline(String template, Map<String, Object> model) throws IOException;

    TemplateOutput processTemplate(Map<String, Object> model, TemplateInput templateInput);

    List<TemplateOutput> processTemplates(Map<String, Object> model, List<TemplateInput> templateInputs);

    List<TemplateOutput> processTemplateNames(Map<String, Object> model, TemplateInput templateInput);

    List<TemplateOutput> processTemplateNames(Map<String, Object> apiModel, List<TemplateInput> templateInputs);
}
