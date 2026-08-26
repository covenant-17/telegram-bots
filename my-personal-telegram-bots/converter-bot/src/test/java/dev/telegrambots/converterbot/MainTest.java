package dev.telegrambots.converterbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsBotTokenFromExternalConfig() throws Exception {
        Path config = tempDir.resolve("converter-bot.properties");
        Files.writeString(config, "bot.token=test-token\n");

        String previousConfigPath = System.getProperty("bot.config.path");
        try {
            System.setProperty("bot.config.path", config.toString());
            assertEquals("test-token", ConverterBot.getBotTokenFromConfig());
        } finally {
            if (previousConfigPath == null) {
                System.clearProperty("bot.config.path");
            } else {
                System.setProperty("bot.config.path", previousConfigPath);
            }
        }
    }
}
