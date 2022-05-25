package io.zenwave360.generator.writers;

import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;

public interface TemplateWriter {

    public void write(List<TemplateOutput> templateOutputList);
}
