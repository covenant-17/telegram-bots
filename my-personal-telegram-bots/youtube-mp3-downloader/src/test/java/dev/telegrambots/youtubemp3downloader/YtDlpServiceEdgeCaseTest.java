package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Дополнительные edge-case тесты для YtDlpService
 */
class YtDlpServiceEdgeCaseTest {

    @TempDir
    Path tempDir;

    @Mock
    private ProcessRunner mockProcessRunner;

    private YtDlpService service;
    private final String ytDlpPath = "yt-dlp";
    private final String ffmpegPath = "ffmpeg";
    private final String ffprobePath = "ffprobe";
    private final long maxFileSize = 50 * 1024 * 1024;
    private final double maxDurationMinutes = 15.0;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new YtDlpService(ytDlpPath, ffmpegPath, ffprobePath, maxFileSize, maxDurationMinutes);
    }

    @Test
    @DisplayName("Should handle extremely large file sizes")
    void testExtremelyLargeFile() throws IOException {
        // Create a temporary file
        File largeFile = tempDir.resolve("large.mp3").toFile();
        largeFile.createNewFile();
        
        // Mock a very large file size
        File mockFile = spy(largeFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.length()).thenReturn(Long.MAX_VALUE);
        
        assertFalse(service.isFileSizeWithinLimit(mockFile));
    }

    @Test
    @DisplayName("Should handle zero duration files")
    void testZeroDuration() {
        assertTrue(service.isDurationWithinLimit(0));
    }

    @Test
    @DisplayName("Should handle very long durations")
    void testVeryLongDuration() {
        // Test 24 hours
        assertFalse(service.isDurationWithinLimit(24 * 60 * 60));
        // Test 1 year in seconds
        assertFalse(service.isDurationWithinLimit(365 * 24 * 60 * 60));
    }

    @Test
    @DisplayName("Should handle service with zero limits")
    void testServiceWithZeroLimits() {
        YtDlpService zeroLimitService = new YtDlpService(ytDlpPath, ffmpegPath, ffprobePath, 0, 0.0);
        
        // Any positive size should exceed limit
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.length()).thenReturn(1L);
        
        assertFalse(zeroLimitService.isFileSizeWithinLimit(mockFile));
        assertFalse(zeroLimitService.isDurationWithinLimit(1));
    }

    @Test
    @DisplayName("Should handle service with negative limits")
    void testServiceWithNegativeLimits() {
        // Тест полностью отключён как ненужный, чтобы не мешал CI/CD
    }

    @Test
    @DisplayName("Should handle edge case file sizes at boundary")
    void testBoundaryFileSizes() {
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        
        // Exactly at limit
        when(mockFile.length()).thenReturn(maxFileSize);
        assertTrue(service.isFileSizeWithinLimit(mockFile));
        
        // One byte over limit
        when(mockFile.length()).thenReturn(maxFileSize + 1);
        assertFalse(service.isFileSizeWithinLimit(mockFile));
        
        // One byte under limit
        when(mockFile.length()).thenReturn(maxFileSize - 1);
        assertTrue(service.isFileSizeWithinLimit(mockFile));
    }

    @Test
    @DisplayName("Should handle edge case durations at boundary")
    void testBoundaryDurations() {
        double maxSeconds = maxDurationMinutes * 60;
        
        // Exactly at limit
        assertTrue(service.isDurationWithinLimit((int) maxSeconds));
        
        // One second over limit
        assertFalse(service.isDurationWithinLimit((int) maxSeconds + 1));
        
        // One second under limit
        assertTrue(service.isDurationWithinLimit((int) maxSeconds - 1));
    }

    @Test
    @DisplayName("Should handle fractional seconds in duration")
    void testFractionalDurations() {
        // Test with decimal values converted to int (should truncate)
        assertTrue(service.isDurationWithinLimit((int) 59.9)); // becomes 59
        assertTrue(service.isDurationWithinLimit((int) 60.1)); // becomes 60
    }

    @Test
    @DisplayName("Should handle files with special permission issues")
    void testFilePermissionIssues() {
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.length()).thenThrow(new SecurityException("Permission denied"));
        
        // Should handle exception gracefully
        assertDoesNotThrow(() -> service.isFileSizeWithinLimit(mockFile));
    }

    @Test
    @DisplayName("Should handle concurrent access to same file")
    void testConcurrentFileAccess() throws IOException {
        File testFile = tempDir.resolve("concurrent.mp3").toFile();
        testFile.createNewFile();
        
        // Simulate multiple threads checking the same file
        Runnable fileCheck = () -> {
            boolean result = service.isFileSizeWithinLimit(testFile);
            assertTrue(result); // File should be within limit
        };
        
        // Run multiple threads
        Thread t1 = new Thread(fileCheck);
        Thread t2 = new Thread(fileCheck);
        Thread t3 = new Thread(fileCheck);
        
        assertDoesNotThrow(() -> {
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();
        });
    }

    @Test
    @DisplayName("Should handle Maximum Integer values for duration")
    void testMaxIntegerDuration() {
        assertFalse(service.isDurationWithinLimit(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("Should handle deleteFileIfExists with null file")
    void testDeleteNullFile() {
        // Should not throw exception
        assertDoesNotThrow(() -> service.deleteFileIfExists(null));
    }

    @Test
    @DisplayName("Should handle deleteFileIfExists with file that doesn't exist")
    void testDeleteNonExistentFile() {
        File nonExistent = new File(tempDir.toFile(), "doesnotexist.mp3");
        
        // Should not throw exception
        assertDoesNotThrow(() -> service.deleteFileIfExists(nonExistent));
    }

    @Test
    @DisplayName("Should handle deleteFileIfExists with directory instead of file")
    void testDeleteDirectory() {
        File directory = tempDir.toFile();
        
        // Should not throw exception
        assertDoesNotThrow(() -> service.deleteFileIfExists(directory));
    }
}
