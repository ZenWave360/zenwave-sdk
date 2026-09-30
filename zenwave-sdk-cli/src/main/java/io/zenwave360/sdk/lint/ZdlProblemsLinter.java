package io.zenwave360.sdk.lint;

import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.lint.LintDiagnostic.Severity;
import io.zenwave360.sdk.utils.JSONPath;

/**
 * Reports the problems found by the ZDL parser: syntax errors, and semantic errors such as unknown types, events,
 * inputs, outputs, aggregates, and invalid {@code @lifecycle} / {@code @transition} states.
 */
public class ZdlProblemsLinter implements Linter {

    @Override
    public boolean requiresValidSyntax() {
        return false;
    }

    @Override
    public void lint(LintContext context) {
        List<Map<String, Object>> problems = JSONPath.get(context.zdlModel(), "$.problems[*]", List.of());
        for (Map<String, Object> problem : problems) {
            String path = problem.get("path") instanceof String value ? value : null;
            String message = String.valueOf(problem.getOrDefault("message", "Unknown problem"));
            String ruleId = ruleId(problem.get("code"), path, message);
            // some semantic problems carry an empty location (line 0): locate them by model path instead
            if (problem.get("location") instanceof int[] location && location.length > 2 && location[2] > 0) {
                context.report(Severity.error, ruleId, message, path, location);
            } else {
                context.report(Severity.error, ruleId, message, path);
            }
        }
    }

    static String ruleId(Object code, String path, String message) {
        if (code instanceof String value && !value.isBlank()) return "zdl." + value;
        if ("syntax".equals(path)) return "zdl.syntax";
        if ("parser".equals(path)) return "zdl.parser";
        // TODO derive from a stable problem code once the ZDL parser emits one; messages are not a contract
        if (message.contains("not a valid state value")) return "zdl.transition-state";
        if (path != null && path.contains("lifecycle.initial")) return "zdl.lifecycle-initial";
        if (path != null && path.contains("lifecycle")) return "zdl.lifecycle";
        if (message.contains("not a valid type")) return "zdl.unknown-type";
        if (message.contains("not an event")) return "zdl.unknown-event";
        if (path != null && path.endsWith(".parameter")) return "zdl.unknown-input";
        if (path != null && path.endsWith(".returnType")) return "zdl.unknown-output";
        if (message.contains("not an aggregate")) return "zdl.unknown-aggregate";
        return "zdl.semantic";
    }
}
