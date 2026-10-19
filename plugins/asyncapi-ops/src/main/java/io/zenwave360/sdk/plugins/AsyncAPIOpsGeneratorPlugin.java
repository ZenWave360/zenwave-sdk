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
            "apiOverlayFiles",
            "avroImports",
            "server",
            "parameterValues",
            "templates",
            "serviceAccountMode",
            "targetFolder",
        },
        hiddenOptions = {"layout", "apiFiles", "zdlFile", "zdlFiles", "style"})
public class AsyncAPIOpsGeneratorPlugin extends Plugin {

    @DocumentedOption(description = "Additional Avro schema files or folders available while bundling owned message schemas. Sibling .avsc files are discovered automatically for local and classpath schemas. Supports local files/folders, classpath resources, and https:// files.")
    public java.util.List<String> avroImports = java.util.List.of();

    @DocumentedOption(description = "Target server/environment name matching a key in asyncapi servers (e.g. dev, staging, production). Applies x-env-server-overrides/env-server-overrides from channel and error-topic bindings.")
    public String server;

    @DocumentedOption(description = "Selects which values a channel address parameter expands to. parameterValues.<name>=v1,v2 restricts every channel using that parameter name to a subset of its declared enum; parameterValues.<channelKey>.<name>=v1 overrides a single channel and is also the only form that can supply values for a parameter with no enum. Values are validated against each channel's own enum.")
    public java.util.Map<String, Object> parameterValues = new java.util.LinkedHashMap<>();

    @DocumentedOption(description = "How Confluent service accounts are resolved: existing looks them up by display name; managed provisions them.", values = {"existing", "managed"})
    public String serviceAccountMode = "existing";

    public AsyncAPIOpsGeneratorPlugin() {
        super();
        withChain(
                AsyncAPIOpsSpecLoader.class,
                AsyncAPIOpsIntentProcessor.class,
                AsyncAPIOpsGenerator.class,
                TemplateFileWriter.class);
    }

}
