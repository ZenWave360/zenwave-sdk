package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.ZenWaveManifestLoader;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses OpenAPI artifacts declared in each service entry and augments the service map
 * with extracted queries and specification links.
 */
public class EventCatalogOpenApiProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;
    @DocumentedOption(description = "Preferred source for generated frontmatter links.")
    public String linkSource;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        ZenWaveManifestLoader manifestLoader = (ZenWaveManifestLoader) contextModel.get("manifestLoader");
        if (architecture == null || manifest == null || manifestLoader == null) return contextModel;

        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());
        ManifestLoadOptions contentOptions = ManifestRuntimeSupport.contentOptions(preferredSource, allowFallback);

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> serviceMap = (Map<String, Object>) entry.getValue();
            ManifestService service = ManifestRuntimeSupport.findService(manifest, serviceMap);
            if (service == null) {
                continue;
            }
            String serviceId = str(serviceMap, "id", entry.getKey());
            processOpenApiArtifacts(manifestLoader, manifest, service, serviceMap, serviceId, contentOptions);
        }

        return contextModel;
    }

    private void processOpenApiArtifacts(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest,
                                         ManifestService manifestService, Map<String, Object> serviceMap, String serviceId,
                                         ManifestLoadOptions contentOptions) {
        for (ManifestArtifact artifact : ManifestRuntimeSupport.findArtifacts(manifestService, "openapi")) {
            String specText;
            try {
                specText = ManifestRuntimeSupport.loadArtifactText(manifestLoader, manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("OpenAPI artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && serviceMap.get("_version") == null) {
                serviceMap.put("_version", version);
            }

            annotateArtifactLink(manifestLoader, manifest, manifestService, serviceMap, artifact, contentOptions);

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
                String schemaPath = resolveSchemaLink(manifestLoader, manifest, manifestService, artifact, operation);

                Map<String, Object> query = new LinkedHashMap<>();
                query.put("id", queryId);
                query.put("name", name);
                query.put("summary", str(operation, "description", str(operation, "summary", null)));
                query.put("version", version != null ? version : str(serviceMap, "version", "0.0.1"));
                if (schemaPath != null) query.put("schemaPath", schemaPath);
                query.put("operation", buildOperation(pathEntry.getKey(), operation));

                addToList(serviceMap, "_queries", query);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void annotateArtifactLink(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest,
                                      ManifestService manifestService, Map<String, Object> serviceMap,
                                      ManifestArtifact artifact, ManifestLoadOptions contentOptions) {
        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) serviceMap.get("artifacts");
        if (artifacts == null) {
            return;
        }
        for (Map<String, Object> artifactMap : artifacts) {
            if (artifact.getPath().equals(artifactMap.get("path")) && artifact.getType().equals(artifactMap.get("type"))) {
                String linkUri = ManifestRuntimeSupport.resolveLinkUri(
                        manifestLoader, manifest, manifestService, artifact, artifact.getPath(), linkSource);
                if (linkUri != null) {
                    artifactMap.put("linkUri", linkUri);
                }
                String buildPath = ManifestRuntimeSupport.resolveContentPath(
                        manifestLoader, manifest, manifestService, artifact, contentOptions);
                if (buildPath != null) {
                    artifactMap.put("buildPath", buildPath);
                }
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
    private Map<String, Object> parseSpec(String specText, String serviceRef, String pathExpression) {
        try {
            return yamlMapper.readValue(specText, Map.class);
        } catch (IOException e) {
            log.warn("Failed to parse OpenAPI artifact {} for {}: {}", pathExpression, serviceRef, e.getMessage());
            return null;
        }
    }

    private String resolveSchemaLink(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest, ManifestService manifestService,
                                     ManifestArtifact artifact, Map<String, Object> operation) {
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
                    return ManifestRuntimeSupport.resolveLinkUri(
                            manifestLoader, manifest, manifestService, artifact, filePart, linkSource);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
