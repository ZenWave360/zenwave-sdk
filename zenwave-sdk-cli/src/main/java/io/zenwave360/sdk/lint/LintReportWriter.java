package io.zenwave360.sdk.lint;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.ZenWaveException;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.CliOutput;

/**
 * Writes the lint report as the only result output, then signals the outcome with the exit code: a
 * {@link ZenWaveException} with code {@code 1} for failing findings, {@code 2} when a linter failed.
 */
public class LintReportWriter implements Processor {

    public enum Format {
        text, json, sarif
    }

    static final String TOOL_NAME = "zenwave-lint";
    static final String INFORMATION_URI = "https://zenwave360.github.io/zenwave-sdk/";

    @DocumentedOption(description = "Report format", values = { "text", "json", "sarif" }, defaultValue = "text")
    public Format format = Format.text;

    @DocumentedOption(description = "Write the report as UTF-8 to this file instead of stdout")
    public String output;

    @DocumentedOption(description = "Fail (exit code 1) when warnings are found, not only errors")
    public boolean strict = false;

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        LintRun run = (LintRun) contextModel.get(LintRun.MODEL_PROPERTY);
        if (run == null) {
            throw new IllegalStateException("No lint results in the model: LintProcessor must run before LintReportWriter");
        }
        String report = write(run);
        if (output != null && !output.isBlank()) {
            writeFile(Path.of(output), report);
        } else {
            CliOutput.stdout().print(report);
            CliOutput.stdout().flush();
        }

