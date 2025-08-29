package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки размера видео и длительности до скачивания
 */
class YtDlpServiceSizeCheckTest {
    private YtDlpService service;
    private final String ytDlpPath = "yt-dlp";
    private final String ffmpegPath = "ffmpeg";
    private final String ffprobePath = "ffprobe";
    private final long maxFileSize = 50 * 1024 * 1024; // 50MB
    private final double maxDurationMinutes = 30.0;

    @BeforeEach
    void setUp() {
        service = new YtDlpService(ytDlpPath, ffmpegPath, ffprobePath, maxFileSize, maxDurationMinutes);
    }

    @Test
    @DisplayName("Should return valid size and duration for existing video")
    void testGetVideoSizeAndDuration_ValidVideo() throws Exception {
        // Test is disabled by default since it requires actual network access
        // Uncomment and use a real video URL for manual testing
        Assumptions.assumeTrue(false, "Test disabled - requires network access");
        
        String testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"; // Rick Roll - short video
        long[] result = service.getVideoSizeAndDuration(testUrl);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        // Size and duration should be positive or -1 if unknown
        assertTrue(result[0] >= -1, "File size should be >= -1");
        assertTrue(result[1] >= -1, "Duration should be >= -1");
    }

    @Test
    @DisplayName("Should handle invalid URL gracefully")
    void testGetVideoSizeAndDuration_InvalidUrl() throws Exception {
        // Skip test if yt-dlp is not available
        Assumptions.assumeTrue(false, "Test disabled - requires yt-dlp installation");
        
        String invalidUrl = "https://www.youtube.com/watch?v=invalid_video_id";
        long[] result = service.getVideoSizeAndDuration(invalidUrl);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(-1, result[0], "File size should be -1 for invalid URL");
        assertEquals(-1, result[1], "Duration should be -1 for invalid URL");
    }

    @Test
    @DisplayName("Should handle null URL")
    void testGetVideoSizeAndDuration_NullUrl() throws Exception {
        long[] result = service.getVideoSizeAndDuration(null);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(-1, result[0]);
        assertEquals(-1, result[1]);
    }

    @Test
    @DisplayName("Should handle empty URL")
    void testGetVideoSizeAndDuration_EmptyUrl() throws Exception {
        long[] result = service.getVideoSizeAndDuration("");
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(-1, result[0]);
        assertEquals(-1, result[1]);
    }
}
