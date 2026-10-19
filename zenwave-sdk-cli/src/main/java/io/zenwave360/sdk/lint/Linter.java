package io.zenwave360.sdk.lint;

/**
 * A lint check over a parsed model. Implementations report findings through {@link LintContext#report} and never
 * print: the {@code LintPlugin} chain writes the single report.
 *
 * <p>Linters are listed by class name in the {@code linters} option of {@code LintPlugin} and are loaded from the
 * project classpath, so custom linters can ship in their own jars. They need a public no-args constructor.
 */
public interface Linter {

    void lint(LintContext context);

    /**
     * Whether this linter only runs on models without syntax errors. After a syntax error the parser returns a
     * partial model, and semantic checks on it would report cascading false errors.
     */
    default boolean requiresValidSyntax() {
        return true;
    }
}
