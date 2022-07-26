package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedOption;
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
public class JDLBackendApplicationDefaultConfiguration extends Configuration {

    public JDLBackendApplicationDefaultConfiguration() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLBackendApplicationDefaultGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Configuration> T processOptions() {
        if(!getOptions().containsKey("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
