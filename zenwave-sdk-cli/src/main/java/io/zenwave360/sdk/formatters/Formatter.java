package io.zenwave360.sdk.formatters;

import java.util.List;

import io.zenwave360.sdk.templating.TemplateOutput;

public interface Formatter {

    enum Formatters {
        google, palantir, spring, eclipse
    }
    List<TemplateOutput> format(List<TemplateOutput> templateOutputList);
}
