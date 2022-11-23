package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

/**
 * This is the long description
 */
@DocumentedPlugin(value = "Generates a full backend application using a flexible hexagonal architecture", shortCode = "jdl-backend-application-default", description = "${javadoc}")
public class JDLBackendApplicationDefaultPlugin extends Plugin {

    public JDLBackendApplicationDefaultPlugin() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLBackendApplicationDefaultGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
