package io.zenwave360.sdk.plugins;

import java.io.IOException;
import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

import static io.zenwave360.sdk.plugins.EventCatalogEntitiesGenerator.OutputFormat.aggregate;
import static io.zenwave360.sdk.plugins.EventCatalogEntitiesGenerator.OutputFormat.task_list;

@DocumentedPlugin(summary = "Generates Event Catalog Entities from Zdl Models",
        hiddenOptions = {"apiFile", "apiFiles", "layout", "targetFolder", "basePackage"})
public class EventCatalogEntitiesPlugin extends Plugin {

    public EventCatalogEntitiesPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, EventCatalogEntitiesGenerator.class, TemplateFileWriter.class);
    }


    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
