package dev.telegrambots.managerbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerBotTest {

    @Test
    void sharexUsesStandaloneRepositoryAndPersistentConfig() {
        AppDefinition app = AppRegistry.get("sharex-server");
        assertTrue(AppRegistry.exists("sharex-server"));
        assertEquals("", app.buildSubPath);
        assertEquals("git@github.com:covenant-17/sharex-server.git", app.repoUrl);
        assertEquals("mvn clean verify -B -q", app.buildCommand);
        assertTrue(app.startCommand.contains("SHAREX_CONFIG_PATH="));
        assertTrue(app.startCommand.contains("/sharex-server/config.properties"));
        assertTrue(app.startCommand.endsWith("java -jar " + app.jarPath));
        assertFalse(app.startCommand.contains("BOT_CONFIG_PATH"));
    }

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
