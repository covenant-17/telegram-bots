package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class YtDlpServiceTest {
    private final String ytDlpPath = "yt-dlp"; // mock or test binary path
    private final String ffmpegPath = "ffmpeg";
    private final String ffprobePath = "ffprobe";
    private final long maxFileSize = 50 * 1024 * 1024;
    private final double maxDurationMinutes = 30.0;
    private final YtDlpService service = new YtDlpService(ytDlpPath, ffmpegPath, ffprobePath, maxFileSize, maxDurationMinutes);

    @Test
    void testIsFileSizeWithinLimit() {
        File file = new File("test.mp3");
        // file does not exist, should NOT be within limit
        assertFalse(service.isFileSizeWithinLimit(file));
    }

    @Test
    void testIsDurationWithinLimit() {
        assertTrue(service.isDurationWithinLimit(60)); // 1 min
        assertFalse(service.isDurationWithinLimit(31 * 60)); // 31 min
    }

    @Test
    void testDeleteFileIfExists() throws Exception {
        File file = File.createTempFile("ytmp3test", ".tmp");
        assertTrue(file.exists());
        service.deleteFileIfExists(file);
        assertFalse(file.exists());
    }

    @Test
    void testGetVideoInfoInvalidUrl() {
        // Пропустить тест, если yt-dlp не найден
        boolean ytDlpExists = isExecutableOnPath(ytDlpPath);
        Assumptions.assumeTrue(ytDlpExists, "yt-dlp not found in PATH, skipping test");
        assertDoesNotThrow(() -> {
            String[] info = service.getVideoInfo("https://invalid.url");
            assertNotNull(info);
            assertEquals(2, info.length);
        });
    }

    private boolean isExecutableOnPath(String exec) {
        String path = System.getenv("PATH");
        String[] dirs = path.split(System.getProperty("path.separator"));
        for (String dir : dirs) {
            java.io.File file = new java.io.File(dir, exec + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));
            if (file.exists() && file.canExecute()) return true;
        }
        return false;
    }
}
