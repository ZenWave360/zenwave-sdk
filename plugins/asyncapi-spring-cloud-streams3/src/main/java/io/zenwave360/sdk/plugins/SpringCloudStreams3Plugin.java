package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(value = "Generates strongly typed SpringCloudStreams3 producer/consumer classes for AsyncAPI", shortCode = "spring-cloud-streams3")
public class SpringCloudStreams3Plugin extends Plugin {

    public SpringCloudStreams3Plugin() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, SpringCloudStreams3Generator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {

        return super.processOptions();
    }
}
