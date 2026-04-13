package dev.telegrambots.managerbot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of all managed applications.
 * Paths are Termux absolute paths (expand ~ manually where needed).
 *
 * To add a new app: add a new entry in the static initializer below.
 */
public class AppRegistry {

    private static final String HOME = "/data/data/com.termux/files/home";
    private static final String LOG_BASE = HOME + "/termuxserver/src/sh/logs";

    private static final Map<String, AppDefinition> APPS = new LinkedHashMap<>();

    static {
        APPS.put("converter-bot", new AppDefinition(
                "converter-bot",
                HOME + "/repos/telegram-bots",
                "my-personal-telegram-bots/converter-bot",
                "my-personal-telegram-bots/shared-config",
                HOME + "/termuxserver/src/converter-bot-1.0-SNAPSHOT-jar-with-dependencies.jar",
                LOG_BASE + "/converter-bot.log",
                LOG_BASE + "/converter-bot-error.log",
                "converter-bot",
                "git@github.com:covenant-17/telegram-bots.git"
        ));

        APPS.put("youtube-mp3-downloader", new AppDefinition(
                "youtube-mp3-downloader",
                HOME + "/repos/telegram-bots",
                "my-personal-telegram-bots/youtube-mp3-downloader",
                null,
                HOME + "/termuxserver/src/youtube-mp3-downloader-1.0-SNAPSHOT-jar-with-dependencies.jar",
                LOG_BASE + "/youtube-mp3-downloader.log",
                LOG_BASE + "/youtube-mp3-downloader-error.log",
                "youtube-mp3-downloader",
                "git@github.com:covenant-17/telegram-bots.git"
        ));

        APPS.put("trace-keeper", new AppDefinition(
                "trace-keeper",
                HOME + "/repos/trace-keeper",
                "",
                null,
                HOME + "/termuxserver/src/trace-keeper-1.0.0-SNAPSHOT.jar",
                LOG_BASE + "/trace-keeper.log",
                LOG_BASE + "/trace-keeper-error.log",
                "trace-keeper",
                "git@github.com:covenant-17/trace-keeper.git"
        ));

        APPS.put("manager-bot", new AppDefinition(
                "manager-bot",
                HOME + "/repos/telegram-bots",
                "my-personal-telegram-bots/manager-bot",
                "my-personal-telegram-bots/shared-config",
                HOME + "/termuxserver/src/manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar",
                LOG_BASE + "/manager-bot.log",
                LOG_BASE + "/manager-bot-error.log",
                "manager-bot",
                "git@github.com:covenant-17/telegram-bots.git"
        ));
    }

    public static AppDefinition get(String name) {
        return APPS.get(name.toLowerCase());
    }

    public static Map<String, AppDefinition> all() {
        return APPS;
    }

    public static boolean exists(String name) {
        return APPS.containsKey(name.toLowerCase());
    }
}
