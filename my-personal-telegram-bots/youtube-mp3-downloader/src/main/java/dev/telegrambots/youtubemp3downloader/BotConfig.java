package dev.telegrambots.youtubemp3downloader;

import java.util.ResourceBundle;

/**
 * Configuration class for the YouTube MP3 Downloader Bot.
 * Handles tool paths, file limits, and parallel download settings.
 */
public class BotConfig {
    public final String ytDlpPath;
    public final String ffmpegPath;
    public final String ffprobePath;
    public final long maxFileSize;
    public final double maxDurationMinutes;
    public final int maxParallelDownloads;

    /**
     * Constructor that loads configuration from config.properties.
     * Supports multiple path resolution strategies for cross-platform compatibility.
     */
    public BotConfig() {
        ResourceBundle config = ResourceBundle.getBundle("config");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows-specific path resolution with fallbacks
            this.ytDlpPath = resolvePath(config, "yt-dlp.path.win", "YT_DLP_PATH", "yt-dlp");
            this.ffmpegPath = resolvePath(config, "ffmpeg.path.win", "FFMPEG_PATH", "ffmpeg");
            this.ffprobePath = resolvePath(config, "ffprobe.path.win", "FFPROBE_PATH", "ffprobe");
        } else {
            // Unix/Linux path resolution
            this.ytDlpPath = config.containsKey("yt-dlp.path") ? config.getString("yt-dlp.path") : "yt-dlp";
            this.ffmpegPath = config.containsKey("ffmpeg.path") ? config.getString("ffmpeg.path") : "ffmpeg";
            this.ffprobePath = config.containsKey("ffprobe.path") ? config.getString("ffprobe.path") : "ffprobe";
        }

        // File size limit in bytes (default: 50MB)
        this.maxFileSize = config.containsKey("max.filesize") ?
            Long.parseLong(config.getString("max.filesize")) : 50 * 1024 * 1024;

        // Duration limit in minutes (default: 10 minutes)
        this.maxDurationMinutes = config.containsKey("max.duration") ?
            Double.parseDouble(config.getString("max.duration")) : 10.0;

        // Maximum parallel downloads (default: 3)
        this.maxParallelDownloads = config.containsKey("max.parallel.downloads") ?
            Integer.parseInt(config.getString("max.parallel.downloads")) : 3;
    }

    /**
     * Resolves tool path with multiple fallback strategies.
     * Priority: config property -> environment variable -> default command
     *
     * @param config ResourceBundle containing configuration
     * @param configKey Configuration property key
     * @param envVar Environment variable name
     * @param defaultCmd Default command name
     * @return Resolved path or command
     */
    private String resolvePath(ResourceBundle config, String configKey, String envVar, String defaultCmd) {
        // First priority: explicit config property
        if (config.containsKey(configKey)) {
            String path = config.getString(configKey);
            if (!path.isEmpty()) {
                return path;
            }
        }

        // Second priority: environment variable
        String envPath = System.getenv(envVar);
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // Third priority: default command (assumes it's in PATH)
        return defaultCmd;
    }
}
