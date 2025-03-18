package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.generators.AbstractOpenAPIGenerator;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "'no operation' plugin description")
public class NoOpPluginConfiguration extends Plugin {

    @DocumentedOption(description = "Operation Type")
    public AbstractOpenAPIGenerator.OperationType operationType = AbstractOpenAPIGenerator.OperationType.GET;

    public NoOpPluginConfiguration() {
        super();
        withChain(NoOpGenerator.class, TemplateStdoutWriter.class);
    }
}
