package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(
        title = "AsyncAPI to Terraform Generator",
        summary = "Generates Terraform HCL (topics, schemas, ACLs) from AsyncAPI specs to prevent API drift.",
        mainOptions = {
            "apiFile",
            "apiFiles",
            "avroImports",
            "server",
            "templates",
            "targetFolder",
        },
        hiddenOptions = {"layout", "apiFiles", "zdlFile", "zdlFiles", "style"})
public class AsyncAPIOpsGeneratorPlugin extends Plugin {

    @DocumentedOption(description = "Avro schema files or folders available while bundling owned message schemas. Supports local files/folders, classpath resources, and https:// files.")
    public java.util.List<String> avroImports = java.util.List.of();

    @DocumentedOption(description = "Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Applies x-env-server-overrides from channel bindings.")
    public String server;

    public AsyncAPIOpsGeneratorPlugin() {
        super();
        withChain(
                AsyncAPIOpsSpecLoader.class,
                AsyncAPIOpsIntentProcessor.class,
                AsyncAPIOpsGenerator.class,
                TemplateFileWriter.class);
    }

}
