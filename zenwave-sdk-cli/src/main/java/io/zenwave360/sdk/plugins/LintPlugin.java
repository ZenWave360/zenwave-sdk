package io.zenwave360.sdk.plugins;

import java.util.List;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.lint.LintProcessor;
import io.zenwave360.sdk.lint.LintReportWriter;
import io.zenwave360.sdk.lint.ZdlCallsLinter;
import io.zenwave360.sdk.lint.ZdlListenerLinter;
import io.zenwave360.sdk.lint.ZdlProblemsLinter;
import io.zenwave360.sdk.lint.ZdlReferencesLinter;
import io.zenwave360.sdk.parsers.ZDLParser;

/**
 * Lints a ZDL model and writes one report (text, JSON or SARIF) to stdout or {@code output}.
 *
 * <p>Exit codes: {@code 0} no failing findings, {@code 1} errors found (or warnings with {@code strict}),
 * {@code 2} the tool failed.
 *
 * <p>The {@code linters} option selects the checks; this plugin's default is the full built-in ZDL set. Presets are
 * plugins too: extend this class and set a different default {@code linters} list in the constructor. Custom
 * linters implement {@link io.zenwave360.sdk.lint.Linter} and are added with {@code --deps}.
 */
@DocumentedPlugin(
        summary = "Lints a ZDL model and reports findings as text, JSON or SARIF",
        mainOptions = { "zdlFile", "linters", "format", "output", "strict", "root" })
public class LintPlugin extends Plugin {

    public static final List<String> ZDL_LINTERS = List.of(
            ZdlProblemsLinter.class.getName(),
            ZdlReferencesLinter.class.getName(),
            ZdlCallsLinter.class.getName(),
            ZdlListenerLinter.class.getName());

    public LintPlugin() {
        withChain(ZDLParser.class, LintProcessor.class, LintReportWriter.class);
        withOption("continueOnZdlError", true);
        withOption("linters", ZDL_LINTERS);
    }
}
