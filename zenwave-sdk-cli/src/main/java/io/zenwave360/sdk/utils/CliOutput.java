package io.zenwave360.sdk.utils;

import java.io.PrintStream;

/**
 * The CLI's result stream. When running from the command line, {@link #reserveStdout()} keeps the real stdout for
 * plugins whose output is the result (reports, JSON, printed templates) and redirects {@code System.out} to stderr,
 * so logs and incidental prints never corrupt what agents and CI parse from stdout.
 *
 * <p>Outside the CLI (tests, Maven plugin, embedding) nothing is redirected and {@link #stdout()} is {@code System.out}.
 */
public final class CliOutput {

    private static PrintStream stdout;

    private CliOutput() {
    }

    /** Keeps the current stdout as the result stream and sends everything else written to System.out to stderr. */
    public static synchronized void reserveStdout() {
        if (stdout == null) {
            stdout = System.out;
            System.setOut(System.err);
        }
    }

    /** Stream for a plugin's result output. */
    public static synchronized PrintStream stdout() {
        return stdout != null ? stdout : System.out;
    }

    /** Overrides the result stream, for tests. {@code null} resets it to System.out. */
    public static synchronized void setStdout(PrintStream stream) {
        stdout = stream;
    }
}
