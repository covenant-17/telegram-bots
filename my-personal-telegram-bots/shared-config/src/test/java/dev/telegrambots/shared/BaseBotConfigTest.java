package dev.telegrambots.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseBotConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void classpathDirectoryConfigOverridesBundledConfigResource() throws Exception {
        Path externalConfig = tempDir.resolve("config.properties");
        String originalClassPath = System.getProperty("java.class.path");

        Files.writeString(externalConfig, "source=external\n");
        try {
            System.setProperty("java.class.path", tempDir + java.io.File.pathSeparator + originalClassPath);
            ResourceBundle config = BaseBotConfig.loadConfig();

            assertEquals("external", config.getString("source"));
        } finally {
            System.setProperty("java.class.path", originalClassPath);
        }
    }
}
