package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

@DocumentedPlugin(summary = "Generates Event Catalog Entities from Zdl Models",
        hiddenOptions = {"apiFile", "apiFiles", "layout", "targetFolder", "basePackage"})
public class EventCatalogPlugin extends Plugin {

    public EventCatalogPlugin() {
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
