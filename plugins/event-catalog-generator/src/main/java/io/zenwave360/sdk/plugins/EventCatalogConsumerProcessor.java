package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ApiConsumptionMatch;
import io.zenwave360.manifest.ApiServiceConsumption;
import io.zenwave360.manifest.AsyncApiAction;
import io.zenwave360.manifest.LegacyClientMatch;
import io.zenwave360.manifest.ManifestApiConsumptions;
import io.zenwave360.sdk.processors.Processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Projects typed manifest-core API consumption evidence into the EventCatalog view model. */
public class EventCatalogConsumerProcessor implements Processor {

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        EventCatalogModel eventCatalog = (EventCatalogModel) contextModel.get("eventCatalog");
        ManifestApiConsumptions consumptions =
                (ManifestApiConsumptions) contextModel.get("apiConsumptions");
        if (eventCatalog == null || consumptions == null) return contextModel;

        for (ApiConsumptionMatch match : consumptions.getMatches()) {
            String messageId = eventCatalog.catalogServiceId(match.getEdge().getProviderService())
                    + "." + match.getChannel().getChannelKey();
            addToList(
                    eventCatalog.serviceData(match.getEdge().getConsumerService()),
                    match.getConsumerAction() == AsyncApiAction.SEND ? "_sends" : "_receives",
                    messageId);

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("messageId", messageId);
            evidence.put("channelId", messageId);
            evidence.put("consumerServiceId", eventCatalog.catalogServiceId(match.getEdge().getConsumerService()));
            evidence.put("consumerServiceRef", match.getEdge().getConsumerService().getServiceRef());
            evidence.put("consumerArtifactId", match.getEdge().getConsumerArtifact().getArtifactId());
            evidence.put("operationId", match.getConsumerOperationId());
            evidence.put("action", serialized(match.getConsumerAction()));
            evidence.put("providerOperationId", match.getProviderOperationId());
            evidence.put("providerAction", serialized(match.getProviderAction()));
            evidence.put("channelKey", match.getChannel().getChannelKey());
            evidence.put("matchKind", match.getMatchKind().name().toLowerCase(Locale.ROOT).replace('_', '-'));
            addToList(eventCatalog.serviceData(match.getEdge().getProviderService()), "_consumptions", evidence);
        }

        for (ApiServiceConsumption consumption : consumptions.getApiConsumptions()) {
            addToList(
                    eventCatalog.serviceData(consumption.getEdge().getProviderService()),
                    "_apiConsumers",
                    eventCatalog.catalogServiceId(consumption.getEdge().getConsumerService()));
        }

        for (LegacyClientMatch match : consumptions.getLegacyMatches()) {
            String messageId = eventCatalog.catalogServiceId(match.getProviderService())
                    + "." + match.getChannel().getChannelKey();
            addToList(
                    eventCatalog.serviceData(match.getConsumerService()),
                    match.getConsumerAction() == AsyncApiAction.SEND ? "_sends" : "_receives",
                    messageId);
        }
        return contextModel;
    }

    private String serialized(AsyncApiAction action) {
        return action.name().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private <T> void addToList(Map<String, Object> map, String key, T value) {
        List<T> list = (List<T>) map.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!list.contains(value)) list.add(value);
    }
}
