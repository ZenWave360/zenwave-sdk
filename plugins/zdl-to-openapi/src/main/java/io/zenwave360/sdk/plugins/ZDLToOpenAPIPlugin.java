package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(summary = "Generates a draft OpenAPI definitions from your ZDL entities and services.", hiddenOptions = {"apiFile, apiFiles"})
public class ZDLToOpenAPIPlugin extends Plugin {

    public ZDLToOpenAPIPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, PathsProcessor.class, ZDLToOpenAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && !getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
