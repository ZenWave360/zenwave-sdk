package io.zenwave360.sdk.lint;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.parsers.WithProjectClassLoader;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.NamingUtils;

/**
 * Runs the configured {@link Linter}s, in order, over the parsed ZDL model and stores their findings for
 * {@link LintReportWriter}. A linter that cannot be loaded or throws is recorded as a tool failure, and the
 * remaining linters still run.
 */
public class LintProcessor implements Processor, WithProjectClassLoader<LintProcessor> {

    private static final String LINTERS_PACKAGE = LintProcessor.class.getPackageName();

    private static final Comparator<LintDiagnostic> ORDER = Comparator
            .comparing(LintDiagnostic::file)
            .thenComparingInt(LintDiagnostic::line)
            .thenComparingInt(LintDiagnostic::column)
            .thenComparing(LintDiagnostic::ruleId)
            .thenComparing(LintDiagnostic::message);

    @DocumentedOption(description = "Linters to run, in order (comma separated). Fully qualified class names, "
            + "or simple names of the built-in linters in io.zenwave360.sdk.lint (e.g. ZdlProblemsLinter)")
    public List<String> linters = List.of();

    @DocumentedOption(description = "Base directory for reported file paths. Defaults to the working directory")
    public String root;

    public String zdlFile;

    private ClassLoader projectClassLoader;

    @Override
    public LintProcessor withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Path lintRoot = Path.of(root != null ? root : "").toAbsolutePath().normalize();
        Map<String, Object> zdlModel = (Map<String, Object>) contextModel.get("zdl");
        var context = new LintContext(displayPath(lintRoot, zdlFile), zdlModel);
        var toolFailures = new ArrayList<String>();
        boolean syntaxErrors = context.hasSyntaxErrors();

        for (String linterName : new LinkedHashSet<>(linters)) {
            try {
                Linter linter = instantiate(linterName);
                if (syntaxErrors && linter.requiresValidSyntax()) {
                    continue;
                }
                linter.lint(context);
            } catch (Exception | LinkageError e) {
                toolFailures.add("Linter '" + linterName + "' failed: " + rootMessage(e));
            }
        }

        var diagnostics = new ArrayList<>(context.diagnostics());
        diagnostics.sort(ORDER);
        contextModel.put(LintRun.MODEL_PROPERTY, new LintRun(lintRoot, 1, List.copyOf(diagnostics), List.copyOf(toolFailures)));
        return contextModel;
    }

    private Linter instantiate(String name) throws ReflectiveOperationException {
        Class<?> linterClass = loadLinterClass(name.trim());
        if (!Linter.class.isAssignableFrom(linterClass)) {
            throw new IllegalArgumentException(linterClass.getName() + " does not implement " + Linter.class.getName());
        }
        return (Linter) linterClass.getDeclaredConstructor().newInstance();
    }

    private Class<?> loadLinterClass(String name) throws ClassNotFoundException {
        var candidates = new ArrayList<String>();
        if (name.contains(".")) {
            candidates.add(name);
        } else {
            String typeName = NamingUtils.asJavaTypeName(name);
            for (String simpleName : List.of(name, typeName)) {
                candidates.add(LINTERS_PACKAGE + "." + simpleName);
                candidates.add(LINTERS_PACKAGE + "." + simpleName + "Linter");
            }
        }
        ClassLoader classLoader = projectClassLoader != null ? projectClassLoader : getClass().getClassLoader();
        for (String candidate : candidates) {
            try {
                return Class.forName(candidate, true, classLoader);
            } catch (ClassNotFoundException ignored) {
                // try next candidate
            }
        }
        throw new ClassNotFoundException("linter class not found: " + name);
    }

    private static String displayPath(Path root, String file) {
        if (file == null) {
            return "-";
        }
        if (file.startsWith("classpath:") || file.contains("://")) {
            return file;
        }
        Path normalized = Path.of(file).toAbsolutePath().normalize();
        String value;
        try {
            value = root.relativize(normalized).toString();
        } catch (IllegalArgumentException e) {
            value = normalized.toString();
        }
        return value.replace('\\', '/');
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }
}
