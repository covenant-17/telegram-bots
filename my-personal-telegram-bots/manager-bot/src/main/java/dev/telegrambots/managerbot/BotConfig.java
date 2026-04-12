package dev.telegrambots.managerbot;

import dev.telegrambots.shared.BaseBotConfig;

public class BotConfig extends BaseBotConfig {
    public final String botToken;
    public final String botUsername;
    /** Comma-separated list of allowed Telegram user IDs (e.g. "123456789,987654321") */
    public final long[] allowedUserIds;

    public BotConfig() {
        super();
        this.botToken = config.getString("bot.token");
        this.botUsername = config.getString("bot.username");
        this.allowedUserIds = parseUserIds(config.getString("allowed.user.ids"));
    }

    private long[] parseUserIds(String raw) {
        String[] parts = raw.split(",");
        long[] ids = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Long.parseLong(parts[i].trim());
        }
        return ids;
    }

    public boolean isAllowed(long userId) {
        for (long id : allowedUserIds) {
            if (id == userId) return true;
        }
        return false;
    }
}
