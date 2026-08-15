package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.BlockingManifestApiConsumptions;
import io.zenwave360.manifest.ApiConsumptionOptions;
import io.zenwave360.manifest.ManifestApiConsumptions;
import io.zenwave360.manifest.ManifestConsumptionRules;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.graph.ArchitectureGraphBuildOptions;
import io.zenwave360.manifest.graph.ArchitectureGraphResult;
import io.zenwave360.manifest.graph.BlockingArchitectureGraph;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code zenwave-architecture.yml} through {@code manifest-core}.
 */
public class EventCatalogArchitectureLoader implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BlockingZenWaveManifestLoader manifestRuntime = new BlockingZenWaveManifestLoader();

    @DocumentedOption(description = "URI of the zenwave-architecture.yml master file.")
    public URI inputFile;
    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        if (inputFile == null) {
            throw new IllegalArgumentException("inputFile is required");
        }

        ZenWaveManifest manifest;
        try {
            manifest = manifestRuntime.load(inputFile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load " + inputFile + ": " + e.getMessage(), e);
        }
        contextModel.put("manifest", manifest);
        contextModel.put("manifestRuntime", manifestRuntime);
        contextModel.put("eventCatalog", new EventCatalogModel(manifest));
        ManifestLoadOptions loadOptions = new ManifestLoadOptions()
                .withPreferredSource(preferredSource)
                .withFallback(allowFallback == null || allowFallback);
        ManifestApiConsumptions apiConsumptions = BlockingManifestApiConsumptions.build(
                manifest,
                manifestRuntime.getDelegate(),
                new ApiConsumptionOptions().withLoadOptions(loadOptions));
        contextModel.put("apiConsumptions", apiConsumptions);
        ArchitectureGraphResult architectureGraph = BlockingArchitectureGraph.build(
                manifest,
                manifestRuntime.getDelegate(),
                new ArchitectureGraphBuildOptions(
                        loadOptions,
                        Set.of("zfl", "zdl", "asyncapi", "openapi"),
                        false,
                        true,
                        ManifestConsumptionRules.getDEFAULT(),
                        false));
        contextModel.put("architectureGraph", architectureGraph);

        manifest.getDiagnostics().forEach(diagnostic ->
                log.warn("Manifest diagnostic [{}] at {}: {}",
                        diagnostic.getCode(),
                        diagnostic.getLocation(),
                        diagnostic.getMessage()));
        apiConsumptions.getDiagnostics().forEach(diagnostic ->
                log.warn("API consumption diagnostic [{}] at {}: {}",
                        diagnostic.getCode(),
                        diagnostic.getLocation(),
                        diagnostic.getMessage()));
        architectureGraph.getDiagnostics().forEach(diagnostic ->
                log.warn("Architecture graph diagnostic [{}] at {}: {}",
                        diagnostic.getCode(),
                        diagnostic.getSource() != null ? diagnostic.getSource().getUri() : null,
                        diagnostic.getMessage()));

        return contextModel;
    }
}
