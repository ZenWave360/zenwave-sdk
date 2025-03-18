package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(summary = "Generates a full backend application using a multiple maven modules", description = "")
public class BackendApplicationMultiModulePlugin extends Plugin {

    public BackendApplicationMultiModulePlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, BackendApplicationMultiModuleGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        return (T) this;
    }

}
