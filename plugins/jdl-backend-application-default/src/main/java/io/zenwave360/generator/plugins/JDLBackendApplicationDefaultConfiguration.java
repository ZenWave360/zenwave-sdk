package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;

public class JDLBackendApplicationDefaultConfiguration extends Configuration {

    public static final String CONFIG_ID = "jdl-backend-application-default";

    public JDLBackendApplicationDefaultConfiguration() {
        super();
        withChain(JDLParser.class, JDLProcessor.class, JDLBackendApplicationDefaultGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if(!options.containsKey("targetFolder")) {
            withChain(JDLParser.class, JDLProcessor.class, JDLBackendApplicationDefaultGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
