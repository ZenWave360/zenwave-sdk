package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Parses OpenAPI specs declared in each service entry and augments the service map
 * with extracted queries and specifications.
 *
 * <p>Only {@code type: openapi} specs are processed. Each {@code GET} operation
 * becomes a query artifact. The spec file path is added to {@code _specifications}.
 *
 * <p>Augmented keys written to each service map (prefixed with {@code _}):
 * <ul>
 *   <li>{@code _version} — String from {@code info.version} (only when not already set by AsyncAPI)</li>
 *   <li>{@code _queries} — List&lt;Map&gt; one entry per GET operation</li>
 *   <li>{@code _specifications} — List&lt;String&gt; absolute paths to openapi spec files</li>
 * </ul>
 */
public class EventCatalogOpenApiProcessor implements Processor {

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
            processOpenApiSpecs(service, serviceId);
        }

        return contextModel;
    }

    @SuppressWarnings("unchecked")
    private void processOpenApiSpecs(Map<String, Object> service, String serviceId) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"openapi".equals(spec.get("type"))) continue;

            String specPath = repository + File.separator + spec.get("path");
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("OpenAPI spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
            if (model == null) continue;

            // Version from info.version — only when not already set by AsyncAPI
            String version = JSONPath.get(model, "$.info.version");
            if (version != null && service.get("_version") == null) {
                service.put("_version", version);
            }

            addToList(service, "_specifications", specFile.getAbsolutePath());

            // Extract GET operations as queries
            List<Map<String, Object>> operations = JSONPath.get(model, "$.paths[*][*][?(@.operationId)]", List.of());
            for (Map<String, Object> operation : operations) {
                String httpVerb = str(operation, "x--httpVerb", "");
                if (!"get".equalsIgnoreCase(httpVerb)) continue;

                String operationId = str(operation, "operationId", null);
                if (operationId == null) continue;

                String queryId = serviceId + "." + operationId;
                String name = str(operation, "summary", operationId);
                String schemaPath = resolveSchemaPath(specFile, operation);

                Map<String, Object> query = new LinkedHashMap<>();
                query.put("id", queryId);
                query.put("name", name);
                query.put("version", version != null ? version : str(service, "version", "0.0.1"));
                if (schemaPath != null) query.put("schemaPath", schemaPath);

                addToList(service, "_queries", query);
            }
        }
    }

    private Map<String, Object> parseSpec(File specFile) {
        String tempKey = "_ec_openapi_" + System.nanoTime();
        try {
            var parsed = new DefaultYamlParser()
                    .withApiFile(specFile)
                    .withTargetProperty(tempKey)
                    .parse();

            var processor = new OpenApiProcessor();
            processor.targetProperty = tempKey;
            var processed = processor.process(parsed);

            return (Map<String, Object>) processed.get(tempKey);
        } catch (Exception e) {
            log.warn("Failed to parse OpenAPI spec {}: {}", specFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveSchemaPath(File specFile, Map<String, Object> operation) {
        // Try x--response first (set by OpenApiProcessor.simplifyOperationResponseInfo)
        Map<String, Object> response = (Map<String, Object>) operation.get("x--response");
        if (response != null) {
            String ref = str(response, "x--original-$ref", null);
            if (ref != null) {
                String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
                if (!filePart.isBlank()) {
                    return specFile.getParentFile().toPath().resolve(filePart).normalize().toAbsolutePath().toString();
                }
            }
        }
        return null;
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
