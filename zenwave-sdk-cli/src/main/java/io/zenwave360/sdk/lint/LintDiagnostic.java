package io.zenwave360.sdk.lint;

/** A source-normalized lint finding. Lines and columns are one-based. */
public record LintDiagnostic(
        String file,
        int line,
        int column,
        int endLine,
        int endColumn,
        Severity severity,
        String ruleId,
        String message,
        String modelPath) {

    public enum Severity {
        error, warning, note
    }
}
