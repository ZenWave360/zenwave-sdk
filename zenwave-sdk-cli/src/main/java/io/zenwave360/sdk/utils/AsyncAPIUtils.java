package io.zenwave360.sdk.utils;

import io.zenwave360.sdk.processors.AsyncApiProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class AsyncAPIUtils {

    public static boolean isV2(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("2.");
    }

    public static boolean isV3(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("3.");
    }

    /**
     * Standard AsyncAPI v2/v3 messages for a single operation, independent of trait composition (call
     * this after traits have been applied if trait-composed headers/payloads are needed): v2 is
     * {@code channel.publish|subscribe.message} (expanding {@code oneOf}); v3 is
     * {@code operations[operationId].messages}, falling back to the referenced channel's
     * {@code messages} when the operation doesn't declare its own.
     */
    public static List<Map<String, Object>> operationMessages(Map apiModel, String operationId) {
        if (operationId == null) {
            return List.of();
        }
        if (isV2(apiModel)) {
            for (Map<String, Object> operation : v2Operations(apiModel)) {
                if (operationId.equals(operation.get("operationId"))) {
                    return v2OperationMessages(operation);
                }
            }
            return List.of();
        }
        if (isV3(apiModel)) {
            Map<String, Object> operation = JSONPath.get(apiModel, "$.operations['" + operationId + "']");
            return v3OperationMessages(operation);
        }
        return List.of();
    }

    /**
     * Standard AsyncAPI v2/v3 messages declared on a channel, independent of trait composition: v2 is
     * the (identity-deduplicated) union of its {@code publish}/{@code subscribe} messages; v3 is
     * {@code channel.messages}.
     */
    public static List<Map<String, Object>> channelMessages(Map apiModel, String channelId) {
        if (channelId == null) {
            return List.of();
        }
        Map<String, Object> channel = JSONPath.get(apiModel, "$.channels['" + channelId + "']");
        if (channel == null) {
            return List.of();
        }
        if (isV2(apiModel)) {
            Set<Map<String, Object>> messages = Collections.newSetFromMap(new IdentityHashMap<>());
            messages.addAll(v2OperationMessages((Map<String, Object>) channel.get("publish")));
            messages.addAll(v2OperationMessages((Map<String, Object>) channel.get("subscribe")));
            return new ArrayList<>(messages);
        }
        // v3 channel.messages is a Map keyed by message name (unlike operation.messages, which is a List)
        return JSONPath.get(channel, "$.messages[*]", Collections.emptyList());
    }

    /** Every declared operationId in the document, in document order. */
    public static List<String> allOperationIds(Map apiModel) {
        if (isV2(apiModel)) {
            return v2Operations(apiModel).stream()
                    .map(operation -> (String) operation.get("operationId"))
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (isV3(apiModel)) {
            Map<String, Object> operations = JSONPath.get(apiModel, "$.operations", Collections.emptyMap());
            return new ArrayList<>(operations.keySet());
        }
        return List.of();
    }

    private static List<Map<String, Object>> v2Operations(Map apiModel) {
        List<Map<String, Object>> operations = new ArrayList<>();
        operations.addAll(JSONPath.get(apiModel, "$.channels[*].publish", Collections.emptyList()));
        operations.addAll(JSONPath.get(apiModel, "$.channels[*].subscribe", Collections.emptyList()));
        return operations;
    }

    private static List<Map<String, Object>> v2OperationMessages(Map<String, Object> operation) {
        if (operation == null) {
            return List.of();
        }
        Map<String, Object> message = (Map<String, Object>) operation.get("message");
        if (message == null) {
            return List.of();
        }
        List<Map<String, Object>> oneOf = (List<Map<String, Object>>) message.get("oneOf");
        return oneOf != null ? oneOf : List.of(message);
    }

    private static List<Map<String, Object>> v3OperationMessages(Map<String, Object> operation) {
        if (operation == null) {
            return List.of();
        }
        List<Map<String, Object>> messages = (List<Map<String, Object>>) operation.get("messages");
        if (messages != null) {
            return messages;
        }
        Map<String, Object> channel = (Map<String, Object>) operation.get("channel");
        return channel != null ? JSONPath.get(channel, "$.messages[*]", Collections.emptyList()) : List.of();
    }

    public static List<Map<String, Object>> extractMessages(Map apiModel, Function<AsyncApiProcessor.SchemaFormatType, Boolean> isFormat, List<String> operationIds, List<String> messageNames) {
        List<Map<String, Object>> allMessages = new ArrayList<>();
        if (!messageNames.isEmpty()) {
            Map<String, Map<String, Object>> components = JSONPath.get(apiModel, "$.components.messages", Collections.emptyMap());
            components.forEach((name, message) -> {
                if (messageNames.contains(name) || messageNames.contains(message.get("name"))) {
                    allMessages.add(message);
                }
            });
        } else {
            List<String> selectedOperationIds = operationIds.isEmpty() ? allOperationIds(apiModel) : operationIds;
            Set<Map<String, Object>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (String operationId : selectedOperationIds) {
                operationMessages(apiModel, operationId).stream().filter(seen::add).forEach(allMessages::add);
            }
        }

        var schemaFormatPath = AsyncAPIUtils.isV3(apiModel) ? "$.payload.schemaFormat" : "$.schemaFormat";
        return allMessages.stream().filter(message -> {
            var schemaFormat = (String) JSONPath.get(message, schemaFormatPath);
            var schemaFormatType = AsyncApiProcessor.SchemaFormatType.getFormat(schemaFormat);
            return isFormat.apply(schemaFormatType); // leave out json-schema or avro
        }).toList();
    }
}
