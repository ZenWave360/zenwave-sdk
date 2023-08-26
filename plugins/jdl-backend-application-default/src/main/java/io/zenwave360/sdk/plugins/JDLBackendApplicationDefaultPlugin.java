package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

/**
 * This is the long description
 */
@DocumentedPlugin(value = "Generates a full backend application using a flexible hexagonal architecture", shortCode = "jdl-backend-application-default", description = "${javadoc}")
public class JDLBackendApplicationDefaultPlugin extends Plugin {

    public JDLBackendApplicationDefaultPlugin() {
        super();
        withChain(JDLParser.class, ZDLProcessor.class, JDLBackendApplicationDefaultGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
