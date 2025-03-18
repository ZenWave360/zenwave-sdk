package io.zenwave360.sdk.plugins;

import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(
        title = "JDL To AsyncAPI Generator",
        summary = "Generates a full AsyncAPI definitions for CRUD operations from JDL models",
        mainOptions = { "zdlFile", "zdlFiles", "basePackage", "avroPackage", "schemaFormat", "includeEvents", "includeCommands", "idType", "idTypeFormat", "payloadStyle", "asyncapiVersion", "targetFile" },
        hiddenOptions = { "apiFile", "apiFiles", "layout", "formatter", "skipFormatting", "haltOnFailFormatting" },
        description = """
                - One channel for each entity update events
                - Messages and payloads for each entity:
                  - Supported Schema Formats: AVRO and AsyncAPI schema
                  - Supported Payload Styles: Entity and State Transfer (for Create/Update/Delete events)

                JDL Example:

                ```jdl
                @aggregate
                entity Customer {
                  username String required minlength(3) maxlength(250)
                  password String required minlength(3) maxlength(250)
                  email String required minlength(3) maxlength(250)
                  firstName String required minlength(3) maxlength(250)
                  lastName String required minlength(3) maxlength(250)
                }
                ```

                Then run:

                ```shell
                jbang zw -p io.zenwave360.sdk.plugins.JDLToAsyncAPIPlugin \\
                    includeCommands=true \\
                    specFile=src/main/resources/model/orders-model.jdl \\
                    idType=integer \\
                    idTypeFormat=int64 \\
                    annotations=aggregate \\
                    payloadStyle=event \\
                    targetFile=src/main/resources/model/asyncapi.yml
                ```
                """
)
public class JDLToAsyncAPIPlugin extends Plugin {

    public JDLToAsyncAPIPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, JDLToAsyncAPIGenerator.class, TemplateFileWriter.class);
    }

    @Override
    public Plugin withOptions(Map<String, Object> options) {
        if (!options.containsKey("targetFolder") && !options.containsKey("targetFile")) {
            withChain(ZDLParser.class, ZDLProcessor.class, JDLToAsyncAPIGenerator.class, JavaFormatter.class, TemplateStdoutWriter.class);
        }
        if(options.containsKey("specFile")) {
            withOption("zdlFile", options.get("specFile"));
        }
        return super.withOptions(options);
    }
}
