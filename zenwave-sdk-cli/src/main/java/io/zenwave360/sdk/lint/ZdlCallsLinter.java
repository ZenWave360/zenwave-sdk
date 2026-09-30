package io.zenwave360.sdk.lint;

import java.util.Map;

import io.zenwave360.sdk.lint.LintDiagnostic.Severity;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLCallsUtils;

/** Reports {@code @calls} targets that cannot be resolved, with the same rules generators apply. */
public class ZdlCallsLinter implements Linter {

    @Override
    public void lint(LintContext context) {
        Map<String, Object> model = context.zdlModel();
        Map<String, Map<String, Object>> services = JSONPath.get(model, "$.services", Map.of());
        for (Map.Entry<String, Map<String, Object>> service : services.entrySet()) {
            Map<String, Map<String, Object>> methods = JSONPath.get(service.getValue(), "$.methods", Map.of());
            for (Map.Entry<String, Map<String, Object>> method : methods.entrySet()) {
                try {
                    ZDLCallsUtils.methodCalls(model, method.getValue());
                } catch (IllegalArgumentException e) {
                    String message = e.getMessage();
                    if (message != null && message.contains("whose model could not be loaded")) {
                        continue; // reported by ZdlReferencesLinter
                    }
                    String path = "services." + service.getKey() + ".methods." + method.getKey() + ".options.calls";
                    context.report(Severity.error, "zdl.calls-command", message, path);
                }
            }
        }
    }
}
