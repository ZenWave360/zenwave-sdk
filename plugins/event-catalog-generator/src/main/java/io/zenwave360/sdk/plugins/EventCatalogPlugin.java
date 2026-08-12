package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;

@DocumentedPlugin(
        title = "Event Catalog Generator",
        summary = "Generates an EventCatalog source tree from a zenwave-architecture.yml master file.",
        mainOptions = {"inputFile", "outputFolder", "docsTemplate"},
        hiddenOptions = {"layout", "apiFile", "apiFiles", "zdlFile", "zdlFiles", "style", "targetFolder",
                "preferredSource", "allowFallback", "linkSource"})
public class EventCatalogPlugin extends Plugin {

    // Chain:
    // 0 = EventCatalogArchitectureLoader  — loads the typed manifest and EventCatalog enrichment model
    // 1 = EventCatalogAsyncApiProcessor   — enriches services with events/commands/sends/receives
    // 2 = EventCatalogOpenApiProcessor    — enriches services with queries
    // 3 = EventCatalogZdlProcessor        — enriches services with entities
    // 4 = EventCatalogConsumerProcessor   — resolves declared consumer artifacts and operations
    // 5 = EventCatalogGenerator           — generates MDX pages
    // 6 = EventCatalogFileWriter          — cleans output, versions service pages, writes files

    @DocumentedOption(description = "Path to the zenwave-architecture.yml master file.")
    public String inputFile;

    @DocumentedOption(description = "Output folder for the EventCatalog source tree.")
    public String outputFolder;

    @DocumentedOption(description = "Custom Handlebars template for docs body rendering. "
            + "Receives a map of { key → file content }. "
            + "Defaults to the built-in template that concatenates summary, content, and changelog.")
    public String docsTemplate;

    @DocumentedOption(description = "Preferred active manifest source for build-time loading, such as workspace or git.")
    public String preferredSource;

    @DocumentedOption(description = "Allow fallback across configured sources after the preferred source.")
    public Boolean allowFallback;

    @DocumentedOption(description = "Preferred active manifest source for generated frontmatter links, such as git.")
    public String linkSource;

    public EventCatalogPlugin() {
        super();
        withChain(
                EventCatalogArchitectureLoader.class,   // 0
                EventCatalogAsyncApiProcessor.class,    // 1
                EventCatalogOpenApiProcessor.class,     // 2
                EventCatalogZdlProcessor.class,         // 3
                EventCatalogConsumerProcessor.class,    // 4
                EventCatalogGenerator.class,            // 5
                EventCatalogFileWriter.class);          // 6
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (inputFile != null) {
            // Match Plugin.withApiFile: keep Windows paths URI-bindable for the loader processor.
            withOption("inputFile", inputFile.replace('\\', '/'));
        }
        if (!getOptions().containsKey("targetFolder") && getOptions().containsKey("outputFolder")) {
            withOption("targetFolder", getOptions().get("outputFolder"));
        }
        return super.processOptions();
    }
}
