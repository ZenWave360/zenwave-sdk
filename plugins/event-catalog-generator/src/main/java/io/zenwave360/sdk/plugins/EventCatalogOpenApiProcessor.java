package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses OpenAPI specs declared in each service entry and augments the service map
 * with extracted queries and specifications.
 */
public class EventCatalogOpenApiProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

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

            String specPath = resolveSpecPath(repository, spec);
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("OpenAPI spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && service.get("_version") == null) {
                service.put("_version", version);
            }

            addToList(service, "_specifications", specFile.getAbsolutePath());

            Map<String, Object> paths = map(model.get("paths"));
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                Map<String, Object> pathItem = map(pathEntry.getValue());
                Map<String, Object> operation = map(pathItem.get("get"));
                if (operation.isEmpty()) {
                    continue;
                }

                String operationId = str(operation, "operationId", null);
                if (operationId == null) continue;

                String queryId = serviceId + "." + operationId;
                String name = str(operation, "summary", operationId);
                String schemaPath = resolveSchemaPath(specFile, operation);

                Map<String, Object> query = new LinkedHashMap<>();
                query.put("id", queryId);
                query.put("name", name);
                query.put("summary", str(operation, "description", str(operation, "summary", null)));
                query.put("version", version != null ? version : str(service, "version", "0.0.1"));
                if (schemaPath != null) query.put("schemaPath", schemaPath);
                query.put("operation", buildOperation(pathEntry.getKey(), operation));

                addToList(service, "_queries", query);
            }
        }
    }

    private Map<String, Object> buildOperation(String path, Map<String, Object> operation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", "GET");
        result.put("path", path);
        List<String> statusCodes = new ArrayList<>(map(operation.get("responses")).keySet());
        if (!statusCodes.isEmpty()) {
            result.put("statusCodes", statusCodes);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSpec(File specFile) {
        try {
            return yamlMapper.readValue(specFile, Map.class);
        } catch (IOException e) {
            log.warn("Failed to parse OpenAPI spec {}: {}", specFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private String resolveSchemaPath(File specFile, Map<String, Object> operation) {
        Map<String, Object> responses = map(operation.get("responses"));
        for (Object responseValue : responses.values()) {
            Map<String, Object> response = map(responseValue);
            Map<String, Object> content = map(response.get("content"));
            for (Object mediaTypeValue : content.values()) {
                Map<String, Object> mediaType = map(mediaTypeValue);
                String ref = str(map(mediaType.get("schema")), "$ref", null);
                if (ref == null) {
                    continue;
                }
                String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
                if (!filePart.isBlank()) {
                    Path resolved = specFile.getParentFile().toPath().resolve(filePart).normalize();
                    return resolved.toAbsolutePath().toString();
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

    private String resolveSpecPath(String repository, Map<String, Object> spec) {
        String resolvedPath = str(spec, "resolvedPath", null);
        if (resolvedPath != null && !resolvedPath.isBlank()) {
            return resolvedPath;
        }
        return repository + File.separator + spec.get("path");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
