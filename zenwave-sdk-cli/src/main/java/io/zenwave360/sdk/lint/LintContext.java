package io.zenwave360.sdk.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.lint.LintDiagnostic.Severity;
import io.zenwave360.sdk.utils.JSONPath;

/** What a {@link Linter} sees: the parsed ZDL model of one file, and where to report findings. */
public final class LintContext {

    private final String file;
    private final Map<String, Object> zdlModel;
    private final List<LintDiagnostic> diagnostics = new ArrayList<>();

    public LintContext(String file, Map<String, Object> zdlModel) {
        this.file = file;
        this.zdlModel = zdlModel;
    }

    /** Linted file, relative to the lint root, with forward slashes. */
    public String file() {
        return file;
    }

    /** Raw parser model of the linted ZDL file. */
    public Map<String, Object> zdlModel() {
        return zdlModel;
    }

    public boolean hasSyntaxErrors() {
        List<Map<String, Object>> problems = JSONPath.get(zdlModel, "$.problems[*]", List.of());
        return problems.stream().anyMatch(problem -> {
            Object path = problem.get("path");
            return "syntax".equals(path) || "parser".equals(path);
        });
    }

    /**
     * Reports a finding at the source location of {@code modelPath}, or of its closest located parent.
     *
     * @param ruleId stable, namespaced rule id, e.g. {@code zdl.unknown-type} or {@code acme.naming}
     * @param modelPath dotted path of the model element, e.g. {@code entities.Customer.fields.name}; also used to
     *        fingerprint the finding across commits, so it should identify it uniquely within its rule
     */
    public void report(Severity severity, String ruleId, String message, String modelPath) {
        report(severity, ruleId, message, modelPath, findLocation(modelPath));
    }

    /**
     * Reports a finding at an explicit parser location: {@code [startOffset, endOffset, line, column, endLine,
     * endColumn]} with 1-based lines and 0-based columns, as the ZDL parser produces them.
     */
    public void report(Severity severity, String ruleId, String message, String modelPath, int[] location) {
        int line = location != null && location.length > 2 ? Math.max(1, location[2]) : 1;
        int column = location != null && location.length > 3 ? Math.max(1, location[3] + 1) : 1;
        int endLine = location != null && location.length > 4 ? Math.max(line, location[4]) : line;
        int endColumn = location != null && location.length > 5 ? Math.max(column, location[5] + 1) : column;
        diagnostics.add(new LintDiagnostic(file, line, column, endLine, endColumn, severity, ruleId, message, modelPath));
    }

    List<LintDiagnostic> diagnostics() {
        return diagnostics;
    }

    private int[] findLocation(String path) {
        if (path == null) return null;
        Map<String, Object> locations = JSONPath.get(zdlModel, "$.locations", Map.of());
        if (locations.get(path) instanceof int[] exact) return exact;
        String candidate = path;
        while (candidate.contains(".")) {
            candidate = candidate.substring(0, candidate.lastIndexOf('.'));
            if (locations.get(candidate) instanceof int[] parent) return parent;
        }
        return locations.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(path + ".") && entry.getValue() instanceof int[])
                .map(entry -> (int[]) entry.getValue())
                .findFirst().orElse(null);
    }
}
