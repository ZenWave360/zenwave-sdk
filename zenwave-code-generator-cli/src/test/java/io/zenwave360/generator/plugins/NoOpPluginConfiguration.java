package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedOption(description = "'no operation' plugin description")
public class NoOpPluginConfiguration extends Configuration {

    public static final String CONFIG_ID = "nop";

    public NoOpPluginConfiguration() {
        super();
        withChain(NoOpPluginGenerator.class, TemplateStdoutWriter.class);
    }
}
