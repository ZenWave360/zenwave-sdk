package io.zenwave360.sdk.writers;

import java.util.List;

import io.zenwave360.sdk.templating.TemplateOutput;

public interface TemplateWriter {

    public void write(List<TemplateOutput> templateOutputList);
}
