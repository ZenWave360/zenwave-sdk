package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

/**
 * jbang zw -p io.zenwave360.generator.plugins.PactConsumerConfiguration \
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
