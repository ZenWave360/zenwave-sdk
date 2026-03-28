package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class ServiceHelperFormatter {

    private ServiceHelperFormatter() {
    }

    static String methodParamsSignature(Map<String, Object> method) {
        var params = new ArrayList<String>();
        if (JSONPath.get(method, "paramId") != null) {
            params.add("id");
        }
        if (JSONPath.get(method, "parameter") != null) {
            params.add(JSONPath.get(method, "parameter"));
        }
        return StringUtils.join(params, ", ");
    }

    static String methodReturnType(Map<String, Object> method) {
        var returnType = JSONPath.get(method, "returnType", "");
        if (JSONPath.get(method, "returnTypeIsArray", false)) {
            returnType = returnType + "[]";
        }
        if (JSONPath.get(method, "returnTypeIsOptional", false)) {
            returnType = returnType + "?";
        }
        return returnType;
    }

    static String methodEvents(Map<String, Object> method) {
        var events = JSONPath.get(method, "withEvents", List.of());
        return StringUtils.join(events, " ").replaceAll(", ", " | ");
    }

    static List<String> formatMethodAnnotations(Map<String, Object> method) {
        if (method == null) {
            return List.of();
        }
        var annotations = new ArrayList<String>();
        Map<String, Object> options = JSONPath.get(method, "$.options", Map.of());
        for (var entry : options.entrySet()) {
            annotations.add(formatAnnotation(entry.getKey(), entry.getValue()));
        }
        return annotations;
    }

    static List<Map<String, Object>> serviceInputs(Map<String, Object> service, Object zdlModel) {
        var inputs = new LinkedHashSet<>();
        var methods = JSONPath.get(service, "methods", Map.of());
        for (Object value : methods.values()) {
            var method = (Map) value;
            if (JSONPath.get(method, "parameter") != null) {
                inputs.add(JSONPath.get(method, "parameter"));
            }
        }
        return inputs.stream().map(input -> {
            var entity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + input);
            if (entity != null && !"entities".equals(JSONPath.get(entity, "type"))) {
                return entity;
            }
            return null;
        }).filter(Objects::nonNull).map(entity -> (Map<String, Object>) entity).collect(Collectors.toList());
    }

    static List<Map<String, Object>> serviceOutputs(Map<String, Object> service, Object zdlModel) {
        var outputs = new LinkedHashSet<>();
        var methods = JSONPath.get(service, "methods", Map.of());
        for (Object method : methods.values()) {
            outputs.add(JSONPath.get(method, "returnType"));
        }
        return outputs.stream().map(output -> {
            var entity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + output);
            if (entity != null && !"entities".equals(JSONPath.get(entity, "type"))) {
                return entity;
            }
            return null;
        }).filter(Objects::nonNull).map(entity -> (Map<String, Object>) entity).collect(Collectors.toList());
    }

    static List<Map<String, Object>> serviceEvents(Map<String, Object> service, Object zdlModel) {
        var events = new LinkedHashSet<>();
        var methods = JSONPath.get(service, "methods", Map.of());
        for (Object method : methods.values()) {
            var methodEvents = JSONPath.get(method, "withEvents", List.of());
            for (Object methodEvent : methodEvents) {
                if (methodEvent instanceof Collection<?> collection) {
                    events.addAll(collection);
                } else {
                    events.add(methodEvent);
                }
            }
        }
        return events.stream()
                .map(event -> JSONPath.get(zdlModel, "$.events." + event))
                .filter(Objects::nonNull)
                .map(event -> (Map<String, Object>) event)
                .collect(Collectors.toList());
    }

    private static String formatAnnotation(String name, Object value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            return "@" + name + "()";
        }
        if (value instanceof String stringValue) {
            return "@" + name + "(\"" + stringValue + "\")";
        }
        if (value instanceof Collection<?> collectionValue) {
            return "@" + name + "(" + collectionValue.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")";
        }
        if (value instanceof Map<?, ?> mapValue) {
            return "@" + name + "(" + mapValue.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + formatAnnotationValue(entry.getValue()))
                    .collect(Collectors.joining(", ")) + ")";
        }
        return "@" + name + "(" + value + ")";
    }

    private static String formatAnnotationValue(Object value) {
        if (value instanceof Collection<?> collectionValue) {
            return "[" + collectionValue.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
        }
        if (value instanceof Map<?, ?> mapValue) {
            return "{" + mapValue.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + formatAnnotationValue(entry.getValue()))
                    .collect(Collectors.joining(", ")) + "}";
        }
        return String.valueOf(value);
    }
}
