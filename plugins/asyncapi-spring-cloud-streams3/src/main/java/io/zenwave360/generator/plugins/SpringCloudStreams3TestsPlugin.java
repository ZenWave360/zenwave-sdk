package io.zenwave360.generator.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

@DocumentedPlugin(value = "Generates tests for Spring Cloud Streams Consumers.", shortCode = "spring-cloud-streams3-tests")
public class SpringCloudStreams3TestsPlugin extends Plugin {

    private Logger log = LoggerFactory.getLogger(getClass());
    public SpringCloudStreams3TestsPlugin() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, SpringCloudStreams3TestsGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!hasOption("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
