package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates Markdown glossary from Zdl Models", shortCode = "zdl-to-markdown",
        hiddenOptions = {"targetFolder", "basePackage"})
public class ZdlToMarkdownPlugin extends Plugin {

    public ZdlToMarkdownPlugin() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, ZdlToMarkdownGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && !getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
