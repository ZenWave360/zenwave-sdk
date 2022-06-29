package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;

public class OpenAPIToJDLConfigurationPreset extends Configuration {

    public static final String CONFIG_ID = "openapi-to-jdl";

    public OpenAPIToJDLConfigurationPreset() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, OpenAPIToJDLGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if(!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(DefaultYamlParser.class, OpenApiProcessor.class, OpenAPIToJDLGenerator.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
