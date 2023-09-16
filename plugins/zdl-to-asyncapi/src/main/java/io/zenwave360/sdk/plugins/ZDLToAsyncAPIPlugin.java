package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates a full OpenAPI definitions for CRUD operations from JDL models", shortCode = "zdl-to-asyncapi")
public class ZDLToAsyncAPIPlugin extends Plugin {

    public ZDLToAsyncAPIPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, ZDLToAsyncAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
