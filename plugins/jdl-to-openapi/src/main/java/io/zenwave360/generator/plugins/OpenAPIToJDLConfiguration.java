package io.zenwave360.generator.plugins;

import java.util.Map;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates JDL model from OpenAPI schemas", shortCode = "openapi-to-jdl")
public class OpenAPIToJDLConfiguration extends Configuration {

    public OpenAPIToJDLConfiguration() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, OpenAPIToJDLGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if (!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(DefaultYamlParser.class, OpenApiProcessor.class, OpenAPIToJDLGenerator.class, TemplateStdoutWriter.class);
        }
        return super.withOptions(options);
    }
}
