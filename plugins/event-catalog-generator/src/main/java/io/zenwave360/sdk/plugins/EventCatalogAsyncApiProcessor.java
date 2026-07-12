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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Parses AsyncAPI artifacts declared in each service entry and augments the service map
 * with extracted events, commands, sends, receives, version, and channels.
 */
public class EventCatalogAsyncApiProcessor implements Processor {

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
        Map<String, Map<String, String>> channelAddressIndex = new LinkedHashMap<>();
        ManifestLoadOptions contentOptions = ManifestRuntimeSupport.contentOptions(preferredSource, allowFallback);

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> serviceMap = (Map<String, Object>) entry.getValue();
            ManifestService service = ManifestRuntimeSupport.findService(manifest, serviceMap);
            if (service == null) {
                continue;
            }
            String serviceId = str(serviceMap, "id", entry.getKey());
            processPublicArtifact(manifestLoader, manifest, service, serviceMap, serviceId, contentOptions, channelAddressIndex);
        }

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> serviceMap = (Map<String, Object>) entry.getValue();
            ManifestService service = ManifestRuntimeSupport.findService(manifest, serviceMap);
            if (service == null) {
                continue;
            }
            processClientArtifact(manifestLoader, manifest, service, serviceMap, contentOptions, channelAddressIndex);
        }

        return contextModel;
    }

    private void processPublicArtifact(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest,
                                       ManifestService manifestService, Map<String, Object> serviceMap, String serviceId,
                                       ManifestLoadOptions contentOptions,
                                       Map<String, Map<String, String>> channelAddressIndex) {
        for (ManifestArtifact artifact : ManifestRuntimeSupport.findArtifacts(manifestService, "asyncapi")) {
            String specText;
            try {
                specText = ManifestRuntimeSupport.loadArtifactText(manifestLoader, manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("AsyncAPI artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && serviceMap.get("_version") == null) {
                serviceMap.put("_version", version);
            }

            annotateArtifactLink(manifestLoader, manifest, manifestService, serviceMap, artifact, contentOptions);

            Map<String, Object> channels = map(model.get("channels"));
            Map<String, Object> operations = map(model.get("operations"));
            List<String> protocols = uniqueStrings(values(map(model.get("servers"))), "protocol");

            Map<String, String> addressToChannelKey = new LinkedHashMap<>();
            for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
                Map<String, Object> channel = map(channelEntry.getValue());
                String channelKey = channelEntry.getKey();
                String channelId = serviceId + "." + channelKey;
                String address = str(channel, "address", null);

                Map<String, Object> channelArtifact = new LinkedHashMap<>();
                channelArtifact.put("id", channelId);
                channelArtifact.put("name", str(channel, "summary", channelKey));
                channelArtifact.put("summary", str(channel, "description", str(channel, "summary", null)));
                channelArtifact.put("version", version != null ? version : str(serviceMap, "version", "0.0.1"));
                if (address != null) {
                    channelArtifact.put("address", address);
                    addressToChannelKey.put(address, channelKey);
                    channelAddressIndex.put(address, Map.of(
                            "serviceId", serviceId,
                            "channelKey", channelKey,
                            "channelId", channelId));
                }
                if (!protocols.isEmpty()) {
                    channelArtifact.put("protocols", protocols);
                }
                addToList(serviceMap, "_channels", channelArtifact);
            }

            for (Object operationValue : operations.values()) {
                Map<String, Object> operation = map(operationValue);
                String action = str(operation, "action", null);
                Map<String, Object> channel = resolveChannel(operation.get("channel"), channels);
                if (action == null || channel.isEmpty()) {
                    continue;
                }

                String address = str(channel, "address", null);
                String channelKey = addressToChannelKey.get(address);
                if (channelKey == null) {
                    continue;
                }

                String messageId = serviceId + "." + channelKey;
                String channelId = serviceId + "." + channelKey;

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("id", messageId);
                message.put("name", str(channel, "summary", channelKey));
                message.put("summary", str(channel, "description", str(channel, "summary", null)));
                message.put("version", version != null ? version : str(serviceMap, "version", "0.0.1"));
                message.put("channelId", channelId);

                String schemaPath = resolveSchemaLink(manifestLoader, manifest, manifestService, artifact, channel);
                if (schemaPath != null) {
                    message.put("schemaPath", schemaPath);
                }

                if ("send".equals(action)) {
                    addToList(serviceMap, "_events", message);
                    addToList(serviceMap, "_sends", messageId);
                } else if ("receive".equals(action)) {
                    addToList(serviceMap, "_commands", message);
                    addToList(serviceMap, "_receives", messageId);
                }
            }
        }
    }

    private void processClientArtifact(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest,
                                       ManifestService manifestService, Map<String, Object> serviceMap,
                                       ManifestLoadOptions contentOptions,
                                       Map<String, Map<String, String>> channelAddressIndex) {
        for (ManifestArtifact artifact : ManifestRuntimeSupport.findArtifacts(manifestService, "asyncapi-client")) {
            String specText;
            try {
                specText = ManifestRuntimeSupport.loadArtifactText(manifestLoader, manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("AsyncAPI client artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
            if (model == null) continue;

            Map<String, Object> channels = map(model.get("channels"));
            Map<String, Object> operations = map(model.get("operations"));

            Map<String, String> addressToChannelKey = new LinkedHashMap<>();
            for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
                String address = str(map(channelEntry.getValue()), "address", null);
                if (address != null) {
                    addressToChannelKey.put(address, channelEntry.getKey());
                }
            }

            for (Object operationValue : operations.values()) {
                Map<String, Object> operation = map(operationValue);
                String action = str(operation, "action", null);
                Map<String, Object> channel = resolveChannel(operation.get("channel"), channels);
                String address = str(channel, "address", null);
                if (action == null || address == null) {
                    continue;
                }

                Map<String, String> ownerInfo = channelAddressIndex.get(address);
                String messageId;
                if (ownerInfo != null) {
                    messageId = ownerInfo.get("serviceId") + "." + ownerInfo.get("channelKey");
                } else {
                    String channelKey = addressToChannelKey.get(address);
                    messageId = channelKey != null ? channelKey : address;
                    log.warn("Channel address '{}' not found in index — using fallback id '{}'", address, messageId);
                }

                if ("send".equals(action)) {
                    addToList(serviceMap, "_sends", messageId);
                } else if ("receive".equals(action)) {
                    addToList(serviceMap, "_receives", messageId);
                }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSpec(String specText, String serviceRef, String pathExpression) {
        try {
            return yamlMapper.readValue(specText, Map.class);
        } catch (IOException e) {
            log.warn("Failed to parse AsyncAPI artifact {} for {}: {}", pathExpression, serviceRef, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> resolveChannel(Object channelRef, Map<String, Object> channels) {
        Map<String, Object> channel = map(channelRef);
        String ref = str(channel, "$ref", null);
        if (ref == null) {
            return channel;
        }
        String prefix = "#/channels/";
        if (!ref.startsWith(prefix)) {
            return Map.of();
        }
        return map(channels.get(ref.substring(prefix.length())));
    }

    private String resolveSchemaLink(ZenWaveManifestLoader manifestLoader, ZenWaveManifest manifest, ManifestService manifestService,
                                     ManifestArtifact artifact, Map<String, Object> channel) {
        Map<String, Object> messages = map(channel.get("messages"));
        for (Object messageValue : messages.values()) {
            Map<String, Object> message = map(messageValue);
            String ref = str(map(map(message.get("payload")).get("schema")), "$ref", null);
            if (ref == null) {
                continue;
            }
            String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
            if (!filePart.isBlank()) {
                return ManifestRuntimeSupport.resolveLinkUri(
                        manifestLoader, manifest, manifestService, artifact, filePart, linkSource);
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

    private Collection<Object> values(Map<String, Object> map) {
        return map.values();
    }

    private List<String> uniqueStrings(Collection<Object> values, String key) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            String string = str(map(value), key, null);
            if (string != null) {
                result.add(string);
            }
        }
        return List.copyOf(result);
    }
}
