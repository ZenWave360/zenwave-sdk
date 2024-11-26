package io.zenwave360.sdk.plugins;

import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates a full AsyncAPI definitions for CRUD operations from JDL models", shortCode = "jdl-to-asyncapi")
public class JDLToAsyncAPIPlugin extends Plugin {

    public JDLToAsyncAPIPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, JDLToAsyncAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public Plugin withOptions(Map<String, Object> options) {
        if (!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(ZDLParser.class, ZDLProcessor.class, JDLToAsyncAPIGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        if(options.containsKey("specFile")) {
            withOption("zdlFile", options.get("specFile"));
        }
        return super.withOptions(options);
    }
}
