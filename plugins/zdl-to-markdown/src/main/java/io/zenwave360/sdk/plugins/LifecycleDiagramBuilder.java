package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class LifecycleDiagramBuilder {

    private static final String DEFAULT_STATE_MACHINE_SKINPARAMS = String.join("\n",
            "hide empty description",
            "skinparam shadowing false",
            "skinparam BackgroundColor white",
            "skinparam ArrowColor #4B5563",
            "skinparam StateBorderColor #1F2937",
            "skinparam StateFontColor #111827",
            "skinparam StateBackgroundColor #F9FAFB",
            "skinparam StateStartColor #111827",
            "skinparam StateEndColor #111827",
            "skinparam NoteBackgroundColor #FFFBEA",
            "skinparam NoteBorderColor #D6B656",
            "skinparam LegendBackgroundColor #F8FAFC",
            "skinparam LegendBorderColor #CBD5E1",
            "skinparam roundcorner 12"
    );

    private LifecycleDiagramBuilder() {
    }

    static String buildAggregateLifecyclePlantUml(Map<String, Object> aggregate, Map<String, Object> rootEntity) {
        if (aggregate == null) {
            return null;
        }
        var lifecycle = lifecycleOf(aggregate);
        if (lifecycle == null) {
            lifecycle = lifecycleOf(rootEntity);
        }
        if (lifecycle == null) {
            return null;
        }

        var commands = commandsOf(aggregate);
        if (commands.isEmpty() && rootEntity != null) {
            commands = commandsOf(rootEntity);
        }

        record Transition(String from, String to, String label) {}
        var transitions = new ArrayList<Transition>();
        for (var cmd : commands) {
            var fromStates = normalizeToStringList(transitionFrom(cmd));
            String to = transitionTo(cmd) != null ? transitionTo(cmd).toString() : null;
            if (to == null || fromStates.isEmpty()) {
                continue;
            }
            String label = transitionLabel(cmd);
            for (String from : fromStates) {
                transitions.add(new Transition(from, to, label));
            }
        }
        if (transitions.isEmpty()) {
            return null;
        }

        String aggregateName = JSONPath.get(aggregate, "$.name", "Aggregate");
        String aggregateRoot = JSONPath.get(aggregate, "$.aggregateRoot", JSONPath.get(rootEntity, "$.name", ""));
        String lifecycleField = JSONPath.get(lifecycle, "$.field", "status");
        String initialState = lifecycleInitialState(lifecycle);

        var allStates = new LinkedHashSet<String>();
        if (initialState != null) {
            allStates.add(initialState);
        }
        transitions.forEach(t -> {
            allStates.add(t.from);
            allStates.add(t.to);
        });

        var outgoing = transitions.stream().map(t -> t.from).collect(Collectors.toSet());
        var terminalStates = allStates.stream().filter(s -> !outgoing.contains(s)).collect(Collectors.toSet());

        var sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("title ").append(aggregateName).append(" lifecycle\n\n");
        sb.append(DEFAULT_STATE_MACHINE_SKINPARAMS).append("\n\n");

        for (String state : allStates) {
            String id = sanitizeStateId(state);
            sb.append("state \"").append(state).append("\" as ").append(id);
            if (terminalStates.contains(state)) {
                sb.append(" <<terminal>>");
            }
            sb.append("\n");
        }
        sb.append("\n");

        if (initialState != null) {
            sb.append("[*] --> ").append(sanitizeStateId(initialState)).append(" : initialState\n\n");
        }

        for (var t : transitions) {
            sb.append(sanitizeStateId(t.from)).append(" --> ").append(sanitizeStateId(t.to))
                    .append(" : ").append(t.label).append("\n");
        }

        if (initialState != null) {
            sb.append("\n");
            sb.append("note right of ").append(sanitizeStateId(initialState)).append("\n");
            sb.append("Aggregate root: ").append(aggregateRoot).append("\n");
            sb.append("Status field: ").append(lifecycleField).append("\n");
            sb.append("Initial state: ").append(initialState).append("\n");
            sb.append("end note\n\n");
        } else {
            sb.append("\n");
        }

        sb.append("legend right\n");
        sb.append("  <b>Aggregate:</b> ").append(aggregateName).append("\n");
        if (aggregateRoot != null && !aggregateRoot.isBlank()) {
            sb.append("  <b>Root:</b> ").append(aggregateRoot).append("\n");
        }
        sb.append("  <b>Label format:</b>\n");
        sb.append("  command(parameter) / emitted events\n");
        sb.append("endlegend\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    static String buildEntityServiceLifecyclePlantUml(Map<String, Object> service, Map<String, Object> entity) {
        if (service == null || entity == null) {
            return null;
        }
        var lifecycle = lifecycleOf(entity);
        if (lifecycle == null) {
            return null;
        }

        String entityName = JSONPath.get(entity, "$.name", (String) null);
        if (entityName == null) {
            return null;
        }

        Map<String, Object> methodsMap = JSONPath.get(service, "$.methods", Map.of());

        record Transition(String from, String to, String label) {}
        var transitions = new ArrayList<Transition>();
        for (Object value : methodsMap.values()) {
            if (!(value instanceof Map)) {
                continue;
            }
            var method = (Map<String, Object>) value;
            String methodEntity = JSONPath.get(method, "$.entity", (String) null);
            if (!entityName.equals(methodEntity)) {
                continue;
            }
            var fromStates = normalizeToStringList(transitionFrom(method));
            String to = transitionTo(method) != null ? transitionTo(method).toString() : null;
            if (to == null || fromStates.isEmpty()) {
                continue;
            }
            String label = transitionLabel(method);
            for (String from : fromStates) {
                transitions.add(new Transition(from, to, label));
            }
        }

        if (transitions.isEmpty()) {
            return null;
        }

        String serviceName = JSONPath.get(service, "$.name", "Service");
        String lifecycleField = JSONPath.get(lifecycle, "$.field", "status");
        String initialState = lifecycleInitialState(lifecycle);

        var allStates = new LinkedHashSet<String>();
        if (initialState != null) {
            allStates.add(initialState);
        }
        transitions.forEach(t -> {
            allStates.add(t.from);
            allStates.add(t.to);
        });

        var outgoing = transitions.stream().map(t -> t.from).collect(Collectors.toSet());
        var terminalStates = allStates.stream().filter(s -> !outgoing.contains(s)).collect(Collectors.toSet());

        var sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("title ").append(entityName).append(" lifecycle (via ").append(serviceName).append(")\n\n");
        sb.append(DEFAULT_STATE_MACHINE_SKINPARAMS).append("\n\n");

        for (String state : allStates) {
            String id = sanitizeStateId(state);
            sb.append("state \"").append(state).append("\" as ").append(id);
            if (terminalStates.contains(state)) {
                sb.append(" <<terminal>>");
            }
            sb.append("\n");
        }
        sb.append("\n");

        if (initialState != null) {
            sb.append("[*] --> ").append(sanitizeStateId(initialState)).append(" : initialState\n\n");
        }

        for (var t : transitions) {
            sb.append(sanitizeStateId(t.from)).append(" --> ").append(sanitizeStateId(t.to))
                    .append(" : ").append(t.label).append("\n");
        }

        if (initialState != null) {
            sb.append("\n");
            sb.append("note right of ").append(sanitizeStateId(initialState)).append("\n");
            sb.append("Entity: ").append(entityName).append("\n");
            sb.append("Service: ").append(serviceName).append("\n");
            sb.append("Status field: ").append(lifecycleField).append("\n");
            sb.append("Initial state: ").append(initialState).append("\n");
            sb.append("end note\n\n");
        } else {
            sb.append("\n");
        }

        sb.append("legend right\n");
        sb.append("  <b>Entity:</b> ").append(entityName).append("\n");
        sb.append("  <b>Service:</b> ").append(serviceName).append("\n");
        sb.append("  <b>Label format:</b>\n");
        sb.append("  method(params) / emitted events\n");
        sb.append("endlegend\n");
        sb.append("@enduml\n");
        return sb.toString();
    }

    static String sanitizeStateId(String state) {
        if (state == null) {
            return null;
        }
        String id = state.replaceAll("[^A-Za-z0-9_]", "_");
        if (id.isEmpty()) {
            return "STATE";
        }
        if (!Character.isLetter(id.charAt(0)) && id.charAt(0) != '_') {
            id = "S_" + id;
        }
        return id;
    }

    static String transitionLabel(Map<String, Object> methodOrCommand) {
        String name = JSONPath.get(methodOrCommand, "$.name", "");
        String params = methodParamLabel(methodOrCommand);
        String signature = params.isEmpty() ? name + "()" : name + "(" + params + ")";

        List<Object> events = JSONPath.get(methodOrCommand, "$.withEvents", List.of());
        var eventNames = new ArrayList<String>();
        for (Object event : events) {
            if (event == null) {
                continue;
            }
            if (event instanceof List list) {
                for (Object inner : list) {
                    if (inner != null) {
                        eventNames.add(inner.toString());
                    }
                }
            } else {
                eventNames.add(event.toString());
            }
        }
        if (!eventNames.isEmpty()) {
            return signature + "\\n/ " + StringUtils.join(eventNames, ", ");
        }
        return signature;
    }

    static Map<String, Object> lifecycleOf(Map<String, Object> model) {
        if (model == null) {
            return null;
        }
        Map<String, Object> lifecycle = (Map<String, Object>) JSONPath.get(model, "$.lifecycle");
        if (lifecycle == null) {
            lifecycle = (Map<String, Object>) JSONPath.get(model, "$.options.lifecycle");
        }
        return lifecycle;
    }

    static List<Map<String, Object>> commandsOf(Map<String, Object> aggregateOrEntity) {
        if (aggregateOrEntity == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) JSONPath.get(aggregateOrEntity, "$.commands[*]", List.<Map<String, Object>>of());
    }

    static Object transitionFrom(Map<String, Object> methodOrCommand) {
        if (methodOrCommand == null) {
            return null;
        }
        Object from = JSONPath.get(methodOrCommand, "$.transition.from");
        if (from == null) {
            from = JSONPath.get(methodOrCommand, "$.from");
        }
        return from;
    }

    static Object transitionTo(Map<String, Object> methodOrCommand) {
        if (methodOrCommand == null) {
            return null;
        }
        Object to = JSONPath.get(methodOrCommand, "$.transition.to");
        if (to == null) {
            to = JSONPath.get(methodOrCommand, "$.to");
        }
        return to;
    }

    static List<String> normalizeToStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List list) {
            return (List<String>) list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    static String methodParamLabel(Map<String, Object> methodOrCommand) {
        var params = new ArrayList<String>();
        if (methodOrCommand == null) {
            return "";
        }
        if (JSONPath.get(methodOrCommand, "$.paramId") != null) {
            params.add("id");
        }
        if (JSONPath.get(methodOrCommand, "$.parameter") != null) {
            params.add("input");
        }
        return StringUtils.join(params, ", ");
    }

    static String lifecycleInitialState(Map<String, Object> lifecycle) {
        if (lifecycle == null) {
            return null;
        }
        String initial = JSONPath.get(lifecycle, "$.initial", (String) null);
        if (initial == null) {
            initial = JSONPath.get(lifecycle, "$.initialState", (String) null);
        }
        return initial;
    }
}
