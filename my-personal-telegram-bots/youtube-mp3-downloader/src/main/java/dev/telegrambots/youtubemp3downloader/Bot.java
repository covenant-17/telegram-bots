package dev.telegrambots.youtubemp3downloader;

// Telegram Bots API - Core bot functionality
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.ActionType;

// Logging framework for comprehensive bot monitoring
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced YouTube MP3 Downloader Bot with comprehensive error handling and logging.
 * Features parallel downloads, file size validation, and robust exception management.
 *
 * Key Features:
 * - YouTube URL validation and metadata extraction
 * - Parallel download processing with configurable limits
 * - Comprehensive error handling and user feedback
 * - Automatic file cleanup and resource management
 * - Cross-platform tool path resolution
 *
 * @author Your Name
 * @version 1.0
 * @since 2025-08-29
 */
public class Bot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final BotConfig config;

    // Using deprecated constructor for compatibility with current Telegram Bots API version
    // TODO: Update to new constructor when upgrading to newer API version
    public Bot() {
        super(getBotTokenFromConfig());
        this.config = new BotConfig();
        logger.info("Bot initialized with token: {}...", config.botToken.substring(0, 10));
    }

    private static String getBotTokenFromConfig() {
        try {
            return java.util.ResourceBundle.getBundle("config").getString("bot.token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load bot token from config", e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.botUsername;
    }

    @Override
    public String getBotToken() {
        return config.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Advanced YouTube MP3 Bot update processing with parallel download management
        try {
            CommandHandler.handle(this, update);
        } catch (Exception e) {
            logger.error("Critical error in YouTube MP3 Bot update processing: {}", e.getMessage(), e);
        }
    }

    public void sendTextMessage(Long chatId, String text) {
        org.telegram.telegrambots.meta.api.methods.send.SendMessage message = new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send text message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendAudioFile(String chatId, java.io.File audioFile) {
        org.telegram.telegrambots.meta.api.methods.send.SendAudio sendAudio = new org.telegram.telegrambots.meta.api.methods.send.SendAudio();
        sendAudio.setChatId(chatId);
        sendAudio.setAudio(new org.telegram.telegrambots.meta.api.objects.InputFile(audioFile));
        sendAudio.setCaption("Your audio is ready!");
        try {
            execute(sendAudio);
        } catch (TelegramApiException e) {
            logger.error("Failed to send audio file to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendAudioFile(String chatId, java.io.File audioFile, String caption) {
        org.telegram.telegrambots.meta.api.methods.send.SendAudio sendAudio = new org.telegram.telegrambots.meta.api.methods.send.SendAudio();
        sendAudio.setChatId(chatId);
        sendAudio.setAudio(new org.telegram.telegrambots.meta.api.objects.InputFile(audioFile));
        if (caption != null) {
            sendAudio.setCaption(caption);
        } else {
            sendAudio.setCaption("Your audio is ready!");
        }
        try {
            execute(sendAudio);
        } catch (TelegramApiException e) {
            logger.error("Failed to send audio file with caption to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendChatAction(Long chatId, ActionType action) {
        org.telegram.telegrambots.meta.api.methods.send.SendChatAction chatAction = new org.telegram.telegrambots.meta.api.methods.send.SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(action);
        try {
            execute(chatAction);
        } catch (TelegramApiException e) {
            logger.error("Failed to send chat action to chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
