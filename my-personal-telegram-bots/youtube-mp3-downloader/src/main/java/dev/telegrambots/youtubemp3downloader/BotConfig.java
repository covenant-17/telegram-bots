package dev.telegrambots.youtubemp3downloader;

import dev.telegrambots.shared.BaseBotConfig;

/**
 * Configuration class for the YouTube MP3 Downloader Bot.
 * Handles tool paths, file limits, and parallel download settings.
 */
public class BotConfig extends BaseBotConfig {
    public static final String MAX_PARALLEL_DOWNLOADS_PROPERTY = "max.parallel.downloads";
    public static final int DEFAULT_MAX_PARALLEL_DOWNLOADS = 3;

    public final String botToken;
    public final String botUsername;
    public final String ytDlpPath;
    public final String ffmpegPath;
    public final String ffprobePath;
    public final int maxParallelDownloads;
    public final String cookiesFilePath;

    /**
     * Constructor that loads configuration from config.properties.
     * Supports multiple path resolution strategies for cross-platform compatibility.
     */
    public BotConfig() {
        super();
        
        // Bot credentials
        this.botToken = getStringProperty("bot.token", "");
        this.botUsername = getStringProperty("bot.username", "");
        
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows-specific path resolution with fallbacks
            this.ytDlpPath = resolvePath("yt-dlp.path.win", "YT_DLP_PATH", "yt-dlp");
            this.ffmpegPath = resolvePath("ffmpeg.path.win", "FFMPEG_PATH", "ffmpeg");
            this.ffprobePath = resolvePath("ffprobe.path.win", "FFPROBE_PATH", "ffprobe");
        } else {
            // Unix/Linux path resolution with fallbacks
            this.ytDlpPath = resolvePath("yt-dlp.path.unix", "YT_DLP_PATH", "yt-dlp");
            this.ffmpegPath = resolvePath("ffmpeg.path.unix", "FFMPEG_PATH", "ffmpeg");
            this.ffprobePath = resolvePath("ffprobe.path.unix", "FFPROBE_PATH", "ffprobe");
        }

        this.maxParallelDownloads = getIntProperty(MAX_PARALLEL_DOWNLOADS_PROPERTY, DEFAULT_MAX_PARALLEL_DOWNLOADS);
        
        // Optional: path to YouTube cookies file for bot-detection bypass
        this.cookiesFilePath = getStringProperty("yt-dlp.cookies.path", "");
        
        // Validate configuration after all fields are initialized
        validateConfiguration();
    }
    
    @Override
    protected void validateConfiguration() {
        super.validateConfiguration();
        
        if (botToken == null || botToken.trim().isEmpty()) {
            throw new IllegalStateException("Bot token is required in config.properties (bot.token)");
        }
        if (botUsername == null || botUsername.trim().isEmpty()) {
            throw new IllegalStateException("Bot username is required in config.properties (bot.username)");
        }
    }
}
