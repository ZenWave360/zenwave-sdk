package io.zenwave360.sdk.formatters;

import java.util.List;

import io.zenwave360.sdk.templating.TemplateOutput;

public interface Formatter {

    enum Formatters {
        palantir, spring, google
    }
    List<TemplateOutput> format(List<TemplateOutput> templateOutputList);
}
