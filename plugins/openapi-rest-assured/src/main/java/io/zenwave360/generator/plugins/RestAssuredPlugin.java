package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates REST-Assured tests from OpenAPI defined endpoints.", shortCode = "rest-assured")
public class RestAssuredPlugin extends Plugin {
    public RestAssuredPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, RestAssuredGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
            withOption(RestAssuredGenerator.class.getName() + ".groupBy", RestAssuredGenerator.GroupByType.partial.toString());
        }
        return (T) this;
    }
}
