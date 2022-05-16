package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.writers.DefaultTemplateWriter;

public class SpringCloudStream3ConfigurationPreset extends Configuration {

    public static final String PRESET_ID = "spring-cloud-streams3";

    public SpringCloudStream3ConfigurationPreset() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, SpringCloudStreams3Generator.class, DefaultTemplateWriter.class);
    }
}
