package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;

public class JDLEntitiesConfigurationPreset extends Configuration {

    public static final String PRESET_ID = "jdl-entities";

    public JDLEntitiesConfigurationPreset() {
        super();
        withChain(DefaultYamlParser.class, JDLProcessor.class, JDLEntitiesGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if(!options.containsKey("targetFolder")) {
            withChain(DefaultYamlParser.class, AsyncApiProcessor.class, JDLEntitiesGenerator.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
