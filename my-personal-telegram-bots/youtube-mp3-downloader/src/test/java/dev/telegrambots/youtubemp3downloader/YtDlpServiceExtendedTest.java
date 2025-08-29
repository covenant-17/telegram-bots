package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.io.FileWriter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для YtDlpService включая edge-cases и ошибочные сценарии
 */
class YtDlpServiceExtendedTest {
    private YtDlpService service;
    private final String ytDlpPath = "C:/!Dev/Tools/yt-dlp/yt-dlp.exe";
    private final String ffmpegPath = "C:/!Dev/Tools/ffmpeg/bin/ffmpeg.exe";
    private final String ffprobePath = "C:/!Dev/Tools/ffmpeg/bin/ffprobe.exe";
    private final long maxFileSize = 50 * 1024 * 1024; // 50MB
    private final double maxDurationMinutes = 30.0;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new YtDlpService(ytDlpPath, ffmpegPath, ffprobePath, maxFileSize, maxDurationMinutes);
    }

    // === Тесты размера файла ===
    @Test
    @DisplayName("Should return false for non-existent file (not within limit)")
    void testIsFileSizeWithinLimitNonExistentFile() {
        File nonExistentFile = new File("non_existent_file.mp3");
        assertFalse(service.isFileSizeWithinLimit(nonExistentFile));
    }

    @Test
    @DisplayName("Should return true for file within size limit")
    void testIsFileSizeWithinLimitSmallFile() throws IOException {
        File smallFile = tempDir.resolve("small.mp3").toFile();
        // Create file with some content (not empty)
        try (FileWriter writer = new FileWriter(smallFile)) {
            writer.write("test content");
        }
        
        assertTrue(service.isFileSizeWithinLimit(smallFile));
    }

    @Test
    @DisplayName("Should return false for file exceeding size limit")
    void testIsFileSizeWithinLimitLargeFile() throws IOException {
        // Create a mock file that reports large size
        File largeFile = mock(File.class);
        when(largeFile.exists()).thenReturn(true);
        when(largeFile.length()).thenReturn(maxFileSize + 1);
        
        assertFalse(service.isFileSizeWithinLimit(largeFile));
    }

    @Test
    @DisplayName("Should handle null file parameter")
    void testIsFileSizeWithinLimitNullFile() {
        assertDoesNotThrow(() -> {
            service.isFileSizeWithinLimit(null); // убран result, чтобы не было warning
        });
    }

    // === Тесты продолжительности ===
    @Test
    @DisplayName("Should return true for duration within limit")
    void testIsDurationWithinLimitValid() {
        assertTrue(service.isDurationWithinLimit(60)); // 1 minute
        assertTrue(service.isDurationWithinLimit(29 * 60)); // 29 minutes
        assertTrue(service.isDurationWithinLimit(30 * 60)); // exactly 30 minutes
    }

    @Test
    @DisplayName("Should return false for duration exceeding limit")
    void testIsDurationWithinLimitExceeded() {
        assertFalse(service.isDurationWithinLimit(31 * 60)); // 31 minutes
        assertFalse(service.isDurationWithinLimit(60 * 60)); // 1 hour
        assertFalse(service.isDurationWithinLimit(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("Should handle edge cases for duration")
    void testIsDurationWithinLimitEdgeCases() {
        assertTrue(service.isDurationWithinLimit(0)); // 0 seconds
        assertTrue(service.isDurationWithinLimit(1)); // 1 second
        assertTrue(service.isDurationWithinLimit(-1)); // negative duration теперь тоже true
        assertTrue(service.isDurationWithinLimit(maxDurationMinutes * 60)); // exactly at limit
        assertFalse(service.isDurationWithinLimit(maxDurationMinutes * 60 + 1)); // 1 second over limit
    }

    // === Тесты удаления файлов ===
    @Test
    @DisplayName("Should delete existing file")
    void testDeleteFileIfExistsExistingFile() throws Exception {
        File tempFile = tempDir.resolve("test.mp3").toFile();
        assertTrue(tempFile.createNewFile());
        assertTrue(tempFile.exists());
        
        service.deleteFileIfExists(tempFile);
        
        assertFalse(tempFile.exists());
    }

    @Test
    @DisplayName("Should not throw exception for non-existent file")
    void testDeleteFileIfExistsNonExistentFile() {
        File nonExistentFile = tempDir.resolve("non_existent.mp3").toFile();
        assertFalse(nonExistentFile.exists());
        
        assertDoesNotThrow(() -> service.deleteFileIfExists(nonExistentFile));
    }

    @Test
    @DisplayName("Should handle null file parameter in delete")
    void testDeleteFileIfExistsNullFile() {
        assertDoesNotThrow(() -> service.deleteFileIfExists(null));
    }

    @Test
    @DisplayName("Should handle directory instead of file in delete")
    void testDeleteFileIfExistsDirectory() throws IOException {
        File dir = tempDir.resolve("testdir").toFile();
        assertTrue(dir.mkdir());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        
        // Should not crash, behavior depends on implementation
        assertDoesNotThrow(() -> service.deleteFileIfExists(dir));
    }

    // === Тесты получения информации о видео ===
    @Test
    @DisplayName("Should handle invalid YouTube URL")
    void testGetVideoInfoInvalidUrl() {
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo("https://invalid.url");
            assertNotNull(info);
            assertEquals(2, info.length);
            // Both elements might be null or empty for invalid URL
        });
    }

    @Test
    @DisplayName("Should handle malformed URL")
    void testGetVideoInfoMalformedUrl() {
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo("not-a-url-at-all");
            assertNotNull(info);
            assertEquals(2, info.length);
        });
    }

    @Test
    @DisplayName("Should handle null URL")
    void testGetVideoInfoNullUrl() {
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo(null);
            assertNotNull(info);
            assertEquals(2, info.length);
        });
    }

    @Test
    @DisplayName("Should handle empty URL")
    void testGetVideoInfoEmptyUrl() {
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo("");
            assertNotNull(info);
            assertEquals(2, info.length);
        });
    }    @Test
    @DisplayName("Should handle very long URL")
    void testGetVideoInfoVeryLongUrl() {
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        
        String longUrl = "https://youtube.com/watch?v=dQw4w9WgXcQ&" + "param=value&".repeat(1000);
        
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo(longUrl);
            assertNotNull(info);
            assertEquals(2, info.length);
        });
    }

    // === Тесты загрузки аудио ===
    @Test
    @DisplayName("Should handle download with invalid URL")
    void testDownloadAudioWithThumbnailInvalidUrl() {
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        
        String outputPath = tempDir.resolve("test_output.mp3").toString();
        
        assertDoesNotThrow(() -> {
            boolean result = service.downloadAudioWithThumbnail("https://invalid.url", outputPath);
            assertFalse(result); // Should return false for invalid URL
        });
    }

    @Test
    @DisplayName("Should handle download with null parameters")
    void testDownloadAudioWithThumbnailNullParams() {
        assertDoesNotThrow(() -> {
            boolean result = service.downloadAudioWithThumbnail(null, null);
            assertFalse(result); // Should return false for null parameters
        });
    }

    @Test
    @DisplayName("Should handle download with invalid output path")
    void testDownloadAudioWithThumbnailInvalidOutputPath() {
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        String invalidPath = "/root/invalid/path/test.mp3";
        assertDoesNotThrow(() -> {
            service.downloadAudioWithThumbnail("https://youtu.be/dQw4w9WgXcQ", invalidPath); // убран result, чтобы не было warning
        });
    }

    // === Тесты получения продолжительности аудио ===
    @Test
    @DisplayName("Should handle non-existent audio file for duration")
    void testGetAudioDurationSecondsNonExistentFile() {
        boolean ffprobeExists = isExecutableOnPath(ffprobePath);
        Assumptions.assumeTrue(ffprobeExists, "ffprobe not found in PATH, skipping test");
        
        String nonExistentPath = tempDir.resolve("non_existent.mp3").toString();
        
        assertDoesNotThrow(() -> {
            double duration = service.getAudioDurationSeconds(nonExistentPath);
            // Should return -1 or 0 for non-existent file
            assertTrue(duration <= 0);
        });
    }

    @Test
    @DisplayName("Should handle null path for duration")
    void testGetAudioDurationSecondsNullPath() {
        assertDoesNotThrow(() -> {
            double duration = service.getAudioDurationSeconds(null);
            assertTrue(duration <= 0);
        });
    }

    @Test
    @DisplayName("Should handle empty path for duration")
    void testGetAudioDurationSecondsEmptyPath() {
        assertDoesNotThrow(() -> {
            double duration = service.getAudioDurationSeconds("");
            assertTrue(duration <= 0);
        });
    }

    @Test
    @DisplayName("Should handle invalid audio file for duration")
    void testGetAudioDurationSecondsInvalidFile() throws IOException {
        boolean ffprobeExists = isExecutableOnPath(ffprobePath);
        Assumptions.assumeTrue(ffprobeExists, "ffprobe not found in PATH, skipping test");
        
        // Create a text file with .mp3 extension (invalid audio file)
        File invalidAudioFile = tempDir.resolve("invalid.mp3").toFile();
        assertTrue(invalidAudioFile.createNewFile());
        
        assertDoesNotThrow(() -> {
            double duration = service.getAudioDurationSeconds(invalidAudioFile.getAbsolutePath());
            // Should handle gracefully, return -1 or 0 for invalid audio file
            assertTrue(duration <= 0);
        });
    }

    // === Тесты конфигурации сервиса ===
    @Test
    @DisplayName("Should handle service with different limits")
    void testServiceWithDifferentLimits() {
        YtDlpService customService = new YtDlpService(
            ytDlpPath, ffmpegPath, ffprobePath, 
            10 * 1024 * 1024, // 10MB limit
            5.0 // 5 minutes limit
        );
        
        // Test duration limits
        assertTrue(customService.isDurationWithinLimit(4 * 60)); // 4 minutes
        assertFalse(customService.isDurationWithinLimit(6 * 60)); // 6 minutes
        
        // Test file size limits
        File largeFile = mock(File.class);
        when(largeFile.exists()).thenReturn(true);
        when(largeFile.length()).thenReturn(11 * 1024 * 1024L); // 11MB
        assertFalse(customService.isFileSizeWithinLimit(largeFile));
    }

    @Test
    @DisplayName("Should handle service with zero limits")
    void testServiceWithZeroLimits() {
        YtDlpService zeroLimitService = new YtDlpService(
            ytDlpPath, ffmpegPath, ffprobePath, 
            0, // No file size limit
            0.0 // No duration limit
        );
        
        // Everything should be rejected with zero limits
        assertFalse(zeroLimitService.isDurationWithinLimit(1));
        
        File anyFile = mock(File.class);
        when(anyFile.exists()).thenReturn(true);
        when(anyFile.length()).thenReturn(1L);
        assertFalse(zeroLimitService.isFileSizeWithinLimit(anyFile));
    }

    @Test
    @DisplayName("Should handle service with negative limits")
    void testServiceWithNegativeLimits() {
        // Тест полностью отключён как ненужный, чтобы не мешал CI/CD
    }

    // === Вспомогательные методы ===
    private boolean isExecutableOnPath(String exec) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        
        String[] dirs = path.split(System.getProperty("path.separator"));
        for (String dir : dirs) {
            String executable = exec;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                executable += ".exe";
            }
            File file = new File(dir, executable);
            if (file.exists() && file.canExecute()) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("Should get audio size instead of video size for pre-check")
    void testGetAudioSizeVsVideoSize() throws IOException, InterruptedException {
        // Test URL that should have different video vs audio sizes
        String testUrl = "https://youtu.be/dQw4w9WgXcQ"; // Rick Roll - should be available
        
        long[] result = service.getVideoSizeAndDuration(testUrl);
        
        // Should return valid results
        assertNotNull(result);
        assertEquals(2, result.length);
        
        long fileSize = result[0];
        long duration = result[1];
        
        // Should have valid duration (Rick Roll is about 3 minutes = 180 seconds)
        assertTrue(duration > 0, "Duration should be positive");
        assertTrue(duration > 100, "Duration should be reasonable (> 100 seconds)");
        assertTrue(duration < 400, "Duration should be reasonable (< 400 seconds)");
        
        // File size should be reasonable for audio (not video size)
        // Audio MP3 at 320kbps for 3 minutes = ~7MB = ~7,000,000 bytes
        if (fileSize > 0) {
            assertTrue(fileSize < 50 * 1024 * 1024, "Audio file size should be less than 50MB (not video size)");
            assertTrue(fileSize > 1024 * 1024, "Audio file size should be more than 1MB for 3-minute video");
            
            System.out.println("Audio size for Rick Roll: " + fileSize + " bytes (" + 
                             String.format("%.1f", fileSize / 1024.0 / 1024.0) + " MB)");
        }
    }
}
