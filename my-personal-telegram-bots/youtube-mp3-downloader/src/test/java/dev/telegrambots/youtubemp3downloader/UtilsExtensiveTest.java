package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

/**
 * Comprehensive tests for Utils class covering all existing methods
 */
class UtilsExtensiveTest {

    @Nested
    class YouTubeUrlValidationTests {
        
        @Test
        void testValidYouTubeUrls() {
            assertTrue(Utils.isValidYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            assertTrue(Utils.isValidYouTubeUrl("https://youtube.com/watch?v=dQw4w9WgXcQ"));
            assertTrue(Utils.isValidYouTubeUrl("http://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            assertTrue(Utils.isValidYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"));
            assertTrue(Utils.isValidYouTubeUrl("youtu.be/dQw4w9WgXcQ"));
        }
        
        @Test
        void testInvalidYouTubeUrls() {
            assertFalse(Utils.isValidYouTubeUrl("https://example.com"));
            assertFalse(Utils.isValidYouTubeUrl("not a url"));
            assertFalse(Utils.isValidYouTubeUrl(""));
            assertFalse(Utils.isValidYouTubeUrl(null));
            assertFalse(Utils.isValidYouTubeUrl("https://youtube.com/watch?v=invalid"));
        }
        
        @Test
        void testVideoIdExtraction() {
            assertEquals("dQw4w9WgXcQ", Utils.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            assertEquals("dQw4w9WgXcQ", Utils.extractVideoId("https://youtu.be/dQw4w9WgXcQ"));
            assertNull(Utils.extractVideoId("https://example.com"));
            assertNull(Utils.extractVideoId("invalid url"));
        }
        
        @Test
        void testEdgeCaseUrls() {
            // URLs with parameters
            assertTrue(Utils.isValidYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"));
            assertEquals("dQw4w9WgXcQ", Utils.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"));
            
            // Mixed case
            assertTrue(Utils.isValidYouTubeUrl("https://www.YouTube.com/watch?v=dQw4w9WgXcQ"));
            
            // Without protocol
            assertTrue(Utils.isValidYouTubeUrl("youtube.com/watch?v=dQw4w9WgXcQ"));
        }
    }
      @Nested
    class PathConfigurationTests {
        
        @Test
        void testYtDlpPathConfiguration() {
            assertDoesNotThrow(() -> {
                String path = Utils.getYtDlpPath();
                assertNotNull(path);
                assertFalse(path.isEmpty());
            });
        }
        
        @Test
        void testFfmpegPathConfiguration() {
            assertDoesNotThrow(() -> {
                String path = Utils.getFfmpegPath();
                assertNotNull(path);
                assertFalse(path.isEmpty());
            });
        }
          @Test
        void testFfprobePathConfiguration() {
            assertDoesNotThrow(() -> {
                String path = Utils.getFfprobePath();
                assertNotNull(path);
                assertFalse(path.isEmpty());
            });
        }
    }
    
    @Nested
    class FileSizeValidationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        void testFileSizeWithinLimit() throws IOException {
            File testFile = tempDir.resolve("test.txt").toFile();
            Files.write(testFile.toPath(), "Hello World".getBytes());
            
            assertTrue(Utils.isFileSizeWithinLimit(testFile, 1000));
            assertFalse(Utils.isFileSizeWithinLimit(testFile, 5));
        }
        
        @Test
        void testFileSizeWithNullFile() {
            assertFalse(Utils.isFileSizeWithinLimit(null, 1000));
        }
        
        @Test
        void testFileSizeWithNonExistentFile() {
            File nonExistent = new File("non-existent-file.txt");
            assertFalse(Utils.isFileSizeWithinLimit(nonExistent, 1000));
        }
        
        @Test
        void testFileSizeBoundaryConditions() throws IOException {
            File zeroFile = tempDir.resolve("zero.txt").toFile();
            Files.write(zeroFile.toPath(), new byte[0]);
            
            // Empty files now always return false (our new logic)
            assertFalse(Utils.isFileSizeWithinLimit(zeroFile, 0));
            assertFalse(Utils.isFileSizeWithinLimit(zeroFile, 1));
            
            File exactFile = tempDir.resolve("exact.txt").toFile();
            Files.write(exactFile.toPath(), "12345".getBytes()); // 5 bytes
            
            assertTrue(Utils.isFileSizeWithinLimit(exactFile, 5));
            assertFalse(Utils.isFileSizeWithinLimit(exactFile, 4));
        }
    }
    
    @Nested
    class DurationValidationTests {
        
        @Test
        void testValidDurationWithinLimit() {
            assertTrue(Utils.isDurationWithinLimit(60.0, 2.0)); // 1 minute within 2 minute limit
            assertTrue(Utils.isDurationWithinLimit(120.0, 2.0)); // exactly 2 minutes
            assertFalse(Utils.isDurationWithinLimit(180.0, 2.0)); // 3 minutes exceeds 2 minute limit
        }
        
        @Test
        void testZeroDuration() {
            assertTrue(Utils.isDurationWithinLimit(0.0, 2.0));
        }
        
        @Test
        void testDurationBoundaryConditions() {
            assertTrue(Utils.isDurationWithinLimit(119.9, 2.0));
            assertTrue(Utils.isDurationWithinLimit(120.0, 2.0));
            assertFalse(Utils.isDurationWithinLimit(120.1, 2.0));
            
            // Test with zero limit
            assertTrue(Utils.isDurationWithinLimit(0.0, 0.0));
            assertFalse(Utils.isDurationWithinLimit(0.1, 0.0));
        }
    }
    
    @Nested
    class FileOperationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        void testDeleteExistingFile() throws IOException {
            File testFile = tempDir.resolve("delete-me.txt").toFile();
            Files.write(testFile.toPath(), "content".getBytes());
            assertTrue(testFile.exists());
            
            Utils.deleteFileIfExists(testFile);
            assertFalse(testFile.exists());
        }
        
        @Test
        void testDeleteNonExistentFile() {
            File nonExistent = tempDir.resolve("non-existent.txt").toFile();
            assertFalse(nonExistent.exists());
            
            // Should not throw exception
            assertDoesNotThrow(() -> Utils.deleteFileIfExists(nonExistent));
        }
        
        @Test
        void testDeleteNullFile() {
            // Should not throw exception
            assertDoesNotThrow(() -> Utils.deleteFileIfExists(null));
        }
        
        @Test
        void testDeleteReadOnlyFile() throws IOException {
            File readOnlyFile = tempDir.resolve("readonly.txt").toFile();
            Files.write(readOnlyFile.toPath(), "content".getBytes());
            readOnlyFile.setReadOnly();
            
            // Should attempt deletion without throwing exception
            assertDoesNotThrow(() -> Utils.deleteFileIfExists(readOnlyFile));
        }
    }
    
    @Nested
    class IntegrationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        void testFullWorkflowWithYouTubeUrl() {
            String url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            
            // Should validate URL
            assertTrue(Utils.isValidYouTubeUrl(url));
            
            // Should extract video ID
            String videoId = Utils.extractVideoId(url);
            assertEquals("dQw4w9WgXcQ", videoId);
            
            // Paths should be configured
            assertNotNull(Utils.getYtDlpPath());
            assertNotNull(Utils.getFfmpegPath());
            assertNotNull(Utils.getFfprobePath());
        }
        
        @Test
        void testFileOperationsWorkflow() throws IOException {
            // Create test file
            File testFile = tempDir.resolve("workflow.txt").toFile();
            Files.write(testFile.toPath(), "Test content for workflow".getBytes());
            
            // Check size
            assertTrue(Utils.isFileSizeWithinLimit(testFile, 1000));
            
            // Delete file
            Utils.deleteFileIfExists(testFile);
            assertFalse(testFile.exists());
            
            // Check size of non-existent file
            assertFalse(Utils.isFileSizeWithinLimit(testFile, 1000));
        }
          @Test
        void testDurationValidationWorkflow() {
            double[] durations = {30.0, 60.0, 120.0, 180.0, 300.0};
            double limit = 2.5; // 2.5 minutes
            
            boolean[] expected = {true, true, true, false, false};
            
            for (int i = 0; i < durations.length; i++) {
                assertEquals(expected[i], Utils.isDurationWithinLimit(durations[i], limit),
                    "Duration " + durations[i] + " should be " + (expected[i] ? "within" : "exceeding") + " limit");
            }
        }
    }
}
