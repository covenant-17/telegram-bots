package dev.telegrambots.groupforward.dao;

import dev.telegrambots.groupforward.model.MessageForwardRecord;
import dev.telegrambots.shared.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MessageDao {
    private static final Logger logger = LoggerFactory.getLogger(MessageDao.class);

    private final DataSource dataSource;

    public MessageDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean tryInsertMessage(Message message, MessageStatus status) {
        String sql = "INSERT INTO message_forward (source_chat_id, source_message_id, from_user_id, from_username, "
                + "message_date, text, status, attempt_count) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0) ON CONFLICT (source_chat_id, source_message_id) DO NOTHING";
        Instant messageDate = Instant.ofEpochSecond(message.getDate());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, message.getChatId());
            statement.setInt(2, message.getMessageId());
            statement.setLong(3, message.getFrom().getId());
            statement.setString(4, message.getFrom().getUserName());
            statement.setTimestamp(5, Timestamp.from(messageDate));
            statement.setString(6, message.getText());
            statement.setString(7, status.name());
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            logger.warn("Failed to insert message {}:{}", message.getChatId(), message.getMessageId(), e);
            return false;
        }
    }

    public void markSent(long chatId, int messageId) {
        String sql = "UPDATE message_forward SET status = ?, attempt_count = attempt_count + 1, "
                + "last_attempt_at = ? WHERE source_chat_id = ? AND source_message_id = ?";
        updateStatus(sql, MessageStatus.SENT, chatId, messageId, null);
    }

    public void markSentWithError(long chatId, int messageId, String error) {
        String sql = "UPDATE message_forward SET status = ?, attempt_count = attempt_count + 1, "
                + "last_attempt_at = ?, last_error = ? WHERE source_chat_id = ? AND source_message_id = ?";
        updateStatus(sql, MessageStatus.SENT, chatId, messageId, error);
    }

    public void markFailed(long chatId, int messageId, String error) {
        String sql = "UPDATE message_forward SET status = ?, attempt_count = attempt_count + 1, "
                + "last_attempt_at = ?, last_error = ? WHERE source_chat_id = ? AND source_message_id = ?";
        updateStatus(sql, MessageStatus.FAILED, chatId, messageId, error);
    }

    public void markSkippedBlacklist(long chatId, int messageId) {
        String sql = "UPDATE message_forward SET status = ?, last_attempt_at = ? "
                + "WHERE source_chat_id = ? AND source_message_id = ?";
        updateStatus(sql, MessageStatus.SKIPPED_BLACKLIST, chatId, messageId, null);
    }

    private void updateStatus(String sql, MessageStatus status, long chatId, int messageId, String error) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            int index = 3;
            if (error != null) {
                statement.setString(3, error);
                index = 4;
            }
            statement.setLong(index, chatId);
            statement.setInt(index + 1, messageId);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.warn("Failed to update status {} for message {}:{}", status, chatId, messageId, e);
        }
    }

    public List<MessageForwardRecord> fetchForRetry(int maxAttempts, Instant cutoff, int limit) {
        String sql = "SELECT source_chat_id, source_message_id, from_user_id, from_username, message_date, text, "
                + "attempt_count FROM message_forward "
                + "WHERE status = ? AND attempt_count < ? AND (last_attempt_at IS NULL OR last_attempt_at < ?) "
                + "ORDER BY last_attempt_at NULLS FIRST LIMIT ?";
        List<MessageForwardRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MessageStatus.FAILED.name());
            statement.setInt(2, maxAttempts);
            statement.setTimestamp(3, Timestamp.from(cutoff));
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(new MessageForwardRecord(
                            resultSet.getLong("source_chat_id"),
                            resultSet.getInt("source_message_id"),
                            resultSet.getLong("from_user_id"),
                            resultSet.getString("from_username"),
                            resultSet.getTimestamp("message_date").toInstant(),
                            resultSet.getString("text"),
                            resultSet.getInt("attempt_count")
                    ));
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch messages for retry", e);
        }
        return records;
    }
}
