package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Parses ZDL domain model specs declared in each service entry and augments the
 * service map with extracted entities and aggregates.
 *
 * <p>Only {@code type: zdl} specs are processed. Entities annotated with
 * {@code @aggregate} are marked as aggregate roots.
 *
 * <p>Augmented keys written to each service map (prefixed with {@code _}):
 * <ul>
 *   <li>{@code _entities} — List&lt;Map&gt; one entry per entity/aggregate</li>
 * </ul>
 */
public class EventCatalogZdlProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        if (architecture == null) return contextModel;

        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) entry.getValue();
            String serviceId = str(service, "id", entry.getKey());
            processZdlSpecs(service, serviceId);
        }

        return contextModel;
    }

    @SuppressWarnings("unchecked")
    private void processZdlSpecs(Map<String, Object> service, String serviceId) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"zdl".equals(spec.get("type"))) continue;

            String specPath = repository + File.separator + spec.get("path");
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("ZDL spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> zdlModel = parseSpec(specFile);
            if (zdlModel == null) continue;

            String version = str(service, "_version", str(service, "version", "0.0.1"));

            // Collect aggregate names for aggregate-root detection
            Map<String, Object> aggregates = JSONPath.get(zdlModel, "$.aggregates", Map.of());
            Set<String> aggregateRootNames = new LinkedHashSet<>();
            for (Map.Entry<String, Object> aggEntry : aggregates.entrySet()) {
                Map<String, Object> agg = (Map<String, Object>) aggEntry.getValue();
                String rootName = str(agg, "aggregateRoot", null);
                if (rootName != null) aggregateRootNames.add(rootName);
            }

            Map<String, Object> entities = JSONPath.get(zdlModel, "$.entities", Map.of());
            for (Map.Entry<String, Object> entityEntry : entities.entrySet()) {
                Map<String, Object> entity = (Map<String, Object>) entityEntry.getValue();
                String entityName = str(entity, "name", entityEntry.getKey());

                boolean isAggregate = aggregateRootNames.contains(entityName)
                        || Boolean.TRUE.equals(JSONPath.get(entity, "$.options.aggregate"));

                String entityId = serviceId + "." + toKebabCase(entityName);

                Map<String, Object> artifact = new LinkedHashMap<>();
                artifact.put("id", entityId);
                artifact.put("name", entityName);
                artifact.put("version", version);
                artifact.put("summary", str(entity, "javadoc", ""));
                if (isAggregate) artifact.put("aggregateRoot", true);

                addToList(service, "_entities", artifact);
            }
        }
    }

    private Map<String, Object> parseSpec(File specFile) {
        String tempKey = "_ec_zdl_" + System.nanoTime();
        try {
            var parsed = new ZDLParser()
                    .withZdlFile(specFile.getAbsolutePath())
                    .withTargetProperty(tempKey)
                    .parse();

            return (Map<String, Object>) parsed.get(tempKey);
        } catch (Exception e) {
            log.warn("Failed to parse ZDL spec {}: {} ({})", specFile.getAbsolutePath(), e.getMessage(), e.getClass().getSimpleName());
            return null;
        }
    }

    /** Converts CamelCase to kebab-case for use in EventCatalog folder names. */
    private String toKebabCase(String name) {
        if (name == null || name.isBlank()) return name;
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Map<String, Object> map, String key, T value) {
        List<T> list = (List<T>) map.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
    }

    private String str(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
