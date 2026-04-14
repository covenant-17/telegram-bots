package dev.telegrambots.groupforward.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BlacklistDao {
    private static final Logger logger = LoggerFactory.getLogger(BlacklistDao.class);

    private final DataSource dataSource;

    public BlacklistDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isBlacklisted(long userId) {
        String sql = "SELECT 1 FROM blacklist WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            logger.warn("Failed to check blacklist for user {}", userId, e);
            return false;
        }
    }

    public void add(long userId, String reason, long createdBy) {
        String sql = "INSERT INTO blacklist (user_id, reason, created_at, created_by) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (user_id) DO UPDATE SET reason = EXCLUDED.reason";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, reason);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setLong(4, createdBy);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.warn("Failed to add user {} to blacklist", userId, e);
        }
    }

    public void remove(long userId) {
        String sql = "DELETE FROM blacklist WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.warn("Failed to remove user {} from blacklist", userId, e);
        }
    }

    public List<String> list() {
        String sql = "SELECT user_id, reason, created_at FROM blacklist ORDER BY created_at DESC";
        List<String> entries = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long userId = resultSet.getLong("user_id");
                String reason = resultSet.getString("reason");
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                String entry = userId + " | " + createdAt + " | " + (reason == null ? "" : reason);
                entries.add(entry);
            }
        } catch (Exception e) {
            logger.warn("Failed to list blacklist", e);
        }
        return entries;
    }
}
