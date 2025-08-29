package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

public class TelegramService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final Bot bot;

    public TelegramService(Bot bot) {
        this.bot = bot;
    }

    private static String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void sendText(Long chatId, String text) {
        try {
            bot.sendTextMessage(chatId, text);
        } catch (Exception e) {
            logger.error("[{}] [Telegram] Failed to send text: {}", now(), text, e);
        }
    }

    public void sendAudio(String chatId, File audioFile) {
        try {
            bot.sendAudioFile(chatId, audioFile);
        } catch (Exception e) {
            logger.error("[{}] [Telegram] Failed to send audio file: {}", now(), audioFile.getName(), e);
        }
    }

    public void sendAudio(String chatId, File audioFile, String caption) {
        try {
            bot.sendAudioFile(chatId, audioFile, caption);
        } catch (Exception e) {
            logger.error("[{}] [Telegram] Failed to send audio file: {}", now(), audioFile.getName(), e);
        }
    }

    public void sendChatAction(Long chatId, ActionType action) {
        try {
            bot.sendChatAction(chatId, action);
        } catch (Exception e) {
            logger.error("[{}] [Telegram] Failed to send chat action: {}", now(), action, e);
        }
    }
}
