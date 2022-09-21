package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;

@DocumentedPlugin(value = "Creates a new standalone maven module cloning an existing plugin", shortCode = "fork-plugin", description = "${javadoc}")
public class ForkPluginConfiguration extends Configuration {

    public ForkPluginConfiguration() {
        withChain(ForkPluginGenerator.class);
    }
}
