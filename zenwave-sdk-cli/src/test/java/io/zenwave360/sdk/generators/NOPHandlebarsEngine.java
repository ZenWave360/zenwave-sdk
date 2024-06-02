package io.zenwave360.sdk.generators;

import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;

import java.io.IOException;
import java.util.Map;

class NOPHandlebarsEngine extends HandlebarsEngine {

    @Override
    public String processInline(String template, Map<String, Object> model) {
        try {
            return super.processInline(template, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public java.util.List<TemplateOutput> processTemplates(Map<String, Object> model, java.util.List<TemplateInput> templateInputs) {
        return templateInputs.stream()
                .map(templateInput -> new TemplateOutput(templateInput.getTargetFile(), processInline(templateInput.getTemplateLocation(), model)))
                .collect(java.util.stream.Collectors.toList());
    }
}
