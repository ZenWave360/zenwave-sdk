package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates a full OpenAPI definitions for CRUD operations from JDL models", shortCode = "jdl-to-openapi")
public class JDLToOpenAPIPlugin extends Plugin {

    public JDLToOpenAPIPlugin() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLToOpenAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && !getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
