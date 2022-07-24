package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;

import java.util.Map;


@DocumentedPlugin("Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files")
public class AsyncApiJsonSchema2PojoConfiguration extends Configuration {

    public static final String CONFIG_ID = "spring-cloud-streams3";

    public AsyncApiJsonSchema2PojoConfiguration() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, AsyncApiJsonSchema2PojoGenerator.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        return super.withOptions(options);
    }
}
