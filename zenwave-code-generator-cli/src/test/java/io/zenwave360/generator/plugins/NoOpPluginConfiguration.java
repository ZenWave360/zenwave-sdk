package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(description = "'no operation' plugin description")
public class NoOpPluginConfiguration extends Configuration {

    public static final String CONFIG_ID = "nop";

    public NoOpPluginConfiguration() {
        super();
        withChain(NoOpPluginGenerator.class, TemplateStdoutWriter.class);
    }
}
