package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileNameSanitizerTest {
    @Test
    void testSanitizeRemovesGarbage() {
        assertEquals("Zillakami Lemon Juice", FileNameSanitizer.sanitize("ZillaKami - LEMON JUICE (Official Audio) (z2.fm) [320 kbps] #039;.mp3"));
        assertEquals("Lemon Juice", FileNameSanitizer.sanitize("LEMON JUICE (Official Audio)"));
        assertEquals("Artist Song Title", FileNameSanitizer.sanitize("Artist - Song Title (Official Music Video)"));
        assertEquals("Track 01", FileNameSanitizer.sanitize("Track 01 (lyric video)"));
        assertEquals("Hello World", FileNameSanitizer.sanitize("Hello & World #039;"));
    }

    @Test
    void testSanitizeHandlesNullAndEmpty() {
        assertNull(FileNameSanitizer.sanitize(null));
        assertEquals("", FileNameSanitizer.sanitize("   "));
    }

    @Test
    void testComposeFileNameNoDuplicateChannel() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        String title = FileNameSanitizer.sanitize("ZillaKami - LEMON JUICE");
        assertEquals("Zillakami Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameWithChannel() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        String title = FileNameSanitizer.sanitize("LEMON JUICE");
        assertEquals("Zillakami - Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameChannelBlank() {
        String channel = FileNameSanitizer.sanitize("");
        String title = FileNameSanitizer.sanitize("LEMON JUICE");
        assertEquals("Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameTitleNull() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        assertEquals("Zillakami", FileNameSanitizer.composeFileName(channel, null));
    }

    @Test
    void testSanitizeRemovesUnderscores() {
        assertEquals("Test Artist Song", FileNameSanitizer.sanitize("Test_Artist_Song_Premiere_2025.mp3"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test__Name"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test___Name"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test_Name"));
    }
}
