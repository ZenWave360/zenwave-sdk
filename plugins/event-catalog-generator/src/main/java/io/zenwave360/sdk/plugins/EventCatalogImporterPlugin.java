package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;

@DocumentedPlugin(
        title = "Event Catalog Importer",
        summary = "Imports an EventCatalog source tree into a ZenWave architecture manifest.",
        mainOptions = {"inputFolder", "outputFile"})
public class EventCatalogImporterPlugin extends Plugin {

    @DocumentedOption(description = "Path to the EventCatalog source tree.")
    public String inputFolder;

    @DocumentedOption(description = "Path to the generated zenwave-architecture.yml file.")
    public String outputFile;

    public EventCatalogImporterPlugin() {
        super();
        withChain(
                EventCatalogImporterGenerator.class,
                EventCatalogImportFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && outputFile != null) {
            var parent = new java.io.File(outputFile).getAbsoluteFile().getParent();
            if (parent != null) {
                withOption("targetFolder", parent);
            }
        }
        return super.processOptions();
    }
}
