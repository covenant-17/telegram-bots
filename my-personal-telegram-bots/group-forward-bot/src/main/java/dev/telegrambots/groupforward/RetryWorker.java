package dev.telegrambots.groupforward;

import dev.telegrambots.groupforward.dao.BlacklistDao;
import dev.telegrambots.groupforward.dao.MessageDao;
import dev.telegrambots.groupforward.model.MessageForwardRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public final class RetryWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RetryWorker.class);

    private final AppConfig config;
    private final BlacklistDao blacklistDao;
    private final MessageDao messageDao;
    private final GroupForwardBot bot;

    public RetryWorker(AppConfig config, BlacklistDao blacklistDao, MessageDao messageDao, GroupForwardBot bot) {
        this.config = config;
        this.blacklistDao = blacklistDao;
        this.messageDao = messageDao;
        this.bot = bot;
    }

    @Override
    public void run() {
        Instant cutoff = Instant.now().minus(config.retryDelay());
        List<MessageForwardRecord> records = messageDao.fetchForRetry(
                config.retryMaxAttempts(),
                cutoff,
                config.retryBatchSize()
        );
        for (MessageForwardRecord record : records) {
            if (blacklistDao.isBlacklisted(record.fromUserId())) {
                messageDao.markSkippedBlacklist(record.sourceChatId(), record.sourceMessageId());
                continue;
            }
            bot.forwardStored(record);
        }
        if (!records.isEmpty()) {
            logger.info("Retry worker processed {} messages", records.size());
        }
    }
}
