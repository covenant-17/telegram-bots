package dev.telegrambots.groupforward;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.telegrambots.groupforward.dao.BlacklistDao;
import dev.telegrambots.groupforward.dao.MessageDao;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.dbUrl());
        hikariConfig.setUsername(config.dbUser());
        hikariConfig.setPassword(config.dbPass());

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        Flyway.configure().dataSource(dataSource).load().migrate();

        BlacklistDao blacklistDao = new BlacklistDao(dataSource);
        MessageDao messageDao = new MessageDao(dataSource);

        GroupForwardBot bot = new GroupForwardBot(config, blacklistDao, messageDao);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        RetryWorker retryWorker = new RetryWorker(config, blacklistDao, messageDao, bot);
        scheduler.scheduleAtFixedRate(retryWorker, 10, 10, TimeUnit.SECONDS);

        logger.info("GroupForwardBot started. Admin chat id: {}", config.adminChatId());
    }
}
