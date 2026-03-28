package io.zenwave360.sdk.plugins.support;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.sdk.utils.NamingUtils.asJavaTypeName;

public final class ServiceEventPlanner {

    private ServiceEventPlanner() {
    }

    public static boolean needsEventsProducer(Map<String, Object> zdl, Map<String, Object> service) {
        var methods = service != null
                ? JSONPath.get(service, "methods[*]", List.<Map<String, Object>>of())
                : ZDLFindUtils.methodsWithEvents(zdl);
        var eventNamesExpr = methods.stream()
                .map(ZDLFindUtils::methodEventsFlatList)
                .flatMap(List::stream)
                .collect(Collectors.joining("|"));
        var externalEvents = (List<?>) JSONPath.get(zdl, "$.events[*][?(@.name =~ /(" + eventNamesExpr + ")/)].options.asyncapi");
        return externalEvents != null && !externalEvents.isEmpty();
    }

    public static boolean needsEventBus(Map<String, Object> zdl, Map<String, Object> service) {
        var methods = service != null
                ? JSONPath.get(service, "methods[*]", List.<Map<String, Object>>of())
                : ZDLFindUtils.methodsWithEvents(zdl);
        var eventNamesExpr = methods.stream()
                .map(ZDLFindUtils::methodEventsFlatList)
                .flatMap(List::stream)
                .collect(Collectors.joining("|"));
        var domainEvents = JSONPath.get(zdl, "$.events[*][?(@.name =~ /(" + eventNamesExpr + ")/)]", List.<Map<String, Object>>of()).stream()
                .filter(event -> JSONPath.get(event, "options.asyncapi") == null)
                .collect(Collectors.toSet());
        return !domainEvents.isEmpty();
    }

    public static List<Map<String, Object>> serviceMethodEventPublications(Map<String, Object> zdl,
                                                                           Map<String, Object> method) {
        var serviceEventNames = ZDLFindUtils.methodEventsFlatList(method);
        if (serviceEventNames.isEmpty()) {
            return Collections.emptyList();
        }

        var aggregateCommandsForMethod = ZDLFindUtils.findAggregateCommandsForMethod(zdl, method);
        var aggregateProducedEvents = new HashSet<String>();
        for (var aggCmd : aggregateCommandsForMethod) {
            var command = (Map<String, Object>) aggCmd.get("command");
            if (command != null) {
                aggregateProducedEvents.addAll(ZDLFindUtils.methodEventsFlatList(command));
            }
        }

        var result = new ArrayList<Map<String, Object>>();
        for (String eventName : serviceEventNames) {
            var event = (Map<String, Object>) JSONPath.get(zdl, "$.events." + eventName);
            boolean producedByAggregate = aggregateProducedEvents.contains(eventName);
            boolean isAsyncApi = event != null && JSONPath.get(event, "options.asyncapi") != null;
            String producerMethod = "on" + asJavaTypeName(eventName);

            int producerCount = 0;
            for (var aggCmd : aggregateCommandsForMethod) {
                var command = (Map<String, Object>) aggCmd.get("command");
                if (command != null && ZDLFindUtils.methodEventsFlatList(command).contains(eventName)) {
                    producerCount++;
                }
            }

            var entry = new LinkedHashMap<String, Object>();
            entry.put("eventName", eventName);
            entry.put("eventClassName", event != null ? event.get("className") : eventName);
            entry.put("instanceName", NamingUtils.asInstanceName(eventName));
            entry.put("producerMethod", producerMethod);
            entry.put("producedByAggregate", producedByAggregate);
            entry.put("isAsyncApi", isAsyncApi);
            entry.put("multipleProducers", producerCount > 1);
            result.add(entry);
        }
        return result;
    }
}
