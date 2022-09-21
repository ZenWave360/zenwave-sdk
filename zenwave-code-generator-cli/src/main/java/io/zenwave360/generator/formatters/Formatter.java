package io.zenwave360.generator.formatters;

import java.util.List;

import io.zenwave360.generator.templating.TemplateOutput;

public interface Formatter {
    List<TemplateOutput> format(List<TemplateOutput> templateOutputList);
}
