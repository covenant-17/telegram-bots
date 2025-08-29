package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Test for enhanced file size validation with empty file detection
 */
public class EmptyFileHandlingTest {

    private final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    @TempDir
    Path tempDir;

    @Test
    public void testEmptyFileDetection() throws IOException {
        // Create empty file
        File emptyFile = tempDir.resolve("empty.mp3").toFile();
        emptyFile.createNewFile();
        
        // Should return false for empty file
        assertFalse(Utils.isFileSizeWithinLimit(emptyFile, MAX_FILE_SIZE));
    }

    @Test
    public void testValidFileSize() throws IOException {
        // Create file with valid content
        File validFile = tempDir.resolve("valid.mp3").toFile();
        try (FileWriter writer = new FileWriter(validFile)) {
            writer.write("Some audio content that makes this file non-empty");
        }
        
        // Should return true for non-empty file within limits
        assertTrue(Utils.isFileSizeWithinLimit(validFile, MAX_FILE_SIZE));
    }

    @Test
    public void testOversizedFile() throws IOException {
        // Create file larger than limit
        File oversizedFile = tempDir.resolve("oversized.mp3").toFile();
        try (FileWriter writer = new FileWriter(oversizedFile)) {
            // Write more than 50MB of data (simplified for test)
            for (int i = 0; i < MAX_FILE_SIZE + 1000; i++) {
                writer.write("X");
            }
        }
        
        // Should return false for file exceeding size limit
        assertFalse(Utils.isFileSizeWithinLimit(oversizedFile, MAX_FILE_SIZE));
    }

    @Test
    public void testNonExistentFile() {
        File nonExistentFile = tempDir.resolve("nonexistent.mp3").toFile();
        
        // Should return false for non-existent file
        assertFalse(Utils.isFileSizeWithinLimit(nonExistentFile, MAX_FILE_SIZE));
    }
}
