package io.zenwave360.generator.plugins;

import java.util.Map;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates a full OpenAPI definitions for CRUD operations from JDL models", shortCode = "jdl-to-openapi")
public class JDLToOpenAPIConfiguration extends Configuration {

    public JDLToOpenAPIConfiguration() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLToOpenAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Configuration> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && !getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
