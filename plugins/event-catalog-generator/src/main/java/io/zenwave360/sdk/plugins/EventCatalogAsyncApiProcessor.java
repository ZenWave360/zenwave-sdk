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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Parses AsyncAPI specs declared in each service entry and augments the service map
 * with extracted events, commands, sends, receives, version, and channels.
 */
public class EventCatalogAsyncApiProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        if (architecture == null) return contextModel;

        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());
        Map<String, Map<String, String>> channelAddressIndex = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) entry.getValue();
            String serviceId = str(service, "id", entry.getKey());
            processPublicSpec(service, serviceId, channelAddressIndex);
        }

        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) entry.getValue();
            processClientSpec(service, channelAddressIndex);
        }

        return contextModel;
    }

    @SuppressWarnings("unchecked")
    private void processPublicSpec(Map<String, Object> service, String serviceId,
                                   Map<String, Map<String, String>> channelAddressIndex) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"asyncapi".equals(spec.get("type"))) continue;

            String specPath = resolveSpecPath(repository, spec);
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("AsyncAPI spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && service.get("_version") == null) {
                service.put("_version", version);
            }

            addToList(service, "_specifications", specFile.getAbsolutePath());

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
                channelArtifact.put("version", version != null ? version : str(service, "version", "0.0.1"));
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
                addToList(service, "_channels", channelArtifact);
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

                String eventId = serviceId + "." + channelKey;
                String channelId = serviceId + "." + channelKey;

                Map<String, Object> artifact = new LinkedHashMap<>();
                artifact.put("id", eventId);
                artifact.put("name", str(channel, "summary", channelKey));
                artifact.put("summary", str(channel, "description", str(channel, "summary", null)));
                artifact.put("version", version != null ? version : str(service, "version", "0.0.1"));
                artifact.put("channelId", channelId);
                String schemaPath = resolveSchemaPath(specFile, channel);
                if (schemaPath != null) {
                    artifact.put("schemaPath", schemaPath);
                }

                if ("send".equals(action)) {
                    addToList(service, "_events", artifact);
                    addToList(service, "_sends", eventId);
                } else if ("receive".equals(action)) {
                    addToList(service, "_commands", artifact);
                    addToList(service, "_receives", eventId);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processClientSpec(Map<String, Object> service, Map<String, Map<String, String>> channelAddressIndex) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"asyncapi-client".equals(spec.get("type"))) continue;

            String specPath = resolveSpecPath(repository, spec);
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("asyncapi-client spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
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
                String eventId;
                if (ownerInfo != null) {
                    eventId = ownerInfo.get("serviceId") + "." + ownerInfo.get("channelKey");
                } else {
                    String channelKey = addressToChannelKey.get(address);
                    eventId = channelKey != null ? channelKey : address;
                    log.warn("Channel address '{}' not found in index — using fallback id '{}'", address, eventId);
                }

                if ("send".equals(action)) {
                    addToList(service, "_sends", eventId);
                } else if ("receive".equals(action)) {
                    addToList(service, "_receives", eventId);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSpec(File specFile) {
        try {
            return yamlMapper.readValue(specFile, Map.class);
        } catch (IOException e) {
            log.warn("Failed to parse AsyncAPI spec {}: {}", specFile.getAbsolutePath(), e.getMessage());
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

    private String resolveSchemaPath(File specFile, Map<String, Object> channel) {
        Map<String, Object> messages = map(channel.get("messages"));
        for (Object messageValue : messages.values()) {
            Map<String, Object> message = map(messageValue);
            String ref = str(map(map(message.get("payload")).get("schema")), "$ref", null);
            if (ref == null) {
                continue;
            }
            String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
            if (!filePart.isBlank()) {
                Path resolved = specFile.getParentFile().toPath().resolve(filePart).normalize();
                return resolved.toAbsolutePath().toString();
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
