package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

final class BotCommandCatalog {
    private BotCommandCatalog() {
    }

    static List<BotCommand> commands() {
        return List.of(
                new BotCommand("start", "Show bot greeting and log sender info"),
                new BotCommand("cut", "Trim audio from an attached file or a YouTube link"),
                new BotCommand("sanitize_mp3", "Preview or sanitize MP3 filenames in the workzone"),
                new BotCommand("delete_mp3", "Delete all MP3 files in the workzone")
        );
    }
}
