package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestResolvedResource;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.graph.ArchitectureGraphIds;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses OpenAPI artifacts declared by typed manifest services and augments the EventCatalog model
 * with extracted queries and specification links.
 */
public class EventCatalogOpenApiProcessor implements Processor {

    private static final List<String> HTTP_METHODS =
            List.of("get", "head", "post", "put", "patch", "delete");

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;
    @DocumentedOption(description = "Preferred source for generated frontmatter links.")
    public String linkSource;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        BlockingZenWaveManifestLoader manifestRuntime =
                (BlockingZenWaveManifestLoader) contextModel.get("manifestRuntime");
        if (manifest == null || eventCatalog == null || manifestRuntime == null) return contextModel;

        ManifestLoadOptions contentOptions = new ManifestLoadOptions()
                .withPreferredSource(preferredSource)
                .withFallback(allowFallback == null || allowFallback);

        for (ManifestService service : manifest.getServices()) {
            processOpenApiArtifacts(
                    manifestRuntime, manifest, eventCatalog, service,
                    eventCatalog.serviceData(service), eventCatalog.catalogServiceId(service), contentOptions);
        }

        return contextModel;
    }

    private void processOpenApiArtifacts(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest,
                                         EventCatalogModel eventCatalog, ManifestService manifestService,
                                         Map<String, Object> serviceData, String serviceId,
                                         ManifestLoadOptions contentOptions) {
        for (ManifestArtifact artifact : manifestService.findArtifacts("openapi")) {
            String specText;
            try {
                specText = manifestRuntime.loadArtifactText(manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("OpenAPI artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && serviceData.get("_version") == null) {
                serviceData.put("_version", version);
            }

            annotateArtifactLink(manifestRuntime, manifest, eventCatalog, manifestService, artifact, contentOptions);

            Map<String, Object> paths = map(model.get("paths"));
            Map<String, Object> componentResponses = map(map(model.get("components")).get("responses"));
            String specificationUrl = manifestRuntime.getDelegate().artifactReferenceUri(
                    manifest, manifestService, artifact, null,
                    new ManifestLoadOptions(linkSource, false));
            String resolvedArtifactId = manifestRuntime.getDelegate()
                    .artifactResolutionContext(manifest, manifestService, artifact).getArtifactId();
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                Map<String, Object> pathItem = map(pathEntry.getValue());
                for (String method : HTTP_METHODS) {
                    Map<String, Object> operation = map(pathItem.get(method));
                    if (operation.isEmpty()) continue;

                    String operationId = str(operation, "operationId", null);
                    if (operationId == null) continue;

                    String operationResourceId = serviceId + "." + operationId;
                    String name = str(operation, "summary", operationId);
                    ResponseSchemaSelection responseSchema = selectResponseSchema(operation, componentResponses);
                    String schemaPath = resolveSchemaLink(
                            manifestRuntime, manifest, manifestService, artifact, responseSchema);

                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("id", operationResourceId);
                    candidate.put("operationId", operationId);
                    candidate.put("name", name);
                    candidate.put("summary", str(operation, "description", str(operation, "summary", null)));
                    candidate.put("version", version != null ? version : serviceVersion(manifestService));
                    candidate.put("_bindingTransport", "openapi");
                    candidate.put("_graphResourceNodeId", ArchitectureGraphIds.apiOperation(
                            manifestService.getServiceRef(), resolvedArtifactId, operationId));
                    candidate.put("_graphBindingTransport", "openapi");
                    if (schemaPath != null) candidate.put("schemaPath", schemaPath);
                    candidate.put("operation", buildOperation(method, pathEntry.getKey(), operation));
                    if (isQueryMethod(method) && isHttpUrl(specificationUrl) && responseSchema != null) {
                        candidate.put("_remoteSchemaUrl", specificationUrl);
                        candidate.put("_remoteSchemaOperationId", operationId);
                        candidate.put("_remoteSchemaOperationTarget", "response");
                        candidate.put("_remoteSchemaStatusCode", responseSchema.statusCode);
                        candidate.put("_remoteSchemaMediaType", responseSchema.mediaType);
                    }

                    addToList(serviceData, isQueryMethod(method) ? "_queries" : "_restCommands", candidate);
                }
            }
        }
    }

    private void annotateArtifactLink(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest,
                                      EventCatalogModel eventCatalog, ManifestService manifestService,
                                      ManifestArtifact artifact, ManifestLoadOptions contentOptions) {
        Map<String, Object> artifactData = eventCatalog.artifactData(manifestService, artifact);
        String linkUri = manifestRuntime.getDelegate().artifactReferenceUri(
                manifest, manifestService, artifact, null,
                new ManifestLoadOptions(linkSource, false));
        if (linkUri != null) {
            artifactData.put("linkUri", linkUri);
        }
        String buildPath = resolvedContentPath(
                manifestRuntime.resolveArtifact(manifest, manifestService, artifact, contentOptions));
        if (buildPath != null) {
            artifactData.put("buildPath", buildPath);
        }
    }

    private Map<String, Object> buildOperation(String method, String path, Map<String, Object> operation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", method.toUpperCase(Locale.ROOT));
        result.put("path", path);
        List<String> statusCodes = new ArrayList<>(map(operation.get("responses")).keySet());
        if (!statusCodes.isEmpty()) {
            result.put("statusCodes", statusCodes);
        }
        return result;
    }

    private boolean isQueryMethod(String method) {
        return "get".equals(method) || "head".equals(method);
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

    private String resolveSchemaLink(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest, ManifestService manifestService,
                                     ManifestArtifact artifact, ResponseSchemaSelection selection) {
        if (selection != null) {
            String ref = str(selection.schema, "$ref", null);
            if (ref != null) {
                String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
                if (!filePart.isBlank()) {
                    return manifestRuntime.getDelegate().artifactReferenceUri(
                            manifest, manifestService, artifact, filePart,
                            new ManifestLoadOptions(linkSource, false));
                }
            }
        }
        return null;
    }

    private String resolvedContentPath(ManifestResolvedResource resource) {
        if (resource == null || resource.getUri() == null) {
            return null;
        }
        URI uri = URI.create(resource.getUri());
        return "file".equalsIgnoreCase(uri.getScheme())
                ? Path.of(uri).toString()
                : resource.referenceUri();
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            String scheme = URI.create(value).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String serviceVersion(ManifestService service) {
        String version = service.documentVersion();
        return version != null && !version.isBlank() ? version : "0.0.1";
    }

    private ResponseSchemaSelection selectResponseSchema(Map<String, Object> operation,
                                                         Map<String, Object> componentResponses) {
        Map<String, Object> responses = map(operation.get("responses"));
        String statusCode = preferredStatusCode(responses);
        if (statusCode == null) {
            return null;
        }

        Map<String, Object> response = resolveResponse(responses.get(statusCode), componentResponses);
        Map<String, Object> content = map(response.get("content"));
        String mediaType = preferredMediaType(content);
        if (mediaType == null) {
            return null;
        }

        Map<String, Object> schema = map(map(content.get(mediaType)).get("schema"));
        return schema.isEmpty() ? null : new ResponseSchemaSelection(statusCode, mediaType, schema);
    }

    private String preferredStatusCode(Map<String, Object> responses) {
        if (responses.containsKey("200")) {
            return "200";
        }
        String success = responses.keySet().stream()
                .filter(code -> code.matches("2\\d\\d"))
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (success != null) {
            return success;
        }
        return responses.containsKey("default") ? "default" : null;
    }

    private String preferredMediaType(Map<String, Object> content) {
        if (content.containsKey("application/json")) {
            return "application/json";
        }
        return content.keySet().stream().sorted().findFirst().orElse(null);
    }

    private Map<String, Object> resolveResponse(Object responseValue, Map<String, Object> componentResponses) {
        Map<String, Object> response = map(responseValue);
        String ref = str(response, "$ref", null);
        String prefix = "#/components/responses/";
        if (ref != null && ref.startsWith(prefix)) {
            return map(componentResponses.get(decodeJsonPointerSegment(ref.substring(prefix.length()))));
        }
        return response;
    }

    private String decodeJsonPointerSegment(String value) {
        return value.replace("~1", "/").replace("~0", "~");
    }

    private static final class ResponseSchemaSelection {
        private final String statusCode;
        private final String mediaType;
        private final Map<String, Object> schema;

        private ResponseSchemaSelection(String statusCode, String mediaType, Map<String, Object> schema) {
            this.statusCode = statusCode;
            this.mediaType = mediaType;
            this.schema = schema;
        }
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
