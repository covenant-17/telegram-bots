package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.io.File;
import java.util.List;
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
    void buildAudioRangeCommandUsesAbsoluteRangeFilter() {
        List<String> command = YtDlpService.buildAudioRangeCommand(
                "ffmpeg",
                new File("source.mp3"),
                new AudioClipRange(11.0, 122.0),
                new File("clip.mp3")
        );

        assertFalse(command.contains("-ss"));
        assertFalse(command.contains("-t"));
        assertFalse(command.contains("-vn"));
        int filterIndex = command.indexOf("-af");
        assertTrue(filterIndex >= 0);
        assertEquals(
                "atrim=start=11.000:end=122.000,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=0.500,afade=t=out:st=110.500:d=0.500",
                command.get(filterIndex + 1)
        );
        assertTrue(command.contains("-map"));
        assertTrue(command.contains("0:v?"));
        assertTrue(command.contains("-c:v"));
        assertTrue(command.contains("copy"));
    }

    @Test
    void testGetVideoInfoInvalidUrl() {
        // Skip test if yt-dlp not found in PATH
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
