package io.zenwave360.sdk.plugins;

import io.zenwave360.jsonrefparser.resolver.RefFormat;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.WithProjectClassLoader;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads and processes one or more AsyncAPI spec files, placing the resulting models
 * into the context as a {@code List<Model>} under the key {@code "apis"}.
 *
 * <p>Replaces the previous fixed {@code DefaultYamlParser × 2 + AsyncApiProcessor × 2} chain
 * entries, supporting an arbitrary number of input specs without changing chain length.
 *
 * <p>Channel ownership is determined downstream by {@link AsyncAPIOpsIntentProcessor} via the
 * {@code x--external-channel} marker set here on channels resolved from cross-file {@code $ref}s.
 */
public class AsyncAPIOpsSpecLoader implements Processor, WithProjectClassLoader<AsyncAPIOpsSpecLoader> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ClassLoader projectClassLoader;

    @DocumentedOption(description = "AsyncAPI Specification File")
    public URI apiFile;

    @DocumentedOption(description = "AsyncAPI Specification Files")
    public List<URI> apiFiles = new ArrayList<>();

    @Override
    public AsyncAPIOpsSpecLoader withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        List<URI> files = effectiveApiFiles();

        if (files.isEmpty()) {
            log.warn("No apiFiles configured — no specs loaded.");
            contextModel.put("apis", List.of());
            return contextModel;
        }

        List<Model> models = new ArrayList<>();
        for (URI fileUri : files) {
            try {
                Model model = loadAndProcess(fileUri);
                models.add(model);
                log.debug("Loaded AsyncAPI spec: {}", fileUri);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load AsyncAPI spec: " + fileUri, e);
            }
        }

        contextModel.put("apis", models);
        return contextModel;
    }

    private Model loadAndProcess(URI fileUri) throws Exception {

        DefaultYamlParser parser = new DefaultYamlParser()
                .withApiFile(fileUri)
                .withProjectClassLoader(projectClassLoader);

        Map<String, Object> parsed = parser.parse();

        // Mark cross-file $ref channels BEFORE AsyncApiProcessor runs.
        markExternalChannels((Model) parsed.get("api"));

        AsyncApiProcessor processor = new AsyncApiProcessor();
        parsed = processor.process(parsed);

        return (Model) parsed.get("api");
    }

    /**
     * Marks channels resolved from cross-file $refs with {@code x--external-channel: true}.
     * Uses object identity (==) because Map.hashCode() on resolved objects may be circular.
     */
    private void markExternalChannels(Model model) {
        // Collect all objects resolved from cross-file (non-INTERNAL) refs
        List<Object> crossFileObjects = new ArrayList<>();
        model.getRefs().getOriginalRefsList().forEach(pair -> {
            if (pair.getKey().getRefFormat() != RefFormat.INTERNAL) {
                crossFileObjects.add(pair.getValue());
            }
        });

        if (crossFileObjects.isEmpty()) {
            return;
        }

        Map<String, Map> channels = JSONPath.get(model, "$.channels", Collections.emptyMap());
        for (Map channel : channels.values()) {
            boolean isCrossFile = crossFileObjects.stream().anyMatch(obj -> obj == channel);
            if (isCrossFile) {
                channel.put("x--external-channel", true);
            }
        }
    }

    private List<URI> effectiveApiFiles() {
        List<URI> result = new ArrayList<>(apiFiles);
        if (apiFile != null && !result.contains(apiFile)) {
            result.add(0, apiFile);
        }
        return result;
    }
}
