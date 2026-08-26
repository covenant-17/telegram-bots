package dev.telegrambots.managerbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void managerStartCommandSelectsItsOwnExternalConfig() {
        String command = AppRegistry.get("manager-bot").startCommand;

        assertTrue(command.startsWith("env BOT_CONFIG_PATH="));
        assertTrue(command.contains("/manager-bot/src/main/resources/config.properties"));
        assertTrue(command.endsWith("java -jar /data/data/com.termux/files/home/termuxserver/src/manager-bot-1.0-SNAPSHOT-jar-with-dependencies.jar"));
    }

    @Test
    void converterStartCommandOverridesInheritedManagerConfig() {
        String command = AppRegistry.get("converter-bot").startCommand;

        assertTrue(command.startsWith("env BOT_CONFIG_PATH="));
        assertTrue(command.contains("/converter-bot/src/main/resources/config.properties"));
        assertFalse(command.contains("/manager-bot/src/main/resources/config.properties"));
    }
}
