package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;

/** Generates multi-page Markdown documentation with Mermaid diagrams from ZDL, ZFL and API specifications. */
@DocumentedPlugin(
        summary = "Generates Markdown documentation with Mermaid diagrams from ZDL, ZFL, OpenAPI and AsyncAPI files",
        mainOptions = { "zdlFiles", "zdlFile", "zflFiles", "apiFiles", "apiFile", "targetFolder", "title", "version", "date",
                "check" },
        hiddenOptions = { "layout", "authentication", "specFile" })
public class DslDocsPlugin extends Plugin {

    public DslDocsPlugin() {
        withChain(DslDocsProcessor.class);
    }
}
