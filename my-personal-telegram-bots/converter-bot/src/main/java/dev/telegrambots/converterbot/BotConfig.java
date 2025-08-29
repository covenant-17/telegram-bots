package dev.telegrambots.converterbot;

import java.util.ResourceBundle;

/**
 * Configuration class for the Converter Bot.
 * Handles bot credentials and tool paths.
 */
public class BotConfig {
    public final String botToken;
    public final String botUsername;
    public final String ffmpegPath;
    public final long maxFileSize;
    public final double maxDurationMinutes;

    /**
     * Constructor that loads configuration from config.properties.
     */
    public BotConfig() {
        ResourceBundle config = ResourceBundle.getBundle("config");

        // Bot credentials
        this.botToken = config.getString("bot.token");
        this.botUsername = config.getString("bot.username");

        // Tool paths
        this.ffmpegPath = config.containsKey("ffmpeg.path") ?
            config.getString("ffmpeg.path") : "ffmpeg";

        // File limits
        this.maxFileSize = config.containsKey("max.filesize") ?
            Long.parseLong(config.getString("max.filesize")) : 50 * 1024 * 1024;

        this.maxDurationMinutes = config.containsKey("max.duration") ?
            Double.parseDouble(config.getString("max.duration")) : 10.0;
    }
}
