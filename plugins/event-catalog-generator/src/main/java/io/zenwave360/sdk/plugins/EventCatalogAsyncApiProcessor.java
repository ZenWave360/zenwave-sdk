package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses AsyncAPI specs declared in each service entry and augments the service map
 * with extracted events, commands, sends, receives, version, and specifications.
 *
 * <p>Two-pass processing:
 * <ol>
 *   <li><b>Pass 1</b> — public {@code asyncapi} specs: generates event/command pages,
 *       builds a channel-address index ({@code address → [serviceId, channelKey]}).</li>
 *   <li><b>Pass 2</b> — {@code asyncapi-client} specs: resolves sends/receives against
 *       the index; no event/command pages generated.</li>
 * </ol>
 *
 * <p>Augmented keys written to each service map (prefixed with {@code _}):
 * <ul>
 *   <li>{@code _version} — String from {@code asyncapi info.version}</li>
 *   <li>{@code _events} — List&lt;Map&gt; one entry per send operation</li>
 *   <li>{@code _commands} — List&lt;Map&gt; one entry per receive operation</li>
 *   <li>{@code _sends} — List&lt;String&gt; event/command ids this service sends</li>
 *   <li>{@code _receives} — List&lt;String&gt; event/command ids this service receives</li>
 *   <li>{@code _specifications} — List&lt;String&gt; absolute paths to asyncapi spec files</li>
 * </ul>
 */
public class EventCatalogAsyncApiProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> architecture = (Map<String, Object>) contextModel.get("architecture");
        if (architecture == null) return contextModel;

        Map<String, Object> services = (Map<String, Object>) architecture.getOrDefault("services", Map.of());

        // address → [serviceId, channelKey] — built from public asyncapi specs
        Map<String, String[]> channelAddressIndex = new LinkedHashMap<>();

        // Pass 1: public asyncapi specs
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) entry.getValue();
            String serviceId = str(service, "id", entry.getKey());
            processPublicSpec(service, serviceId, channelAddressIndex);
        }

        // Pass 2: asyncapi-client specs (resolved against the index)
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = (Map<String, Object>) entry.getValue();
            processClientSpec(service, channelAddressIndex);
        }

        return contextModel;
    }

    // -------------------------------------------------------------------------
    // Pass 1: public asyncapi specs
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void processPublicSpec(Map<String, Object> service, String serviceId,
                                   Map<String, String[]> channelAddressIndex) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"asyncapi".equals(spec.get("type"))) continue;

            String specPath = repository + File.separator + spec.get("path");
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("AsyncAPI spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
            if (model == null) continue;

            String version = JSONPath.get(model, "$.info.version");
            if (version != null && service.get("_version") == null) {
                service.put("_version", version);
            }

            addToList(service, "_specifications", specFile.getAbsolutePath());

            Map<String, Map> channels = JSONPath.get(model, "$.channels", Map.of());
            Map<String, Map> operations = JSONPath.get(model, "$.operations", Map.of());

            // Build address → channelKey index for owned channels
            Map<String, String> addressToChannelKey = new LinkedHashMap<>();
            for (Map.Entry<String, Map> ch : channels.entrySet()) {
                String address = str(ch.getValue(), "address", null);
                if (address != null) {
                    addressToChannelKey.put(address, ch.getKey());
                    channelAddressIndex.put(address, new String[]{serviceId, ch.getKey()});
                }
            }

            // Classify operations
            for (Map<?, ?> operation : operations.values()) {
                String action = str(operation, "action", null);
                Map<?, ?> channel = (Map<?, ?>) operation.get("channel");
                if (action == null || channel == null) continue;

                String address = str(channel, "address", null);
                String channelKey = addressToChannelKey.get(address);
                if (channelKey == null) continue;

                String eventId = serviceId + "." + channelKey;
                String name = str(channel, "summary", channelKey);
                String schemaPath = resolveSchemaPath(specFile, channel);

                Map<String, String> artifact = new LinkedHashMap<>();
                artifact.put("id", eventId);
                artifact.put("name", name);
                artifact.put("version", version != null ? version : str(service, "version", "0.0.1"));
                if (schemaPath != null) artifact.put("schemaPath", schemaPath);

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

    // -------------------------------------------------------------------------
    // Pass 2: asyncapi-client specs — sends/receives only, no pages
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void processClientSpec(Map<String, Object> service, Map<String, String[]> channelAddressIndex) {
        List<Map<String, Object>> specs = (List<Map<String, Object>>) service.getOrDefault("specs", List.of());
        String repository = str(service, "repository", ".");

        for (Map<String, Object> spec : specs) {
            if (!"asyncapi-client".equals(spec.get("type"))) continue;

            String specPath = repository + File.separator + spec.get("path");
            File specFile = new File(specPath);
            if (!specFile.exists()) {
                log.warn("asyncapi-client spec not found: {}", specFile.getAbsolutePath());
                continue;
            }

            Map<String, Object> model = parseSpec(specFile);
            if (model == null) continue;

            Map<String, Map> channels = JSONPath.get(model, "$.channels", Map.of());
            Map<String, Map> operations = JSONPath.get(model, "$.operations", Map.of());

            // Build address index for this client file
            Map<String, String> addressToChannelKey = new LinkedHashMap<>();
            for (Map.Entry<String, Map> ch : channels.entrySet()) {
                String address = str(ch.getValue(), "address", null);
                if (address != null) {
                    addressToChannelKey.put(address, ch.getKey());
                }
            }

            for (Map<?, ?> operation : operations.values()) {
                String action = str(operation, "action", null);
                Map<?, ?> channel = (Map<?, ?>) operation.get("channel");
                if (action == null || channel == null) continue;

                String address = str(channel, "address", null);
                if (address == null) continue;

                // Look up the owning service + channel key from the index
                String[] ownerInfo = channelAddressIndex.get(address);
                String eventId;
                if (ownerInfo != null) {
                    eventId = ownerInfo[0] + "." + ownerInfo[1];  // externalServiceId.channelKey
                } else {
                    // Fallback: use channel key from client file (address not in index yet)
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

    // -------------------------------------------------------------------------
    // AsyncAPI parsing
    // -------------------------------------------------------------------------

    private Map<String, Object> parseSpec(File specFile) {
        String tempKey = "_ec_asyncapi_" + System.nanoTime();
        try {
            var parsed = new DefaultYamlParser()
                    .withApiFile(specFile)
                    .withTargetProperty(tempKey)
                    .parse();

            var processor = new AsyncApiProcessor();
            processor.targetProperty = tempKey;
            var processed = processor.process(parsed);

            return (Map<String, Object>) processed.get(tempKey);
        } catch (Exception e) {
            log.warn("Failed to parse AsyncAPI spec {}: {}", specFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Schema path resolution
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String resolveSchemaPath(File specFile, Map<?, ?> channel) {
        Map<String, Map> messages = JSONPath.get(channel, "$.messages", Map.of());
        for (Map message : messages.values()) {
            Map schema = JSONPath.get(message, "$.payload.schema");
            if (schema == null) continue;

            String originalRef = (String) schema.get("x--original-$ref");
            if (originalRef != null) {
                // Strip fragment (#/...) if present
                String filePart = originalRef.contains("#") ? originalRef.substring(0, originalRef.indexOf('#')) : originalRef;
                if (!filePart.isBlank()) {
                    Path resolved = specFile.getParentFile().toPath().resolve(filePart).normalize();
                    return resolved.toAbsolutePath().toString();
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
