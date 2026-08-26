package dev.telegrambots.converterbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void recognizesSupportedTelegramMediaRepresentations() {
        assertEquals("clip.WEBM", ConverterBot.supportedFileName("clip.WEBM", null, null));
        assertEquals("telegram-upload.webm", ConverterBot.supportedFileName(null, "video/webm", null));
        assertEquals("telegram-upload.gif", ConverterBot.supportedFileName(null, "video/mp4", ".gif"));
        assertNull(ConverterBot.supportedFileName("clip.mp4", "video/mp4", null));
    }

    @Test
    void recognizesMp4ByFileNameOrMimeType() {
        assertTrue(ConverterBot.isMp4("clip.MP4", null));
        assertTrue(ConverterBot.isMp4(null, "video/mp4"));
        assertFalse(ConverterBot.isMp4("clip.webm", "video/webm"));
        assertFalse(ConverterBot.isMp4("clip.webm", "video/mp4"));
    }
}
