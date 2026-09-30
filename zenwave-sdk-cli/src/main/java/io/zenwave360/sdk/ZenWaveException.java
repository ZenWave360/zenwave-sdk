package io.zenwave360.sdk;

/**
 * Exception carrying the process exit code the CLI returns when it is thrown from a plugin chain.
 *
 * <p>Exit codes: {@code 0} success, {@code 1} the input was processed and found invalid (e.g. lint findings),
 * {@code 2} the tool itself failed. Any other exception reaching the CLI exits with {@code 2}.
 */
public class ZenWaveException extends RuntimeException {

    public static final int EXIT_FINDINGS = 1;
    public static final int EXIT_TOOL_FAILURE = 2;

    private final int exitCode;

    public ZenWaveException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ZenWaveException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
