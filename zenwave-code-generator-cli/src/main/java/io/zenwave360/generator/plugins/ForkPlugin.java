package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;

@DocumentedPlugin(value = "Creates a new standalone maven module cloning an existing plugin", shortCode = "fork-plugin", description = "${javadoc}")
public class ForkPlugin extends Plugin {

    public ForkPlugin() {
        withChain(ForkPluginGenerator.class);
    }
}
