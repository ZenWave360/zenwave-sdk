package io.zenwave360.sdk.lint;

import java.util.Map;

import io.zenwave360.sdk.lint.LintDiagnostic.Severity;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLListenerUtils;

/**
 * Checks {@code @listener} bindings with the same rules generators apply ({@link ZDLListenerUtils}), plus:
 * <ul>
 * <li>{@code zdl.mixed-event-binding}: the consumed event is also declared with {@code @asyncapi};</li>
 * <li>{@code zdl.data-lineage}: a required field of the listener's input is missing from the event.</li>
 * </ul>
 */
public class ZdlListenerLinter implements Linter {

    @Override
    public void lint(LintContext context) {
        Map<String, Object> model = context.zdlModel();
        Map<String, Map<String, Object>> services = JSONPath.get(model, "$.services", Map.of());
        for (Map.Entry<String, Map<String, Object>> service : services.entrySet()) {
            Map<String, Map<String, Object>> methods = JSONPath.get(service.getValue(), "$.methods", Map.of());
            for (Map.Entry<String, Map<String, Object>> method : methods.entrySet()) {
                String methodLabel = service.getKey() + "." + method.getKey();
                String path = "services." + service.getKey() + ".methods." + method.getKey() + ".options.listener";
                for (Object occurrence : ZDLListenerUtils.rawListenerOccurrences(method.getValue())) {
                    if (referencesUnloadedApi(model, occurrence)) {
                        continue; // reported by ZdlReferencesLinter
                    }
                    Map<String, Object> event;
                    try {
                        event = ZDLListenerUtils.resolveListenerEvent(model, methodLabel, occurrence);
                    } catch (IllegalArgumentException e) {
                        context.report(Severity.error, "zdl.listener-event", e.getMessage(), path);
                        continue;
                    }
                    String eventName = (String) ((Map<?, ?>) occurrence).get("event");
                    if (JSONPath.get(event, "$.options.asyncapi") != null) {
                        context.report(Severity.warning, "zdl.mixed-event-binding", "Event '" + eventName
                                + "' is declared with @asyncapi and consumed with @listener", path);
                    }
                    checkLineage(context, method.getValue(), event, eventName, path);
                }
            }
        }
    }

    private boolean referencesUnloadedApi(Map<String, Object> model, Object occurrence) {
        if (occurrence instanceof Map<?, ?> map && map.get("zdl") instanceof String apiName) {
            Map<String, Object> api = JSONPath.get(model, "$.apis['" + apiName.trim() + "']");
            return api != null && api.get(ZDLParser.REFERENCED_API_ERROR_PROPERTY) != null;
        }
        return false;
    }

    private void checkLineage(LintContext context, Map<String, Object> method, Map<String, Object> event,
            String eventName, String listenerPath) {
        if (!(method.get("parameter") instanceof String inputName) || inputName.isBlank()) {
            return;
        }
        Map<String, Object> input = JSONPath.get(context.zdlModel(), "$.inputs['" + inputName + "']");
        if (input == null) {
            return;
        }
        Map<String, Map<String, Object>> inputFields = JSONPath.get(input, "$.fields", Map.of());
        Map<String, Object> eventFields = JSONPath.get(event, "$.fields", Map.of());
        for (Map.Entry<String, Map<String, Object>> field : inputFields.entrySet()) {
            if (JSONPath.get(field.getValue(), "$.validations.required") != null
                    && !eventFields.containsKey(field.getKey())) {
                context.report(Severity.warning, "zdl.data-lineage", "Required input field '" + inputName + "."
                        + field.getKey() + "' is missing from triggering event '" + eventName + "'", listenerPath);
            }
        }
    }
}
