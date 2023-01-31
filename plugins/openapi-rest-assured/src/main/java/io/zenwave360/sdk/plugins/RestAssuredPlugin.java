package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates REST-Assured tests based on OpenAPI specification.", shortCode = "rest-assured")
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
