package io.zenwave360.generator.plugins;

import java.util.Map;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates a full AsyncAPI definitions for CRUD operations from JDL models", shortCode = "jdl-to-asyncapi")
public class JDLToAsyncAPIPlugin extends Plugin {

    public JDLToAsyncAPIPlugin() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLToAsyncAPIGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public Plugin withOptions(Map<String, Object> options) {
        if (!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(JDLParser.class, JDLProcessor.class, JDLToAsyncAPIGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
