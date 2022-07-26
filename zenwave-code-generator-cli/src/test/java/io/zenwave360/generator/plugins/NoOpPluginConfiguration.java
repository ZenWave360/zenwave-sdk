package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.generators.AbstractOpenAPIGenerator;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "'no operation' plugin description", shortCode = "no-op")
public class NoOpPluginConfiguration extends Configuration {

    @DocumentedOption(description = "Operation Type")
    public AbstractOpenAPIGenerator.OperationType operationType = AbstractOpenAPIGenerator.OperationType.GET;

    public NoOpPluginConfiguration() {
        super();
        withChain(NoOpGenerator.class, TemplateStdoutWriter.class);
    }
}
