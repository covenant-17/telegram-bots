package dev.telegrambots.managerbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ManagerBotTest {

    @Test
    void repositorySyncUsesCanonicalUpstreamWithoutDeletingLocalConfig() {
        String command = ManagerBot.repositorySyncCommand();

        assertEquals(
                "git fetch --prune origin"
                        + " && upstream=$(git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}')"
                        + " && git reset --hard \"$upstream\"",
                command
        );
        assertFalse(command.contains("git clean"));
        assertFalse(command.contains("git pull"));
    }
}
