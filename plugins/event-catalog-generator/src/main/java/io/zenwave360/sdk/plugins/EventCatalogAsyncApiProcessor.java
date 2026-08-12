package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestConsumerReference;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestResolvedResource;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses AsyncAPI artifacts declared by typed manifest services and augments the EventCatalog model
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
    public Map<String, Object> process(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        BlockingZenWaveManifestLoader manifestRuntime =
                (BlockingZenWaveManifestLoader) contextModel.get("manifestRuntime");
        if (manifest == null || eventCatalog == null || manifestRuntime == null) return contextModel;

        Map<String, Map<String, String>> channelAddressIndex = new LinkedHashMap<>();
        ManifestLoadOptions contentOptions = new ManifestLoadOptions()
                .withPreferredSource(preferredSource)
                .withFallback(allowFallback == null || allowFallback);

        for (ManifestService service : manifest.getServices()) {
            Map<String, Object> serviceData = eventCatalog.serviceData(service);
            processPublicArtifact(
                    manifestRuntime, manifest, eventCatalog, service, serviceData,
                    eventCatalog.catalogServiceId(service), contentOptions, channelAddressIndex);
        }

        Set<String> edgeManagedConsumerServices = qualifiedConsumerServiceRefs(manifest);
        for (ManifestService service : manifest.getServices()) {
            if (!edgeManagedConsumerServices.contains(service.getServiceRef())) {
                processClientArtifact(
                        manifestRuntime, manifest, service, eventCatalog.serviceData(service),
                        contentOptions, channelAddressIndex);
            }
        }

        return contextModel;
    }

    private void processPublicArtifact(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest,
                                       EventCatalogModel eventCatalog, ManifestService manifestService,
                                       Map<String, Object> serviceData, String serviceId,
                                       ManifestLoadOptions contentOptions,
                                       Map<String, Map<String, String>> channelAddressIndex) {
        for (ManifestArtifact artifact : manifestService.findArtifacts("asyncapi")) {
            String specText;
            try {
                specText = manifestRuntime.loadArtifactText(manifest, manifestService, artifact, contentOptions);
            } catch (Exception e) {
                log.warn("AsyncAPI artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
                continue;
            }

            Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
            if (model == null) continue;

            String version = str(map(model.get("info")), "version", null);
            if (version != null && serviceData.get("_version") == null) {
                serviceData.put("_version", version);
            }

            annotateArtifactLink(manifestRuntime, manifest, eventCatalog, manifestService, artifact, contentOptions);

            Map<String, Object> channels = map(model.get("channels"));
            Map<String, Object> operations = map(model.get("operations"));
            Map<String, Object> componentMessages = map(map(model.get("components")).get("messages"));
            List<String> protocols = uniqueStrings(values(map(model.get("servers"))), "protocol");
            String specificationUrl = manifestRuntime.getDelegate().artifactReferenceUri(
                    manifest, manifestService, artifact, null,
                    new ManifestLoadOptions(linkSource, false));

            Map<String, String> addressToChannelKey = new LinkedHashMap<>();
            Map<String, Object> channelIndex = new LinkedHashMap<>();
            eventCatalog.artifactRuntimeData(manifestService, artifact).put("channelIndex", channelIndex);
            for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
                Map<String, Object> channel = map(channelEntry.getValue());
                String channelKey = channelEntry.getKey();
                String channelId = serviceId + "." + channelKey;
                String address = str(channel, "address", null);

                Map<String, Object> channelArtifact = new LinkedHashMap<>();
                channelArtifact.put("id", channelId);
                channelArtifact.put("name", str(channel, "summary", channelKey));
                channelArtifact.put("summary", str(channel, "description", str(channel, "summary", null)));
                channelArtifact.put("version", version != null ? version : serviceVersion(manifestService));
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
                addToList(serviceData, "_channels", channelArtifact);

                Map<String, Object> indexedChannel = new LinkedHashMap<>();
                indexedChannel.put("channelKey", channelKey);
                indexedChannel.put("channelId", channelId);
                indexedChannel.put("messageId", channelId);
                if (address != null) indexedChannel.put("address", address);
                indexedChannel.put("operations", new ArrayList<Map<String, Object>>());
                channelIndex.put(channelKey, indexedChannel);
            }

            Map<String, LinkedHashSet<String>> channelActions = new LinkedHashMap<>();
            for (Map.Entry<String, Object> operationEntry : operations.entrySet()) {
                Map<String, Object> operation = map(operationEntry.getValue());
                String action = str(operation, "action", null);
                String channelKey = resolveChannelKey(operation.get("channel"), channels, addressToChannelKey);
                if (!("send".equals(action) || "receive".equals(action)) || channelKey == null) {
                    continue;
                }
                channelActions.computeIfAbsent(channelKey, ignored -> new LinkedHashSet<>()).add(action);
                Map<String, Object> indexedChannel = map(channelIndex.get(channelKey));
                if (!indexedChannel.isEmpty()) {
                    Map<String, Object> indexedOperation = new LinkedHashMap<>();
                    indexedOperation.put("operationId", operationEntry.getKey());
                    indexedOperation.put("action", action);
                    addToList(indexedChannel, "operations", indexedOperation);
                }
            }

            for (Map.Entry<String, Object> operationEntry : operations.entrySet()) {
                Map<String, Object> operation = map(operationEntry.getValue());
                String action = str(operation, "action", null);
                String channelKey = resolveChannelKey(operation.get("channel"), channels, addressToChannelKey);
                Map<String, Object> channel = channelKey != null ? map(channels.get(channelKey)) : Map.of();
                if (!("send".equals(action) || "receive".equals(action)) || channelKey == null || channel.isEmpty()) {
                    continue;
                }

                String messageId = serviceId + "." + channelKey;
                String channelId = serviceId + "." + channelKey;

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("id", messageId);
                message.put("name", str(channel, "summary", channelKey));
                message.put("summary", str(channel, "description", str(channel, "summary", null)));
                message.put("version", version != null ? version : serviceVersion(manifestService));
                message.put("channelId", channelId);

                MessageSelection messageSelection = resolveMessageSelection(operation, channel, channelKey, componentMessages);
                String schemaPath = resolveSchemaLink(
                        manifestRuntime, manifest, manifestService, artifact, messageSelection);
                if (schemaPath != null) {
                    message.put("schemaPath", schemaPath);
                }
                if (isHttpUrl(specificationUrl) && messageSelection != null && messageSelection.hasRemoteSelector()) {
                    message.put("_remoteSchemaUrl", specificationUrl);
                    message.put("_remoteSchemaChannel", messageSelection.channel);
                    message.put("_remoteSchemaChannelMessage", messageSelection.channelMessage);
                }

                boolean eventChannel = classifyMessage(
                        channelKey, channel, componentMessages,
                        channelActions.getOrDefault(channelKey, new LinkedHashSet<>())) == MessageKind.EVENT;
                addMessageById(serviceData, eventChannel ? "_events" : "_commands", message);
                addToList(serviceData, "send".equals(action) ? "_sends" : "_receives", messageId);
            }
        }
    }

    private Set<String> qualifiedConsumerServiceRefs(ZenWaveManifest manifest) {
        Set<String> result = new LinkedHashSet<>();
        for (ManifestService provider : manifest.getServices()) {
            for (String rawReference : provider.getConsumers()) {
                if (!rawReference.contains("#")) continue;
                try {
                    ManifestConsumerReference reference = ManifestConsumerReference.parse(rawReference);
                    ManifestService consumer = manifest.findService(reference.getServiceReference());
                    if (consumer == null) {
                        consumer = manifest.findService(provider.getDomainKey() + "/" + reference.getServiceReference());
                    }
                    if (consumer != null) result.add(consumer.getServiceRef());
                } catch (IllegalArgumentException ignored) {
                    // ManifestConsumerIndex reports malformed qualified references later in the chain.
                }
            }
        }
        return result;
    }

    /** Explicit extension, then naming convention, then provider direction. */
    private MessageKind classifyMessage(String channelKey, Map<String, Object> channel,
                                        Map<String, Object> componentMessages, Set<String> actions) {
        MessageKind channelType = messageKind(channel.get("x-message-type"));
        if (channelType != null) return channelType;

        LinkedHashSet<MessageKind> messageTypes = new LinkedHashSet<>();
        for (Object message : map(channel.get("messages")).values()) {
            MessageKind messageType = messageKind(
                    resolveChannelMessage(message, componentMessages).get("x-message-type"));
            if (messageType != null) messageTypes.add(messageType);
        }
        if (messageTypes.size() == 1) return messageTypes.iterator().next();
        if (messageTypes.size() > 1) {
            log.warn("Channel {} declares conflicting x-message-type values; falling back to its name and direction",
                    channelKey);
        }

        String normalizedKey = channelKey.toLowerCase(Locale.ROOT);
        boolean eventName = normalizedKey.contains("event");
        boolean commandName = normalizedKey.contains("command");
        if (eventName != commandName) return eventName ? MessageKind.EVENT : MessageKind.COMMAND;
        return actions.contains("send") ? MessageKind.EVENT : MessageKind.COMMAND;
    }

    private MessageKind messageKind(Object value) {
        if (value == null) return null;
        return switch (value.toString().trim().toLowerCase(Locale.ROOT)) {
            case "event" -> MessageKind.EVENT;
            case "command" -> MessageKind.COMMAND;
            default -> null;
        };
    }

    private enum MessageKind {
        EVENT,
        COMMAND
    }

    private void processClientArtifact(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest,
                                       ManifestService manifestService, Map<String, Object> serviceData,
                                       ManifestLoadOptions contentOptions,
                                       Map<String, Map<String, String>> channelAddressIndex) {
        for (ManifestArtifact artifact : manifestService.findArtifacts("asyncapi-client")) {
            String specText;
            try {
                specText = manifestRuntime.loadArtifactText(manifest, manifestService, artifact, contentOptions);
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
                if (ownerInfo == null) {
                    log.warn("Legacy AsyncAPI client channel address '{}' was not found in a provider contract", address);
                    continue;
                }
                String messageId = ownerInfo.get("serviceId") + "." + ownerInfo.get("channelKey");

                if ("send".equals(action)) {
                    addToList(serviceData, "_sends", messageId);
                } else if ("receive".equals(action)) {
                    addToList(serviceData, "_receives", messageId);
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

    private String resolveChannelKey(Object channelRef, Map<String, Object> channels,
                                     Map<String, String> addressToChannelKey) {
        Map<String, Object> channel = map(channelRef);
        String ref = str(channel, "$ref", null);
        String prefix = "#/channels/";
        if (ref != null && ref.startsWith(prefix)) {
            String key = decodeJsonPointerSegment(ref.substring(prefix.length()));
            return channels.containsKey(key) ? key : null;
        }
        String address = str(channel, "address", null);
        return address != null ? addressToChannelKey.get(address) : null;
    }

    private String resolveSchemaLink(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest, ManifestService manifestService,
                                     ManifestArtifact artifact, MessageSelection messageSelection) {
        if (messageSelection == null) {
            return null;
        }
        String ref = str(map(map(messageSelection.message.get("payload")).get("schema")), "$ref", null);
        if (ref != null) {
            String filePart = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
            if (!filePart.isBlank()) {
                return manifestRuntime.getDelegate().artifactReferenceUri(
                        manifest, manifestService, artifact, filePart,
                        new ManifestLoadOptions(linkSource, false));
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

    private MessageSelection resolveMessageSelection(Map<String, Object> operation, Map<String, Object> channel, String channelKey,
                                                      Map<String, Object> componentMessages) {
        Map<String, Object> channelMessages = map(channel.get("messages"));
        if (channelMessages.isEmpty()) {
            return null;
        }
        Object operationMessages = operation.get("messages");
        if (operationMessages instanceof Collection<?> collection) {
            for (Object messageValue : collection) {
                for (Map.Entry<String, Object> channelMessage : channelMessages.entrySet()) {
                    if (operationMessageMatchesChannelMessage(messageValue, channelKey, channelMessage.getKey(), channelMessage.getValue())) {
                        return MessageSelection.channel(
                                channelKey, channelMessage.getKey(), resolveChannelMessage(channelMessage.getValue(), componentMessages));
                    }
                }
            }
            log.warn("AsyncAPI operation messages must be present in channel {}. No remote schema was selected.", channelKey);
            return null;
        }

        Map.Entry<String, Object> firstMessage = channelMessages.entrySet().iterator().next();
        return MessageSelection.channel(
                channelKey, firstMessage.getKey(), resolveChannelMessage(firstMessage.getValue(), componentMessages));
    }

    private Map<String, Object> resolveChannelMessage(Object channelMessageValue, Map<String, Object> componentMessages) {
        Map<String, Object> channelMessage = map(channelMessageValue);
        String ref = str(channelMessage, "$ref", null);
        String prefix = "#/components/messages/";
        if (ref != null && ref.startsWith(prefix)) {
            Map<String, Object> componentMessage = map(componentMessages.get(decodeJsonPointerSegment(ref.substring(prefix.length()))));
            if (!componentMessage.isEmpty()) {
                return componentMessage;
            }
        }
        return channelMessage;
    }

    private boolean operationMessageMatchesChannelMessage(Object operationMessageValue, String channelKey,
                                                           String channelMessageName, Object channelMessageValue) {
        if (operationMessageValue == channelMessageValue) {
            return true;
        }
        String operationRef = str(map(operationMessageValue), "$ref", null);
        if (operationRef == null) {
            return false;
        }
        String channelRef = str(map(channelMessageValue), "$ref", null);
        if (operationRef.equals(channelRef)) {
            return true;
        }
        return operationRef.equals("#/channels/" + encodeJsonPointerSegment(channelKey)
                + "/messages/" + encodeJsonPointerSegment(channelMessageName));
    }

    private String encodeJsonPointerSegment(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private String decodeJsonPointerSegment(String value) {
        return value.replace("~1", "/").replace("~0", "~");
    }

    private static final class MessageSelection {
        private final String channel;
        private final String channelMessage;
        private final Map<String, Object> message;

        private MessageSelection(String channel, String channelMessage, Map<String, Object> message) {
            this.channel = channel;
            this.channelMessage = channelMessage;
            this.message = message;
        }

        private static MessageSelection channel(String channel, String channelMessage, Map<String, Object> message) {
            return new MessageSelection(channel, channelMessage, message);
        }

        private boolean hasRemoteSelector() {
            return channel != null && channelMessage != null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Map<String, Object> map, String key, T value) {
        List<T> list = (List<T>) map.computeIfAbsent(key, k -> new ArrayList<>());
        if (!list.contains(value)) list.add(value);
    }

    @SuppressWarnings("unchecked")
    private void addMessageById(Map<String, Object> map, String key, Map<String, Object> message) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.computeIfAbsent(key, ignored -> new ArrayList<>());
        String id = str(message, "id", null);
        if (list.stream().noneMatch(existing -> id != null && id.equals(str(existing, "id", null)))) {
            list.add(message);
        }
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
