package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

@DocumentedPlugin(summary = "Generates an AsyncAPI client file for ZDL service methods consuming external AsyncAPI channels.")
public class ZDLToAsyncAPIClientPlugin extends Plugin {

    public ZDLToAsyncAPIClientPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, ZDLToAsyncAPIClientGenerator.class,
                AsyncAPIOverlayProcessor.class, TemplateFileWriter.class);
    }
}
