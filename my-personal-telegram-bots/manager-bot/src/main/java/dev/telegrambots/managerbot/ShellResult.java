package dev.telegrambots.managerbot;

/**
 * Result of a shell command execution.
 */
public class ShellResult {
    public final int exitCode;
    public final String stdout;
    public final String stderr;

    public ShellResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

    /** Combined output for display: stdout first, then stderr if present. */
    public String combined() {
        if (stderr == null || stderr.isBlank()) return stdout;
        if (stdout == null || stdout.isBlank()) return stderr;
        return stdout + "\n--- stderr ---\n" + stderr;
    }
}
