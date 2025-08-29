package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import java.util.ResourceBundle;

public class Bot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;

    // Using deprecated constructor for compatibility with current Telegram Bots API version
    // TODO: Update to new constructor when upgrading to newer API version
    public Bot() {
        super(ResourceBundle.getBundle("config").getString("bot.token"));
        ResourceBundle config = ResourceBundle.getBundle("config");
        this.botToken = config.getString("bot.token");
        this.botUsername = config.getString("bot.username");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        CommandHandler.handle(this, update);
    }

    public void sendTextMessage(Long chatId, String text) {
        org.telegram.telegrambots.meta.api.methods.send.SendMessage message = new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public void sendChatAction(Long chatId, ActionType action) {
        org.telegram.telegrambots.meta.api.methods.send.SendChatAction chatAction = new org.telegram.telegrambots.meta.api.methods.send.SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(action);
        try {
            execute(chatAction);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
