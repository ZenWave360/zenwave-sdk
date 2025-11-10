package io.zenwave360.sdk.utils;

import io.zenwave360.sdk.processors.AsyncApiProcessor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class AsyncAPIUtils {

    public static boolean isV2(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("2.");
    }

    public static boolean isV3(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("3.");
    }

    public static List<Map<String, Object>> extractMessages(Map apiModel, Function<AsyncApiProcessor.SchemaFormatType, Boolean> isFormat, List<String> operationIds, List<String> messageNames) {
        List<Map<String, Object>> allMessages = new ArrayList<>();
        if (AsyncAPIUtils.isV2(apiModel)) {
//            String operationIdsRegex = operationIds.isEmpty() ? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
//            List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.channels[*][*][?(@.operationId" + operationIdsRegex + ")]");
//
//            List<Map<String, Object>> messages = JSONPath.get(operations, "$[*].x--messages[*][?(@.name" + operationIdsRegex + ")]", Collections.emptyList());
//            List<Map<String, Object>> oneOfMessages = JSONPath.get(operations, "$[*].x--messages[*].oneOf[?(@.name" + operationIdsRegex + ")]", Collections.emptyList());
//            allMessages.addAll(messages);
//            allMessages.addAll(oneOfMessages);
            if (!messageNames.isEmpty()) {
                String messageNamesRegex = " =~ /(" + StringUtils.join(messageNames, "|") + ")/";
                Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.components.messages[*][?(@.name" + messageNamesRegex + ")]", Collections.emptySet()));
                allMessages.addAll(messages);
            } else {
                if(operationIds.isEmpty()) {
                    Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.channels[*][*].x--messages[*]", Collections.emptySet()));
                    allMessages.addAll(messages);
                } else {
                    for (String operationId : operationIds) {
                        List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.channels[*][*][?(@.operationId == '" + operationId + "')]");
                        Set<Map<String, Object>> messages = JSONPath.get(operations, "$[*].x--messages[*]", Collections.emptySet());
                        allMessages.addAll(messages);
                    }
                }
            }
        }
        if (AsyncAPIUtils.isV3(apiModel)) {
            if (!messageNames.isEmpty()) {
                String messageNamesRegex = " =~ /(" + StringUtils.join(messageNames, "|") + ")/";
                Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.components.messages[*][?(@.name" + messageNamesRegex + ")]", Collections.emptySet()));
                allMessages.addAll(messages);
            } else {
                if(operationIds.isEmpty()) {
                    Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.operations[*].channel.messages[*]", Collections.emptySet()));
                    allMessages.addAll(messages);
                } else {
                    for (String operationId : operationIds) {
                        Set<Map<String, Object>> messages = new HashSet<>(JSONPath.get(apiModel, "$.operations['" + operationId + "'].channel.messages[*]", Collections.emptySet()));
                        allMessages.addAll(messages);
                    }
                }
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
