package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.manifest.BlockingZenWaveManifestLoader;
import io.zenwave360.manifest.ManifestConsumptionEdge;
import io.zenwave360.manifest.ManifestConsumerIndex;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ResolvedManifestArtifact;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves manifest-declared consumer artifacts and verifies operation-level AsyncAPI usage.
 * Channel matches require an external provider reference or an unambiguous inline address; a local
 * channel-key coincidence alone is not consumption evidence.
 */
public class EventCatalogConsumerProcessor implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @DocumentedOption(description = "Preferred artifact source for build-time content loading.")
    public String preferredSource;
    @DocumentedOption(description = "Allow source fallback for build-time content loading.")
    public Boolean allowFallback;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        ZenWaveManifest manifest = (ZenWaveManifest) contextModel.get("manifest");
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        BlockingZenWaveManifestLoader manifestRuntime =
                (BlockingZenWaveManifestLoader) contextModel.get("manifestRuntime");
        if (manifest == null || eventCatalog == null || manifestRuntime == null) return contextModel;

        ManifestConsumerIndex index = ManifestConsumerIndex.build(manifest, manifestRuntime.getDelegate());
        index.getDiagnostics().forEach(diagnostic -> log.warn(
                "Manifest consumer diagnostic [{}] at {}: {}",
                diagnostic.getCode(), diagnostic.getLocation(), diagnostic.getMessage()));

        ManifestLoadOptions options = new ManifestLoadOptions()
                .withPreferredSource(preferredSource)
                .withFallback(allowFallback == null || allowFallback);
        Map<String, Map<String, Object>> parsedConsumerArtifacts = new LinkedHashMap<>();

        for (ManifestConsumptionEdge edge : index.getEdges()) {
            String consumerType = edge.getConsumerArtifact().getArtifact().getType();
            if ("openapi".equals(consumerType)) {
                if (edge.getProviderArtifacts().stream()
                        .anyMatch(artifact -> "openapi".equals(artifact.getArtifact().getType()))) {
                    addToList(eventCatalog.serviceData(edge.getProviderService()), "_apiConsumers",
                            eventCatalog.catalogServiceId(edge.getConsumerService()));
                }
                continue;
            }
            if (!"asyncapi-client".equals(consumerType)) continue;
            Map<String, Object> clientModel = loadConsumerArtifact(
                    manifestRuntime, manifest, edge, options, parsedConsumerArtifacts);
            if (clientModel == null) continue;
            for (ResolvedManifestArtifact providerArtifact : edge.getProviderArtifacts()) {
                if ("asyncapi".equals(providerArtifact.getArtifact().getType())) {
                    matchAsyncApiOperations(eventCatalog, edge, providerArtifact, clientModel);
                }
            }
        }
        return contextModel;
    }

    private Map<String, Object> loadConsumerArtifact(
            BlockingZenWaveManifestLoader manifestRuntime,
            ZenWaveManifest manifest,
            ManifestConsumptionEdge edge,
            ManifestLoadOptions options,
            Map<String, Map<String, Object>> cache) {
        String cacheKey = edge.getConsumerArtifact().getOwnerRef() + "#" + edge.getConsumerArtifact().getArtifactId();
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
        try {
            String text = manifestRuntime.loadArtifactText(
                    manifest, edge.getConsumerService(), edge.getConsumerArtifact().getArtifact(), options);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = yamlMapper.readValue(text, Map.class);
            cache.put(cacheKey, parsed);
            return parsed;
        } catch (Exception exception) {
            log.warn("Consumer artifact {} could not be loaded for {}: {}",
                    edge.getConsumerArtifact().getArtifactId(), edge.getConsumerService().getServiceRef(),
                    exception.getMessage());
            return null;
        }
    }

    private void matchAsyncApiOperations(
            EventCatalogModel eventCatalog,
            ManifestConsumptionEdge edge,
            ResolvedManifestArtifact providerArtifact,
            Map<String, Object> clientModel) {
        Map<String, Object> runtimeData = eventCatalog.artifactRuntimeData(
                edge.getProviderService(), providerArtifact.getArtifact());
        Map<String, Object> providerChannels = map(runtimeData.get("channelIndex"));
        if (providerChannels.isEmpty()) return;

        Map<String, Object> clientChannels = map(clientModel.get("channels"));
        Map<String, Object> clientOperations = map(clientModel.get("operations"));
        for (Map.Entry<String, Object> operationEntry : clientOperations.entrySet()) {
            Map<String, Object> operation = map(operationEntry.getValue());
            String consumerAction = str(operation, "action", null);
            if (!("send".equals(consumerAction) || "receive".equals(consumerAction))) continue;

            ClientChannel clientChannel = resolveClientChannel(operation.get("channel"), clientChannels);
            Map<String, Object> providerChannel = matchProviderChannel(clientChannel, providerChannels);
            if (providerChannel == null) continue;

            Map<String, Object> providerOperation = complementaryProviderOperation(providerChannel, consumerAction);
            if (providerOperation == null) {
                log.warn("Consumer operation {} action '{}' has no complementary provider operation in {}#{}",
                        operationEntry.getKey(), consumerAction, edge.getProviderService().getId(),
                        providerArtifact.getArtifactId());
                continue;
            }

            String messageId = str(providerChannel, "messageId", null);
            if (messageId == null) continue;
            Map<String, Object> consumerServiceData = eventCatalog.serviceData(edge.getConsumerService());
            addToList(consumerServiceData, "send".equals(consumerAction) ? "_sends" : "_receives", messageId);

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("messageId", messageId);
            evidence.put("channelId", str(providerChannel, "channelId", null));
            evidence.put("consumerServiceId", eventCatalog.catalogServiceId(edge.getConsumerService()));
            evidence.put("consumerServiceRef", edge.getConsumerService().getServiceRef());
            evidence.put("consumerArtifactId", edge.getConsumerArtifact().getArtifactId());
            evidence.put("operationId", operationEntry.getKey());
            evidence.put("action", consumerAction);
            evidence.put("providerOperationId", str(providerOperation, "operationId", null));
            evidence.put("providerAction", str(providerOperation, "action", null));
            evidence.put("channelKey", str(providerChannel, "channelKey", null));
            evidence.put("matchKind", clientChannel.matchKind());
            addToList(eventCatalog.serviceData(edge.getProviderService()), "_consumptions", evidence);
        }
    }

    private ClientChannel resolveClientChannel(Object operationChannel, Map<String, Object> channels) {
        Map<String, Object> channelPointer = map(operationChannel);
        String operationRef = str(channelPointer, "$ref", null);
        String localPrefix = "#/channels/";
        if (operationRef != null && operationRef.startsWith(localPrefix)) {
            String localKey = decodeJsonPointerSegment(operationRef.substring(localPrefix.length()));
            Map<String, Object> declaredChannel = map(channels.get(localKey));
            String externalKey = channelKeyFromExternalRef(str(declaredChannel, "$ref", null));
            return new ClientChannel(localKey, externalKey, str(declaredChannel, "address", null));
        }
        return new ClientChannel(null, null, str(channelPointer, "address", null));
    }

    private Map<String, Object> matchProviderChannel(ClientChannel client, Map<String, Object> providerChannels) {
        if (client.externalKey() != null) {
            Map<String, Object> match = map(providerChannels.get(client.externalKey()));
            if (!match.isEmpty()) return match;
        }
        if (client.address() != null) {
            List<Map<String, Object>> matches = providerChannels.values().stream()
                    .map(this::map)
                    .filter(channel -> client.address().equals(str(channel, "address", null)))
                    .toList();
            if (matches.size() == 1) return matches.get(0);
            if (matches.size() > 1) {
                log.warn("Consumer channel address '{}' is ambiguous in the selected provider artifact", client.address());
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> complementaryProviderOperation(Map<String, Object> providerChannel, String consumerAction) {
        String requiredAction = "send".equals(consumerAction) ? "receive" : "send";
        return listOfMaps(providerChannel.get("operations")).stream()
                .filter(operation -> requiredAction.equals(str(operation, "action", null)))
                .findFirst()
                .orElse(null);
    }

    private String channelKeyFromExternalRef(String reference) {
        if (reference == null) return null;
        String marker = "#/channels/";
        int markerIndex = reference.indexOf(marker);
        return markerIndex < 0 ? null : decodeJsonPointerSegment(reference.substring(markerIndex + marker.length()));
    }

    private String decodeJsonPointerSegment(String value) {
        return value.replace("~1", "/").replace("~0", "~");
    }

    private record ClientChannel(String localKey, String externalKey, String address) {
        String matchKind() {
            if (externalKey != null) return "external-ref";
            return "address";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof Collection<?> collection)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map<?, ?> map) result.add((Map<String, Object>) map);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Map<String, Object> map, String key, T value) {
        List<T> list = (List<T>) map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!list.contains(value)) list.add(value);
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
