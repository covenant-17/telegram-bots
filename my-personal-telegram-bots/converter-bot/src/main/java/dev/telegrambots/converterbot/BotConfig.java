package dev.telegrambots.converterbot;

import dev.telegrambots.shared.BaseBotConfig;

/**
 * Configuration class for the Converter Bot.
 * Handles bot credentials and tool paths.
 */
public class BotConfig extends BaseBotConfig {
    public final String botToken;
    public final String botUsername;
    public final String ffmpegPath;

    /**
     * Constructor that loads configuration from config.properties.
     */
    public BotConfig() {
        super();

        // Bot credentials
        this.botToken = config.getString("bot.token");
        this.botUsername = config.getString("bot.username");

        // Tool paths
        this.ffmpegPath = getStringProperty("ffmpeg.path", "ffmpeg");
    }
}
