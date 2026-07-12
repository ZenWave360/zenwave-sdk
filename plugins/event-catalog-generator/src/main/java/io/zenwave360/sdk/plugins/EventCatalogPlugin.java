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
    // 0 = EventCatalogArchitectureLoader  — loads zenwave-architecture.yml → "architecture"
    // 1 = EventCatalogAsyncApiProcessor   — enriches services with events/commands/sends/receives
    // 2 = EventCatalogOpenApiProcessor    — enriches services with queries
    // 3 = EventCatalogZdlProcessor        — enriches services with entities
    // 4 = EventCatalogGenerator           — generates MDX pages
    // 5 = EventCatalogFileWriter          — cleans output, versions service pages, writes files

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
                EventCatalogGenerator.class,            // 4
                EventCatalogFileWriter.class);          // 5
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFolder") && getOptions().containsKey("outputFolder")) {
            withOption("targetFolder", getOptions().get("outputFolder"));
        }
        return super.processOptions();
    }
}
