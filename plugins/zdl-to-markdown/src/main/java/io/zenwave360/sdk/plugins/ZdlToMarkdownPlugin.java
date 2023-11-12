package io.zenwave360.sdk.plugins;

import java.io.IOException;
import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates Markdown glossary from Zdl Models", shortCode = "zdl-to-markdown",
        hiddenOptions = {"targetFolder", "basePackage"})
public class ZdlToMarkdownPlugin extends Plugin {

    public ZdlToMarkdownPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, ZdlToMarkdownGenerator.class, TemplateFileWriter.class);
    }

    public static String generateMarkdown(String zdlContent) throws IOException {
        Map<String, Object> model = new ZDLParser().withContent(zdlContent).parse();
        model = new ZDLProcessor().process(model);
        var out = new ZdlToMarkdownGenerator().generate(model);
        return out.get(0).getContent();
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
