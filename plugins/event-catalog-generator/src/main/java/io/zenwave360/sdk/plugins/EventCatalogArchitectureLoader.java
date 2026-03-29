package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code zenwave-architecture.yml} into {@code Map<String, Object>} and places it
 * in the context under the key {@code "architecture"}.
 *
 * <p>Processing steps:
 * <ol>
 *   <li>Parse the YAML file.</li>
 *   <li>Resolve {@code config.properties.root} relative to the input file's directory.</li>
 *   <li>Replace {@code {{root}}} in every {@code repository} field recursively.</li>
 *   <li>Resolve consumer {@code $ref} values of the form {@code #/services/<id>}
 *       by replacing the ref map with the referenced service id string.</li>
 * </ol>
 */
public class EventCatalogArchitectureLoader implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Path to the zenwave-architecture.yml master file.")
    public String inputFile;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        File file = new File(inputFile);
        if (!file.exists()) {
            throw new RuntimeException("zenwave-architecture.yml not found: " + file.getAbsolutePath());
        }

        Map<String, Object> architecture;
        try {
            architecture = yamlMapper.readValue(file, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse " + inputFile + ": " + e.getMessage(), e);
        }

        String rawRoot = getRoot(architecture);
        Path resolvedRoot = file.getParentFile().toPath().resolve(rawRoot).normalize();
        replaceRoot(architecture, "{{root}}", resolvedRoot.toString());

        resolveConsumerRefs(architecture);

        contextModel.put("architecture", architecture);
        return contextModel;
    }

    // -------------------------------------------------------------------------
    // Root resolution
    // -------------------------------------------------------------------------

    private String getRoot(Map<String, Object> architecture) {
        Object config = architecture.get("config");
        if (config instanceof Map<?,?> configMap) {
            Object props = configMap.get("properties");
            if (props instanceof Map<?,?> propsMap) {
                Object root = propsMap.get("root");
                if (root != null) {
                    return root.toString().trim();
                }
            }
        }
        return ".";
    }

    @SuppressWarnings("unchecked")
    private void replaceRoot(Object node, String placeholder, String root) {
        if (node instanceof Map<?,?> map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String s && s.contains(placeholder)) {
                    entry.setValue(s.replace(placeholder, root));
                } else {
                    replaceRoot(value, placeholder, root);
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                replaceRoot(item, placeholder, root);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Consumer $ref resolution
    // -------------------------------------------------------------------------

    /**
     * Replaces each consumer entry of the form {@code {$ref: "#/services/<id>"}}
     * with just the service id string. This makes consumer ids easy to use downstream
     * without needing to parse the $ref syntax again.
     */
    @SuppressWarnings("unchecked")
    private void resolveConsumerRefs(Map<String, Object> architecture) {
        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());
        for (Object serviceObj : services.values()) {
            if (!(serviceObj instanceof Map<?,?> service)) continue;
            Object consumersObj = ((Map<String, Object>) service).get("consumers");
            if (!(consumersObj instanceof List<?> consumers)) continue;

            List<Object> resolvedConsumers = (List<Object>) consumers;
            for (int i = 0; i < resolvedConsumers.size(); i++) {
                Object consumer = resolvedConsumers.get(i);
                if (consumer instanceof Map<?,?> refMap) {
                    Object ref = ((Map<?,?>) refMap).get("$ref");
                    if (ref instanceof String refStr && refStr.startsWith("#/services/")) {
                        String serviceId = refStr.substring("#/services/".length());
                        if (services.containsKey(serviceId)) {
                            resolvedConsumers.set(i, serviceId);
                        } else {
                            log.warn("Unresolvable consumer $ref: {} in service", refStr);
                        }
                    }
                }
            }
        }
    }
}
