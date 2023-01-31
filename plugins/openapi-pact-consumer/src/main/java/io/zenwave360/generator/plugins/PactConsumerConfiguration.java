package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Configuration;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

/**
 * jbang zw -p io.zenwave360.sdk.plugins.PactConsumerConfiguration \
 *     specFile=src/main/resources/model/openapi.yml \
 *     targetFolder=src/test/java \
 *     testsPackage=io.zenwave360.example.tests.contract \
 *     openApiApiPackage=io.zenwave360.example.adapters.web \
 *     openApiModelPackage=io.zenwave360.example.adapters.web.model \
 *     groupBy=operation
 */
@DocumentedPlugin(value = "Generates Consumer Contracts for Pact.", shortCode = "pact-consumer")
public class PactConsumerConfiguration extends Configuration {
    public PactConsumerConfiguration() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, PactConsumerGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Configuration> T processOptions() {
        System.out.println("options: " + getOptions());
        if (!getOptions().containsKey("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
            withOption(PactConsumerGenerator.class.getName() + ".groupBy", PactConsumerGenerator.GroupByType.partial.toString());
        }
        return (T) this;
    }
}