        if (!run.toolFailures().isEmpty()) {
            throw new ZenWaveException(String.join(System.lineSeparator(), run.toolFailures()),
                    ZenWaveException.EXIT_TOOL_FAILURE);
        }
        if (failing(run)) {
            throw new ZenWaveException(TOOL_NAME + ": " + run.errors() + " error(s), " + run.warnings() + " warning(s)"
                    + (strict && run.errors() == 0 ? " (--strict)" : ""), ZenWaveException.EXIT_FINDINGS);
        }
        return contextModel;
    }

    String write(LintRun run) {
        return switch (format) {
            case text -> text(run);
            case json -> pretty(json(run));
            case sarif -> pretty(sarif(run));
        };
    }

    private boolean failing(LintRun run) {
        return run.errors() > 0 || (strict && run.warnings() > 0);
    }

    private int exitCode(LintRun run) {
        return !run.toolFailures().isEmpty() ? ZenWaveException.EXIT_TOOL_FAILURE : failing(run) ? ZenWaveException.EXIT_FINDINGS : 0;
    }

    private String text(LintRun run) {
        boolean color = (output == null || output.isBlank()) && System.console() != null;
        var report = new StringBuilder();
        for (LintDiagnostic diagnostic : run.diagnostics()) {
            String severity = diagnostic.severity().name();
            if (color) {
                severity = switch (diagnostic.severity()) {
                    case error -> "\u001B[31merror\u001B[0m";
                    case warning -> "\u001B[33mwarning\u001B[0m";
                    case note -> "\u001B[36mnote\u001B[0m";
                };
            }
            report.append("%s:%d:%d: %s %s %s\n".formatted(diagnostic.file(), diagnostic.line(), diagnostic.column(),
                    severity, diagnostic.ruleId(), diagnostic.message()));
        }
        return report.toString();
    }

    private Map<String, Object> json(LintRun run) {
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("schemaVersion", "1");
        envelope.put("tool", Map.of("name", TOOL_NAME, "version", version()));
        var summary = new LinkedHashMap<String, Object>();
        summary.put("files", run.files());
        summary.put("errors", run.errors());
        summary.put("warnings", run.warnings());
        envelope.put("summary", summary);
        envelope.put("diagnostics", run.diagnostics().stream().map(this::jsonDiagnostic).toList());
        if (!run.toolFailures().isEmpty()) {
            envelope.put("toolFailures", run.toolFailures());
        }
        return envelope;
    }

    private Map<String, Object> jsonDiagnostic(LintDiagnostic diagnostic) {
        var value = new LinkedHashMap<String, Object>();
        value.put("file", diagnostic.file());
        value.put("line", diagnostic.line());
        value.put("column", diagnostic.column());
        value.put("endLine", diagnostic.endLine());
        value.put("endColumn", diagnostic.endColumn());
        value.put("severity", diagnostic.severity().name());
        value.put("ruleId", diagnostic.ruleId());
        value.put("message", diagnostic.message());
        if (diagnostic.modelPath() != null) {
            value.put("modelPath", diagnostic.modelPath());
        }
        return value;
    }

    private Map<String, Object> sarif(LintRun run) {
        List<String> ruleIds = new ArrayList<>(new TreeSet<>(run.diagnostics().stream().map(LintDiagnostic::ruleId).toList()));

        var driver = new LinkedHashMap<String, Object>();
        driver.put("name", TOOL_NAME);
        driver.put("version", version());
        driver.put("informationUri", INFORMATION_URI);
        driver.put("rules", ruleIds.stream().map(id -> Map.of("id", id)).toList());

        var invocation = new LinkedHashMap<String, Object>();
        invocation.put("executionSuccessful", run.toolFailures().isEmpty());
        invocation.put("exitCode", exitCode(run));
        if (!run.toolFailures().isEmpty()) {
            invocation.put("toolExecutionNotifications", run.toolFailures().stream()
                    .map(failure -> Map.of("level", "error", "message", Map.of("text", failure)))
                    .toList());
        }

        var sarifRun = new LinkedHashMap<String, Object>();
        sarifRun.put("tool", Map.of("driver", driver));
        sarifRun.put("originalUriBaseIds", Map.of("%SRCROOT%", Map.of("uri", sourceRootUri(run))));
        sarifRun.put("results", run.diagnostics().stream().map(d -> sarifResult(d, ruleIds.indexOf(d.ruleId()))).toList());
        sarifRun.put("invocations", List.of(invocation));

        var document = new LinkedHashMap<String, Object>();
        document.put("$schema", "https://json.schemastore.org/sarif-2.1.0.json");
        document.put("version", "2.1.0");
        document.put("runs", List.of(sarifRun));
        return document;
    }

    private Map<String, Object> sarifResult(LintDiagnostic diagnostic, int ruleIndex) {
        var result = new LinkedHashMap<String, Object>();
        result.put("ruleId", diagnostic.ruleId());
        result.put("ruleIndex", ruleIndex);
        result.put("level", diagnostic.severity().name());
        result.put("message", Map.of("text", diagnostic.message()));
        result.put("locations", List.of(Map.of(
                "physicalLocation", Map.of(
                        "artifactLocation", Map.of("uri", diagnostic.file(), "uriBaseId", "%SRCROOT%"),
                        "region", Map.of(
                                "startLine", diagnostic.line(),
                                "startColumn", diagnostic.column(),
                                "endLine", diagnostic.endLine(),
                                "endColumn", diagnostic.endColumn())))));
        result.put("partialFingerprints", Map.of("zenwaveLint/v1", fingerprint(diagnostic)));
        if (diagnostic.modelPath() != null) {
            result.put("properties", Map.of("modelPath", diagnostic.modelPath()));
        }
        return result;
    }

    private String version() {
        String implementationVersion = LintReportWriter.class.getPackage().getImplementationVersion();
        return implementationVersion != null ? implementationVersion : "dev";
    }

    private String sourceRootUri(LintRun run) {
        String uri = run.root().toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    private String fingerprint(LintDiagnostic diagnostic) {
        String identity = diagnostic.ruleId() + "\u0000" + diagnostic.file() + "\u0000"
                + (diagnostic.modelPath() != null ? diagnostic.modelPath() + "\u0000" : "") + diagnostic.message();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void writeFile(Path path, String report) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (normalized.getParent() != null) {
                Files.createDirectories(normalized.getParent());
            }
            Files.writeString(normalized, report, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ZenWaveException("Unable to write lint report to " + path + ": " + e.getMessage(),
                    ZenWaveException.EXIT_TOOL_FAILURE, e);
        }
    }

    private String pretty(Object value) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize lint report", e);
        }
    }
}
