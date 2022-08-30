package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;

import java.util.Map;


@DocumentedPlugin(value = "Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files", shortCode = "jsonschema2pojo")
public class AsyncApiJsonSchema2PojoConfiguration extends Configuration {

    public AsyncApiJsonSchema2PojoConfiguration() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, OriginalRefProcessor.class, AsyncApiJsonSchema2PojoGenerator.class);
    }

    @Override
    public Configuration withOptions(Map<String, Object> options) {
        return super.withOptions(options);
    }
}
