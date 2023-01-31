package io.zenwave360.sdk.formatters;

import java.util.List;

import io.zenwave360.sdk.templating.TemplateOutput;

public interface Formatter {
    List<TemplateOutput> format(List<TemplateOutput> templateOutputList);
}
