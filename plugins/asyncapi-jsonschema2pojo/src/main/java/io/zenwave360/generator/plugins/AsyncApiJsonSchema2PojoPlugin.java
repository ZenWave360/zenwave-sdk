package io.zenwave360.generator.plugins;

import java.util.Map;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;

@DocumentedPlugin(value = "Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files", shortCode = "jsonschema2pojo")
public class AsyncApiJsonSchema2PojoPlugin extends Plugin {

    public AsyncApiJsonSchema2PojoPlugin() {
        super();
        withChain(DefaultYamlParser.class, AsyncApiProcessor.class, AsyncApiJsonSchema2PojoGenerator.class);
    }

    @Override
    public Plugin withOptions(Map<String, Object> options) {
        return super.withOptions(options);
    }
}
