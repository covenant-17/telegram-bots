package dev.telegrambots.groupforward;

import dev.telegrambots.shared.EnvConfig;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class AppConfig {
    private final String botToken;
    private final String botUsername;
    private final long adminChatId;
    private final Long adminUserId;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;
    private final int retryMaxAttempts;
    private final Duration retryDelay;
    private final int retryBatchSize;

    private AppConfig(String botToken,
                      String botUsername,
                      long adminChatId,
                      Long adminUserId,
                      String dbUrl,
                      String dbUser,
                      String dbPass,
                      int retryMaxAttempts,
                      Duration retryDelay,
                      int retryBatchSize) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.adminChatId = adminChatId;
        this.adminUserId = adminUserId;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelay = retryDelay;
        this.retryBatchSize = retryBatchSize;
    }

    public static AppConfig load() {
        JSONObject json = loadOptionalJson();
        String botToken = EnvConfig.getRequired("BOT_TOKEN");
        String botUsername = EnvConfig.getOptional("BOT_USERNAME")
                .orElseGet(() -> json.optString("BOT_USERNAME", ""));
        long adminChatId = EnvConfig.getRequiredLong("ADMIN_CHAT_ID");
        Long adminUserId = EnvConfig.getOptionalLong("ADMIN_USER_ID")
                .orElseGet(() -> json.has("ADMIN_USER_ID") ? json.getLong("ADMIN_USER_ID") : null);
        String dbUrl = EnvConfig.getRequired("DB_URL");
        String dbUser = EnvConfig.getRequired("DB_USER");
        String dbPass = EnvConfig.getRequired("DB_PASS");
        int retryMaxAttempts = json.optInt("RETRY_MAX_ATTEMPTS", 3);
        int retryDelaySeconds = json.optInt("RETRY_DELAY_SECONDS", 30);
        int retryBatchSize = json.optInt("RETRY_BATCH_SIZE", 50);

        return new AppConfig(
                botToken,
                botUsername,
                adminChatId,
                adminUserId,
                dbUrl,
                dbUser,
                dbPass,
                retryMaxAttempts,
                Duration.ofSeconds(retryDelaySeconds),
                retryBatchSize
        );
    }

    private static JSONObject loadOptionalJson() {
        String configPath = System.getenv("CONFIG_JSON");
        if (configPath == null || configPath.isBlank()) {
            return new JSONObject();
        }
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("CONFIG_JSON path not found: " + path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return new JSONObject(new JSONTokener(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CONFIG_JSON: " + path, e);
        }
    }

    public String botToken() {
        return botToken;
    }

    public String botUsername() {
        return botUsername;
    }

    public long adminChatId() {
        return adminChatId;
    }

    public Long adminUserId() {
        return adminUserId;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPass() {
        return dbPass;
    }

    public int retryMaxAttempts() {
        return retryMaxAttempts;
    }

    public Duration retryDelay() {
        return retryDelay;
    }

    public int retryBatchSize() {
        return retryBatchSize;
    }
}
