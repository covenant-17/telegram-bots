package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotCommandCatalogTest {
    @Test
    @DisplayName("Should expose supported commands for Telegram slash menu")
    void shouldExposeSupportedCommandsForTelegramSlashMenu() {
        List<BotCommand> commands = BotCommandCatalog.commands();

        Set<String> commandNames = commands.stream()
                .map(BotCommand::getCommand)
                .collect(Collectors.toSet());

        assertEquals(Set.of("start", "cut", "sanitize_mp3", "delete_mp3"), commandNames);
        assertEquals(commandNames.size(), commands.size());
        for (BotCommand command : commands) {
            assertTrue(command.getCommand().matches("[a-z0-9_]{1,32}"));
            assertFalse(command.getDescription().isBlank());
        }
    }
}
