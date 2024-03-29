package io.zenwave360.sdk.plugins;

import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;

@DocumentedPlugin(summary = "Generate Plain Old Java Objects from OpenAPI/AsyncAPI schemas or full JSON-Schema files.",
        mainOptions = {"apiFile", "apiFiles", "targetFolder", "modelPackage", "generatedAnnotationClass", "jsonschema2pojo"},
        hiddenOptions = {"layout", "zdlFile", "zdlFiles", "apiPackage", "producerApiPackage", "consumerApiPackage", "role", "runtimeHeadersProperty", "sourceFolder"},
        description = """
                Command line usage example:

                ```shell
                jbang zw -p io.zenwave360.sdk.plugins.AsyncApiJsonSchema2PojoPlugin \\
                    apiFile=src/main/resources/model/asyncapi.yml \\
                    modelPackage=io.zenwave360.example.core.domain.events \\
                    jsonschema2pojo.includeTypeInfo=true \\
                    targetFolder=.
                ```
                """
)
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
