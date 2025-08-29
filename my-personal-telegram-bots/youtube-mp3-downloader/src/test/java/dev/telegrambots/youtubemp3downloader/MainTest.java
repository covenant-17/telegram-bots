package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void testMain() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
