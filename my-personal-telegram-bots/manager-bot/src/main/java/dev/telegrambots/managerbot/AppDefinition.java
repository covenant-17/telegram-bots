package dev.telegrambots.managerbot;

/**
 * Describes a managed application: its name, repo path on Termux,
 * build commands, the resulting jar path, and how to detect its process.
 */
public class AppDefinition {

    public final String name;
    /** Absolute path to the git repo on Termux, e.g. ~/repos/telegram-bots */
    public final String repoPath;
    /**
     * Relative subpath inside repoPath where mvn package is run.
     * Empty string means the repo root itself.
     */
    public final String buildSubPath;
    /**
     * Extra Maven step before the main build (e.g. "mvn install -B" for shared-config).
     * Null if not needed.
     */
    public final String preBuildSubPath;
    /** Absolute path to the uber-jar deployed/running on Termux */
    public final String jarPath;
    /** Log file path */
    public final String logPath;
    /** Error log file path */
    public final String errLogPath;
    /** String used to identify this app's process in ps output */
    public final String processPattern;
    /**
     * Git remote URL for cloning the repo if {@code repoPath} does not exist yet.
     * Null means cloning is not supported (repo must be present manually).
     */
    public final String repoUrl;

    public AppDefinition(
            String name,
            String repoPath,
            String buildSubPath,
            String preBuildSubPath,
            String jarPath,
            String logPath,
            String errLogPath,
            String processPattern,
            String repoUrl) {
        this.name = name;
        this.repoPath = repoPath;
        this.buildSubPath = buildSubPath;
        this.preBuildSubPath = preBuildSubPath;
        this.jarPath = jarPath;
        this.logPath = logPath;
        this.errLogPath = errLogPath;
        this.processPattern = processPattern;
        this.repoUrl = repoUrl;
    }
}
