package io.zenwave360.generator.formatters;

import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;

public interface Formatter {
    List<TemplateOutput> format(List<TemplateOutput> templateOutputList);
}
