package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.AsyncApiAction;
import io.zenwave360.manifest.AsyncApiChannel;
import io.zenwave360.manifest.AsyncApiChannelIndex;
import io.zenwave360.manifest.AsyncApiMessageKind;
import io.zenwave360.manifest.AsyncApiOperationRef;
import io.zenwave360.manifest.BlockingAsyncApiChannelIndex;
import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ManifestArtifact;
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
import java.util.List;
import java.util.Map;

/**
 * Projects typed AsyncAPI channel semantics into EventCatalog while retaining Jackson-side
 * presentation extraction for schema links and message metadata.
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
        if (manifest == null || eventCatalog == null || manifestRuntime == null) {
            return contextModel;
        }

        ManifestLoadOptions contentOptions = new ManifestLoadOptions()
                .withPreferredSource(preferredSource)
                .withFallback(allowFallback == null || allowFallback);
        for (ManifestService service : manifest.getServices()) {
            Map<String, Object> serviceData = eventCatalog.serviceData(service);
            for (ManifestArtifact artifact : service.findArtifacts("asyncapi")) {
                processPublicArtifact(
                        manifestRuntime, manifest, eventCatalog, service, artifact, serviceData,
                        eventCatalog.catalogServiceId(service), contentOptions);
            }
        }
        return contextModel;
    }

    private void processPublicArtifact(BlockingZenWaveManifestLoader manifestRuntime,
                                       ZenWaveManifest manifest,
                                       EventCatalogModel eventCatalog,
                                       ManifestService manifestService,
                                       ManifestArtifact artifact,
                                       Map<String, Object> serviceData,
                                       String serviceId,
                                       ManifestLoadOptions contentOptions) {
        String specText;
        try {
            specText = manifestRuntime.loadArtifactText(manifest, manifestService, artifact, contentOptions);
        } catch (Exception e) {
            log.warn("AsyncAPI artifact could not be loaded for {}: {}", manifestService.getServiceRef(), e.getMessage());
            return;
        }

        Map<String, Object> model = parseSpec(specText, manifestService.getServiceRef(), artifact.getPath());
        if (model == null) return;
        AsyncApiChannelIndex channelIndex = BlockingAsyncApiChannelIndex.parse(specText, artifact.getPath());
        channelIndex.getDiagnostics().forEach(diagnostic -> log.warn(
                "AsyncAPI channel diagnostic [{}] at {}: {}",
                diagnostic.getCode(), diagnostic.getLocation(), diagnostic.getMessage()));

        String version = channelIndex.getVersion();
        if (version != null && serviceData.get("_version") == null) {
            serviceData.put("_version", version);
        }
        annotateArtifactLink(manifestRuntime, manifest, eventCatalog, manifestService, artifact, contentOptions);

        Map<String, Object> channels = map(model.get("channels"));
        Map<String, Object> operations = map(model.get("operations"));
        Map<String, Object> componentMessages = map(map(model.get("components")).get("messages"));
        String specificationUrl = manifestRuntime.getDelegate().artifactReferenceUri(
                manifest, manifestService, artifact, null,
                new ManifestLoadOptions(linkSource, false));

        for (AsyncApiChannel typedChannel : channelIndex.getChannels().values()) {
            String channelKey = typedChannel.getChannelKey();
            String channelId = serviceId + "." + channelKey;
            Map<String, Object> channel = map(channels.get(channelKey));

            Map<String, Object> channelArtifact = new java.util.LinkedHashMap<>();
            channelArtifact.put("id", channelId);
            channelArtifact.put("name", typedChannel.getSummary() != null ? typedChannel.getSummary() : channelKey);
            channelArtifact.put("summary", typedChannel.getDescription() != null
                    ? typedChannel.getDescription()
                    : typedChannel.getSummary());
            channelArtifact.put("version", version != null ? version : serviceVersion(manifestService));
            if (typedChannel.getAddress() != null) channelArtifact.put("address", typedChannel.getAddress());
            if (!channelIndex.getProtocols().isEmpty()) channelArtifact.put("protocols", channelIndex.getProtocols());
            addToList(serviceData, "_channels", channelArtifact);

            for (AsyncApiOperationRef typedOperation : typedChannel.getOperations()) {
                Map<String, Object> operation = map(operations.get(typedOperation.getOperationId()));
                Map<String, Object> message = new java.util.LinkedHashMap<>();
                message.put("id", channelId);
                message.put("name", typedChannel.getSummary() != null ? typedChannel.getSummary() : channelKey);
                message.put("summary", typedChannel.getDescription() != null
                        ? typedChannel.getDescription()
                        : typedChannel.getSummary());
                message.put("version", version != null ? version : serviceVersion(manifestService));
                message.put("channelId", channelId);

                MessageSelection messageSelection = resolveMessageSelection(operation, channel, channelKey, componentMessages);
                String schemaPath = resolveSchemaLink(
                        manifestRuntime, manifest, manifestService, artifact, messageSelection);
                if (schemaPath != null) message.put("schemaPath", schemaPath);
                if (isHttpUrl(specificationUrl) && messageSelection != null && messageSelection.hasRemoteSelector()) {
                    message.put("_remoteSchemaUrl", specificationUrl);
                    message.put("_remoteSchemaChannel", messageSelection.channel);
                    message.put("_remoteSchemaChannelMessage", messageSelection.channelMessage);
                }

                addMessageById(
                        serviceData,
                        typedChannel.getMessageKind() == AsyncApiMessageKind.EVENT ? "_events" : "_commands",
                        message);
                addToList(
                        serviceData,
                        typedOperation.getAction() == AsyncApiAction.SEND ? "_sends" : "_receives",
                        channelId);
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
        if (linkUri != null) artifactData.put("linkUri", linkUri);
        String buildPath = resolvedContentPath(
                manifestRuntime.resolveArtifact(manifest, manifestService, artifact, contentOptions));
        if (buildPath != null) artifactData.put("buildPath", buildPath);
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

    private String resolveSchemaLink(BlockingZenWaveManifestLoader manifestRuntime, ZenWaveManifest manifest,
                                     ManifestService manifestService, ManifestArtifact artifact,
                                     MessageSelection messageSelection) {
        if (messageSelection == null) return null;
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
        if (resource == null || resource.getUri() == null) return null;
        URI uri = URI.create(resource.getUri());
        return "file".equalsIgnoreCase(uri.getScheme()) ? Path.of(uri).toString() : resource.referenceUri();
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) return false;
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

    private MessageSelection resolveMessageSelection(Map<String, Object> operation,
                                                      Map<String, Object> channel,
                                                      String channelKey,
                                                      Map<String, Object> componentMessages) {
        Map<String, Object> channelMessages = map(channel.get("messages"));
        if (channelMessages.isEmpty()) return null;
        Object operationMessages = operation.get("messages");
        if (operationMessages instanceof Collection<?> collection) {
            for (Object messageValue : collection) {
                for (Map.Entry<String, Object> channelMessage : channelMessages.entrySet()) {
                    if (operationMessageMatchesChannelMessage(
                            messageValue, channelKey, channelMessage.getKey(), channelMessage.getValue())) {
                        return MessageSelection.channel(
                                channelKey, channelMessage.getKey(),
                                resolveChannelMessage(channelMessage.getValue(), componentMessages));
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

    private Map<String, Object> resolveChannelMessage(Object channelMessageValue,
                                                      Map<String, Object> componentMessages) {
        Map<String, Object> channelMessage = map(channelMessageValue);
        String ref = str(channelMessage, "$ref", null);
        String prefix = "#/components/messages/";
        if (ref != null && ref.startsWith(prefix)) {
            Map<String, Object> componentMessage = map(
                    componentMessages.get(decodeJsonPointerSegment(ref.substring(prefix.length()))));
            if (!componentMessage.isEmpty()) return componentMessage;
        }
        return channelMessage;
    }

    private boolean operationMessageMatchesChannelMessage(Object operationMessageValue,
                                                           String channelKey,
                                                           String channelMessageName,
                                                           Object channelMessageValue) {
        if (operationMessageValue == channelMessageValue) return true;
        String operationRef = str(map(operationMessageValue), "$ref", null);
        if (operationRef == null) return false;
        String channelRef = str(map(channelMessageValue), "$ref", null);
        if (operationRef.equals(channelRef)) return true;
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
        List<T> list = (List<T>) map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!list.contains(value)) list.add(value);
    }

    @SuppressWarnings("unchecked")
    private void addMessageById(Map<String, Object> map, String key, Map<String, Object> message) {
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) map.computeIfAbsent(key, ignored -> new ArrayList<>());
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
}
