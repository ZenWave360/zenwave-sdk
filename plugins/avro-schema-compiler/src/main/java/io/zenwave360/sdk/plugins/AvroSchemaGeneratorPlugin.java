package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;

import java.util.Map;

@DocumentedPlugin(summary = "",
        mainOptions = {},
        hiddenOptions = {},
        description = """

                """
)
public class AvroSchemaGeneratorPlugin extends Plugin {

    public AvroSchemaGeneratorPlugin() {
        super();
        withChain(AvroSchemaLoader.class, AvroSchemaGenerator.class);
    }
}
