package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;

public class SpringWebTestsClientConfigurationPreset extends Configuration {

    public static final String PRESET_ID = "spring-webtestclient";

    public SpringWebTestsClientConfigurationPreset() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, SpringWebTestClientGenerator.class, JavaFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        if(!options.containsKey("targetFolder")) {
            withChain(DefaultYamlParser.class, OpenApiProcessor.class, SpringWebTestClientGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
            options.put(SpringWebTestClientGenerator.class.getName() + ".groupBy", SpringWebTestClientGenerator.GroupByType.PARTIAL.toString());
        }
        return super.withOptions(options);
    }
}
