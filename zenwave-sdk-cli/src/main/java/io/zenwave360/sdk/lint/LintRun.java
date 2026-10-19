package io.zenwave360.sdk.lint;

import java.nio.file.Path;
import java.util.List;

/** Result of running the configured linters, passed from {@link LintProcessor} to {@link LintReportWriter}. */
record LintRun(Path root, int files, List<LintDiagnostic> diagnostics, List<String> toolFailures) {

    static final String MODEL_PROPERTY = "lint";

    long errors() {
        return diagnostics.stream().filter(d -> d.severity() == LintDiagnostic.Severity.error).count();
    }

    long warnings() {
        return diagnostics.stream().filter(d -> d.severity() == LintDiagnostic.Severity.warning).count();
    }
}
