package io.zenwave360.sdk.lint;

import java.util.Map;

import io.zenwave360.sdk.lint.LintDiagnostic.Severity;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;

/**
 * Reports {@code apis} entries ({@code zdl client}, {@code asyncapi client}, ...) whose referenced model could not be
 * loaded. Loading is done by {@link ZDLParser}, with the project classpath and authentication, so {@code classpath:}
 * and authenticated URIs resolve the same way they do for generation.
 */
public class ZdlReferencesLinter implements Linter {

    @Override
    public void lint(LintContext context) {
        Map<String, Map<String, Object>> apis = JSONPath.get(context.zdlModel(), "$.apis", Map.of());
        for (Map.Entry<String, Map<String, Object>> entry : apis.entrySet()) {
            Map<String, Object> api = entry.getValue();
            if (api.get(ZDLParser.REFERENCED_API_ERROR_PROPERTY) instanceof String error) {
                String path = "apis." + entry.getKey() + ".uri";
                context.report(Severity.error, "zdl.reference-unresolved",
                        "Unable to load " + api.get("type") + " api '" + entry.getKey() + "': " + error, path);
            }
        }
    }
}
