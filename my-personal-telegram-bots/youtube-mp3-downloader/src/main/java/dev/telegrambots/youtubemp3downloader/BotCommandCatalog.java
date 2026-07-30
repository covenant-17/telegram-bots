package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

final class BotCommandCatalog {
    private BotCommandCatalog() {
    }

    static List<BotCommand> commands() {
        return List.of(
                new BotCommand("start", "Show bot greeting and log sender info"),
                new BotCommand("cut", "Trim an attached audio file: /cut 0:00 2:50"),
                new BotCommand("sanitize_mp3", "Preview or sanitize MP3 filenames in the workzone"),
                new BotCommand("delete_mp3", "Sanitize MP3 filenames in the workzone")
        );
    }
}
