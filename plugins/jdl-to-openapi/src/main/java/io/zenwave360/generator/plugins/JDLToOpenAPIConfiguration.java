package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;

@DocumentedPlugin("Generates a full OpenAPI definitions for CRUD operations from JDL models")
public class JDLToOpenAPIConfiguration extends Configuration {

    public static final String CONFIG_ID = "jdl-to-openapi";

    public JDLToOpenAPIConfiguration() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLToOpenAPIGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if(!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(JDLParser.class, JDLProcessor.class, JDLToOpenAPIGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
