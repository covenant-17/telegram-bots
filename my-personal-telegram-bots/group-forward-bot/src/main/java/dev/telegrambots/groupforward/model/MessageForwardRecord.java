package dev.telegrambots.groupforward.model;

import java.time.Instant;

public record MessageForwardRecord(
        long sourceChatId,
        int sourceMessageId,
        long fromUserId,
        String fromUsername,
        Instant messageDate,
        String text,
        int attemptCount
) {
}
