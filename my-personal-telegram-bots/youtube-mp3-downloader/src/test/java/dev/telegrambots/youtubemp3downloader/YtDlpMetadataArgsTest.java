package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YtDlpMetadataArgsTest {

    @Test
    void commonArgumentsDisableImplicitConfigAndAddConfiguredCookies() {
        List<String> args = YtDlpService.buildCommonYtDlpArgs("/tmp/cookies.txt");

        assertEquals(List.of(
                "--ignore-config",
                "--remote-components", "ejs:github",
                "--cookies", "/tmp/cookies.txt"
        ), args);
    }

    @Test
    void addsAndroidClientForMetadataRequests() {
        List<String> args = new ArrayList<>();

        YtDlpService.addYoutubeMetadataExtractorArgs(args, List.of("--remote-components", "ejs:github"));

        assertEquals(List.of("--extractor-args", "youtube:player_client=android"), args);
    }

    @Test
    void usesCookieCompatibleClientsForAudioDownloadsUsingCookies() {
        List<String> command = YtDlpService.buildDownloadAudioCommand(
                "yt-dlp",
                null,
                "/tmp/out.mp3",
                "https://www.youtube.com/watch?v=s6UbtIqOTR0",
                1024L,
                List.of("--ignore-config", "--cookies", "/tmp/cookies.txt")
        );

        int extractorArgsIndex = command.indexOf("--extractor-args");
        assertTrue(extractorArgsIndex >= 0);
        assertEquals("youtube:player_client=default,web_embedded", command.get(extractorArgsIndex + 1));
        assertFalse(command.contains("youtube:player_client=android"));
        assertTrue(command.contains("--ignore-config"));
        assertTrue(command.contains("--cookies"));
        assertTrue(command.indexOf("--cookies") < command.indexOf("https://www.youtube.com/watch?v=s6UbtIqOTR0"));
    }

    @Test
    void addsAndroidClientForAudioDownloadsWithoutCookies() {
        List<String> command = YtDlpService.buildDownloadAudioCommand(
                "yt-dlp",
                null,
                "/tmp/out.mp3",
                "https://www.youtube.com/watch?v=s6UbtIqOTR0",
                1024L,
                List.of("--ignore-config", "--remote-components", "ejs:github")
        );

        assertTrue(command.contains("--ignore-config"));
        int extractorArgsIndex = command.indexOf("--extractor-args");
        assertTrue(extractorArgsIndex >= 0);
        assertEquals("youtube:player_client=android", command.get(extractorArgsIndex + 1));
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

    @Test
    void parsesOnlyMarkedVideoInfoWhenYtDlpPrintsMultilineWarnings() {
        String output = """
                WARNING: Your yt-dlp version (2026.06.09) is older than 90 days!
                         It is strongly recommended to always use the latest version.
                         You installed yt-dlp with pip or using the wheel from PyPi; Use that to update.
                         To suppress this warning, add --no-update to your command/config.
                [youtube] Extracting URL: https://www.youtube.com/watch?v=ZP-KMvYzg5s
                __YT_DLP_CHANNEL__=The Real Channel
                __YT_DLP_TITLE__=The Real Song
                """;

        assertEquals(
                List.of("The Real Channel", "The Real Song"),
                List.of(YtDlpService.parseVideoInfoOutput(output))
        );
    }

    @Test
    void fallsBackToSafePlaceholdersWhenMarkedVideoInfoIsMissing() {
        String warningOnlyOutput = """
                WARNING: Your yt-dlp version is older than 90 days!
                         It is strongly recommended to always use the latest version.
                         You installed yt-dlp with pip or using the wheel from PyPi; Use that to update.
                """;

        assertEquals(
                List.of("channel", "video"),
                List.of(YtDlpService.parseVideoInfoOutput(warningOnlyOutput))
        );
    }

    @Test
    void extractsJsonMetadataAfterYtDlpWarnings() {
        String output = """
                WARNING: Your yt-dlp version is older than 90 days!
                         It is strongly recommended to always use the latest version.
                {"uploader":"The Real Channel","title":"The Real Song"}
                """;

        assertEquals(
                "{\"uploader\":\"The Real Channel\",\"title\":\"The Real Song\"}",
                YtDlpService.extractJsonObject(output)
        );
    }
}
