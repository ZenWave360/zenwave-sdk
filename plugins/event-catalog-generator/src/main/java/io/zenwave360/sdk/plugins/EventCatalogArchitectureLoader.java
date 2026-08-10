package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Loads {@code zenwave-architecture.yml} through {@code manifest-core}.
 */
public class EventCatalogArchitectureLoader implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BlockingZenWaveManifestLoader manifestRuntime = new BlockingZenWaveManifestLoader();

    @DocumentedOption(description = "URI of the zenwave-architecture.yml master file.")
    public URI inputFile;

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

        manifest.getDiagnostics().forEach(diagnostic ->
                log.warn("Manifest diagnostic [{}] at {}: {}",
                        diagnostic.getCode(),
                        diagnostic.getLocation(),
                        diagnostic.getMessage()));

        return contextModel;
    }
}
