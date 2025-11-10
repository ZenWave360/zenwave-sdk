package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;

@DocumentedPlugin(summary = "Generates Java classes from Avro schemas using Avro Compiler.",
        mainOptions = {
                "avroFiles",
                "avroCompilerProperties",
                "avroCompilerProperties.sourceDirectory",
                "avroCompilerProperties.imports",
                "avroCompilerProperties.includes",
                "avroCompilerProperties.excludes",
                "sourceFolder",
                "targetFolder",
        },
        hiddenOptions = {
                "layout","zdlFile","zdlFiles","apiFile", "apiFiles"
        },
        description = """
                Generates Java classes from Avro schemas using your provided Avro Compiler version. Compatible with Avro versions from 1.8.0 to 1.12.0.
                """
)
public class AvroSchemaGeneratorPlugin extends Plugin {

    public AvroSchemaGeneratorPlugin() {
        super();
        withChain(AvroSchemaLoader.class, AvroSchemaGenerator.class);
    }
}



