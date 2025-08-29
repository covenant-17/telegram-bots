package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for FileNameSanitizer to cover edge-cases
 */
class FileNameSanitizerAdvancedTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should handle very long file names")
    void testVeryLongFileName() {
        String longName = "A".repeat(300) + " - " + "B".repeat(300) + " (Official Video)";
        String result = FileNameSanitizer.sanitize(longName);
        
        assertNotNull(result);
        assertTrue(result.length() < longName.length());
    }

    @Test
    @DisplayName("Should handle file names with only special characters")
    void testOnlySpecialCharacters() {
        String specialChars = "!@#$%^&*()[]{}|\\:;\"'<>?/";
        String result = FileNameSanitizer.sanitize(specialChars);
        
        // Should return empty or cleaned string
        assertTrue(result == null || result.trim().isEmpty() || !result.equals(specialChars));
    }

    @Test
    @DisplayName("Should handle Unicode characters")
    void testUnicodeCharacters() {
        String unicode = "🎵 Music - Song 音楽 موسيقى (Official)";
        String result = FileNameSanitizer.sanitize(unicode);
        
        assertNotNull(result);
        // Should preserve some readable content
        assertTrue(result.length() > 0);
    }    @Test
    @DisplayName("Should handle nested parentheses and brackets")
    void testNestedBrackets() {
        String nested = "Song [Remix (Official) [HD]] (2023)";
        String result = FileNameSanitizer.sanitize(nested);
        
        // After cleaning brackets, parentheses, and "official", should leave "Song 2023"
        assertEquals("Song 2023", result);
    }

    @Test
    @DisplayName("Should handle multiple dashes and spaces")
    void testMultipleDashesSpaces() {
        String multiple = "Artist  -  -  Song  -  Title  -  -  ";
        String result = FileNameSanitizer.sanitize(multiple);
        
        assertEquals("Artist Song Title", result);
    }    @Test
    @DisplayName("Should handle file extension in middle of name")
    void testExtensionInMiddle() {
        String withExt = "Song.mp3.in.the.middle (Official).mp3";
        String result = FileNameSanitizer.sanitize(withExt);
        
        assertTrue(result.contains("Song"));
        // The sanitizer only removes ".mp3" from the end, not from middle
        assertTrue(result.contains(".mp3"));
        assertEquals("Song.mp3.in.the.middle", result);
    }@Test
    @DisplayName("Should compose filename with very long channel name")
    void testVeryLongChannelName() {
        String longChannel = "A".repeat(200);
        String title = "Short Title";
        
        // Note: composeFileName takes (channel, title), not (title, channel)
        String result = FileNameSanitizer.composeFileName(longChannel, title);
        
        assertNotNull(result);
        // Since title doesn't contain channel, result should be "channel - title"
        assertTrue(result.contains(" - "));
        assertTrue(result.contains("Short Title"));
    }

    @Test
    @DisplayName("Should handle sanitizeAllInDirectory with non-existent directory")
    void testSanitizeNonExistentDirectory() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent");
        
        // Should not throw exception
        assertDoesNotThrow(() -> 
            FileNameSanitizer.sanitizeAllInDirectory(nonExistent.getPath(), ".mp3", true)
        );
    }

    @Test
    @DisplayName("Should handle sanitizeAllInDirectory with empty directory")
    void testSanitizeEmptyDirectory() throws IOException {
        File emptyDir = tempDir.toFile();
        
        // Should not throw exception
        assertDoesNotThrow(() -> 
            FileNameSanitizer.sanitizeAllInDirectory(emptyDir.getPath(), ".mp3", true)
        );
    }

    @Test
    @DisplayName("Should handle sanitizeAllInDirectory with files of different extensions")
    void testSanitizeMixedExtensions() throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("test.mp3"));
        Files.createFile(tempDir.resolve("test.wav"));
        Files.createFile(tempDir.resolve("test.txt"));
        
        // Should process only .mp3 files
        assertDoesNotThrow(() -> 
            FileNameSanitizer.sanitizeAllInDirectory(tempDir.toString(), ".mp3", true)
        );
    }    @Test
    @DisplayName("Should handle file names with numbers and years")
    void testNumbersAndYears() {
        String withNumbers = "Track 01 - Artist 2023 Song (2023) [320kbps]";
        String result = FileNameSanitizer.sanitize(withNumbers);
        
        // After sanitization, should remove brackets and parentheses
        assertTrue(result.contains("Track"));
        assertTrue(result.contains("Artist"));
        assertTrue(result.contains("Song"));
        assertTrue(result.contains("01"));
        assertTrue(result.contains("2023"));
        assertFalse(result.contains("["));
        assertFalse(result.contains("]"));
        assertFalse(result.contains("320kbps"));
    }    @Test
    @DisplayName("Should handle HTML entities and URL encoding")
    void testHtmlEntitiesAndUrlEncoding() {
        String encoded = "Song%20Title%20&amp;%20Artist%20-%20%22Official%22";
        String result = FileNameSanitizer.sanitize(encoded);
        
        assertNotNull(result);
        assertFalse(result.contains("%20"));
        assertFalse(result.contains("&amp;"));
        assertFalse(result.contains("\""));
        // Should contain the cleaned content
        assertTrue(result.contains("Song"));
        assertTrue(result.contains("Title"));
        assertTrue(result.contains("Artist"));
    }    @Test
    @DisplayName("Should handle repeated words in title")
    void testRepeatedWords() {
        String repeated = "Song Song Song - Artist Artist (Official Official)";
        String result = FileNameSanitizer.sanitize(repeated);
        
        // Should clean up "Official" words and parentheses but keep repeated names
        assertNotNull(result);
        assertTrue(result.contains("Song"));
        assertTrue(result.contains("Artist"));
        assertFalse(result.contains("Official"));
        assertFalse(result.contains("("));
        assertFalse(result.contains(")"));
    }

    @Test
    @DisplayName("Should handle title with only artist name")
    void testOnlyArtistName() {
        String artist = "Artist Name";
        String result = FileNameSanitizer.sanitize(artist);
        
        assertEquals("Artist Name", result);
    }    @Test
    @DisplayName("Should handle composeFileName with identical title and channel")
    void testIdenticalTitleAndChannel() {
        String name = "Same Name";
        // Note: composeFileName takes (channel, title), not (title, channel)
        String result = FileNameSanitizer.composeFileName(name, name);
        
        // Since title contains channel (same string), should return only title
        assertEquals(name, result);
    }

    @Test
    @DisplayName("Should handle whitespace-only strings")
    void testWhitespaceOnly() {
        String whitespace = "   \t\n\r   ";
        String result = FileNameSanitizer.sanitize(whitespace);
        
        assertEquals("", result);
    }    @Test
    @DisplayName("Should handle mixed case channel names")
    void testMixedCaseChannels() {
        String channel = "CHANNEL name";
        String title = "Song Title";
        
        // Note: composeFileName takes (channel, title), not (title, channel)
        String result = FileNameSanitizer.composeFileName(channel, title);
        
        // Since title doesn't contain channel, should return "channel - title"
        assertTrue(result.contains("Song Title"));
        assertTrue(result.contains("CHANNEL name"));
        assertTrue(result.contains(" - "));
    }
}
