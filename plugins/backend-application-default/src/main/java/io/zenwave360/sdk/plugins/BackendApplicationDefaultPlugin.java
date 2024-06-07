package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.formatters.KotlinFormatter;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(summary = "Generates a full backend application using the provided 'layout' property",
        mainOptions = { "layout", "zdlFile", "zdlFiles", "basePackage", "persistence", "databaseType", "style", "useLombok", "addRelationshipsById", "idJavaType", "includeEmitEventsImplementation" },
        hiddenOptions = { "apiFile", "apiFiles" },
        description = """
Sample configuration:

```zdl
config {
    basePackage "com.example"
    persistence jpa
    databaseType postgresql
    layout CleanHexagonalProjectLayout

    // The IDE will automatically use the active .zdl file
    // Alternatively, specify the path here to maintain separation between models and plugins
    zdlFile "models/example.zdl"

    plugins {
        BackendApplicationDefaultPlugin {
            useLombok true
            --force // overwrite all files
        }
    }
}
```

Visit https://www.zenwave360.io/docs/zenwave-sdk/backend-application for complete documentation.

                """)
public class BackendApplicationDefaultPlugin extends Plugin {

    public BackendApplicationDefaultPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, BackendApplicationDefaultGenerator.class, JavaFormatter.class, KotlinFormatter.class, TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        return (T) this;
    }

}
