package io.zenwave360.generator.writers;

import java.util.List;

import io.zenwave360.generator.templating.TemplateOutput;

public interface TemplateWriter {

    public void write(List<TemplateOutput> templateOutputList);
}
