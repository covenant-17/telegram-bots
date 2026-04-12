package dev.telegrambots.managerbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs shell commands on the local system (Termux).
 * All operations are synchronous and return a ShellResult.
 */
public class ShellRunner {

    private static final String SHELL = "/data/data/com.termux/files/usr/bin/bash";
    private static final int BUILD_TIMEOUT_MINUTES = 10;

    /**
     * Run a bash command in the given working directory.
     * Blocks until the process finishes or timeout is reached.
     */
    public static ShellResult run(String command, String workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(SHELL, "-c", command);
            if (workDir != null && !workDir.isBlank()) {
                pb.directory(Paths.get(workDir).toFile());
            }
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Read stdout and stderr concurrently to avoid blocking
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();

            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(BUILD_TIMEOUT_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            stdoutReader.join(5000);
            stderrReader.join(5000);

            if (!finished) {
                process.destroyForcibly();
                return new ShellResult(-1, out.toString(), "TIMEOUT after " + BUILD_TIMEOUT_MINUTES + " minutes");
            }
            return new ShellResult(process.exitValue(), out.toString().trim(), err.toString().trim());
        } catch (Exception e) {
            return new ShellResult(-1, "", e.getMessage());
        }
    }

    /**
     * Run a background process (nohup-style). Does not wait for completion.
     * Returns immediately with pid=-1 (process is detached).
     */
    public static void runDetached(String command, String logFile) {
        try {
            String cmd = "nohup " + command + " >> " + logFile + " 2>&1 &";
            ProcessBuilder pb = new ProcessBuilder(SHELL, "-c", cmd);
            pb.start();
        } catch (Exception ignored) {}
    }

    /**
     * Get the last N lines of a file.
     */
    public static ShellResult tail(String filePath, int lines) {
        return run("tail -n " + lines + " " + filePath, null);
    }

    /**
     * Returns PIDs of processes matching the pattern (via ps + grep).
     */
    public static List<String> findPids(String pattern) {
        ShellResult result = run(
                "ps aux | grep '" + pattern + "' | grep 'java -jar' | grep -v grep | awk '{print $2}'",
                null);
        List<String> pids = new ArrayList<>();
        if (result.isSuccess() && !result.stdout.isBlank()) {
            for (String line : result.stdout.split("\n")) {
                String pid = line.trim();
                if (!pid.isBlank()) pids.add(pid);
            }
        }
        return pids;
    }

    /**
     * Kill all processes matching the pattern.
     * Returns how many were killed.
     */
    public static int killByPattern(String pattern) {
        List<String> pids = findPids(pattern);
        for (String pid : pids) {
            run("kill -9 " + pid, null);
        }
        return pids.size();
    }
}
