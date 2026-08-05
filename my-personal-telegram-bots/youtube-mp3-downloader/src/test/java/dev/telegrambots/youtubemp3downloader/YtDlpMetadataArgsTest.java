package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YtDlpMetadataArgsTest {

    @Test
    void addsAndroidClientForMetadataRequests() {
        List<String> args = new ArrayList<>();

        YtDlpService.addYoutubeMetadataExtractorArgs(args);

        assertEquals(List.of("--extractor-args", "youtube:player_client=android"), args);
    }

    @Test
    void addsAndroidClientForAudioDownloads() {
        List<String> command = YtDlpService.buildDownloadAudioCommand(
                "yt-dlp",
                null,
                "/tmp/out.mp3",
                "https://www.youtube.com/watch?v=s6UbtIqOTR0",
                1024L,
                List.of("--cookies", "/tmp/cookies.txt")
        );

        int extractorArgsIndex = command.indexOf("--extractor-args");
        assertTrue(extractorArgsIndex >= 0);
        assertEquals("youtube:player_client=android", command.get(extractorArgsIndex + 1));
        assertTrue(command.indexOf("--extractor-args") < command.indexOf("https://www.youtube.com/watch?v=s6UbtIqOTR0"));
    }

    @Test
    void treatsYtDlpWarningsAndErrorsAsDiagnostics() {
        assertTrue(YtDlpService.isYtDlpDiagnosticLine(""));
        assertTrue(YtDlpService.isYtDlpDiagnosticLine("WARNING: [youtube] HTTP Error 429"));
        assertTrue(YtDlpService.isYtDlpDiagnosticLine("ERROR: [youtube] Sign in to confirm you're not a bot"));
        assertTrue(YtDlpService.isYtDlpDiagnosticLine("[youtube] Extracting URL"));
        assertFalse(YtDlpService.isYtDlpDiagnosticLine("Untitled Burial"));
        assertFalse(YtDlpService.isYtDlpDiagnosticLine("Polytence - Angels in the Ocean"));
    }
}
