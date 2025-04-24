package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(summary = "Generates JDL model from OpenAPI schemas")
public class OpenAPIToJDLPlugin extends Plugin {

    public OpenAPIToJDLPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, OpenAPIToJDLGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        } else if(!getOptions().containsKey("targetFolder")) {
            withOption("targetFolder", ".");
        }
        return (T) this;
    }
}
