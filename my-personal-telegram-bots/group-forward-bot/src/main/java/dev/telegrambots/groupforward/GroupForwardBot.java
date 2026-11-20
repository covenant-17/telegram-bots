package dev.telegrambots.groupforward;

import dev.telegrambots.groupforward.dao.BlacklistDao;
import dev.telegrambots.groupforward.dao.MessageDao;
import dev.telegrambots.groupforward.model.MessageForwardRecord;
import dev.telegrambots.shared.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.List;

public final class GroupForwardBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(GroupForwardBot.class);

    private final AppConfig config;
    private final BlacklistDao blacklistDao;
    private final MessageDao messageDao;

    public GroupForwardBot(AppConfig config, BlacklistDao blacklistDao, MessageDao messageDao) {
        super(config.botToken());
        this.config = config;
        this.blacklistDao = blacklistDao;
        this.messageDao = messageDao;
    }

    @Override
    public String getBotUsername() {
        return config.botUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }
        if (isAdminCommand(message)) {
            handleAdminCommand(message);
            return;
        }
        if (!isGroupMessage(message)) {
            return;
        }

        long fromUserId = message.getFrom().getId();
        if (blacklistDao.isBlacklisted(fromUserId)) {
            return;
        }

        boolean inserted = messageDao.tryInsertMessage(message, MessageStatus.NEW);
        if (!inserted) {
            return;
        }

        forwardWithTracking(message);
    }

    public void forwardStored(MessageForwardRecord record) {
        ForwardMessage forwardMessage = ForwardMessage.builder()
                .chatId(String.valueOf(config.adminChatId()))
                .fromChatId(String.valueOf(record.sourceChatId()))
                .messageId(record.sourceMessageId())
                .build();
        try {
            execute(forwardMessage);
            messageDao.markSent(record.sourceChatId(), record.sourceMessageId());
        } catch (TelegramApiException e) {
            messageDao.markFailed(record.sourceChatId(), record.sourceMessageId(), e.getMessage());
            logger.warn("Forward failed for retry message {}:{}", record.sourceChatId(), record.sourceMessageId(), e);
        }
    }

    private void forwardWithTracking(Message message) {
        ForwardMessage forwardMessage = ForwardMessage.builder()
                .chatId(String.valueOf(config.adminChatId()))
                .fromChatId(String.valueOf(message.getChatId()))
                .messageId(message.getMessageId())
                .build();
        try {
            execute(forwardMessage);
            messageDao.markSent(message.getChatId(), message.getMessageId());
        } catch (TelegramApiException e) {
            logger.warn("Forward failed for message {}:{}", message.getChatId(), message.getMessageId(), e);
            tryFallback(message, e.getMessage());
        }
    }

    private void tryFallback(Message message, String forwardError) {
        StringBuilder text = new StringBuilder();
        text.append("Forward failed, fallback text delivered.")
                .append("\nSource chat: ").append(message.getChatId())
                .append("\nSource message: ").append(message.getMessageId())
                .append("\nFrom user: ").append(message.getFrom().getId())
                .append("\nUsername: ").append(message.getFrom().getUserName())
                .append("\nDate: ").append(Instant.ofEpochSecond(message.getDate()))
                .append("\nError: ").append(forwardError)
                .append("\n\nText:\n").append(message.getText());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(String.valueOf(config.adminChatId()))
                .text(text.toString())
                .build();
        try {
            execute(sendMessage);
            messageDao.markSentWithError(message.getChatId(), message.getMessageId(), forwardError);
        } catch (TelegramApiException e) {
            messageDao.markFailed(message.getChatId(), message.getMessageId(), e.getMessage());
            logger.warn("Fallback send failed for message {}:{}", message.getChatId(), message.getMessageId(), e);
        }
    }

    private boolean isGroupMessage(Message message) {
        String type = message.getChat().getType();
        return "group".equals(type) || "supergroup".equals(type);
    }

    private boolean isAdminCommand(Message message) {
        if (!message.getText().startsWith("/blacklist_")) {
            return false;
        }
        if (message.getChatId() == config.adminChatId()) {
            return true;
        }
        Long adminUserId = config.adminUserId();
        return adminUserId != null && adminUserId.equals(message.getFrom().getId());
    }

    private void handleAdminCommand(Message message) {
        String text = message.getText().trim();
        String[] parts = text.split("\\s+", 3);
        String command = parts[0];

        switch (command) {
            case "/blacklist_add" -> handleBlacklistAdd(message, parts);
            case "/blacklist_remove" -> handleBlacklistRemove(message, parts);
            case "/blacklist_list" -> handleBlacklistList(message);
            default -> sendAdminMessage(message.getChatId(), "Unknown command.");
        }
    }

    private void handleBlacklistAdd(Message message, String[] parts) {
        if (parts.length < 2) {
            sendAdminMessage(message.getChatId(), "Usage: /blacklist_add <user_id> [reason]");
            return;
        }
        long userId;
        try {
            userId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendAdminMessage(message.getChatId(), "Invalid user_id: " + parts[1]);
            return;
        }
        String reason = parts.length >= 3 ? parts[2] : null;
        blacklistDao.add(userId, reason, message.getFrom().getId());
        sendAdminMessage(message.getChatId(), "User " + userId + " added to blacklist.");
    }

    private void handleBlacklistRemove(Message message, String[] parts) {
        if (parts.length < 2) {
            sendAdminMessage(message.getChatId(), "Usage: /blacklist_remove <user_id>");
            return;
        }
        long userId;
        try {
            userId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendAdminMessage(message.getChatId(), "Invalid user_id: " + parts[1]);
            return;
        }
        blacklistDao.remove(userId);
        sendAdminMessage(message.getChatId(), "User " + userId + " removed from blacklist.");
    }

    private void handleBlacklistList(Message message) {
        List<String> entries = blacklistDao.list();
        if (entries.isEmpty()) {
            sendAdminMessage(message.getChatId(), "Blacklist is empty.");
            return;
        }
        StringBuilder text = new StringBuilder("Blacklist:\n");
        for (String entry : entries) {
            text.append(entry).append("\n");
        }
        sendAdminMessage(message.getChatId(), text.toString());
    }

    private void sendAdminMessage(long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.warn("Failed to send admin message", e);
        }
    }
}
