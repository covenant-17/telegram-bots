package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DownloadRequestDuplicateIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldMatchSameVideoAndClipRangeAcrossUrlForms() throws Exception {
        Path musicIndexPath = tempDir.resolve("music-index.tsv");
        DownloadRequestDuplicateIndex index = new DownloadRequestDuplicateIndex(musicIndexPath.toString());
        DownloadRequest original = new DownloadRequest(
                "https://www.youtube.com/watch?v=Sq_DbuY0kqU",
                new AudioClipRange(11, 122)
        );
        Path downloadedFile = tempDir.resolve("Sosmula Krakk Star Ft. Rocket Rese.mp3");
        Files.writeString(downloadedFile, "fake");

        assertTrue(index.addOrUpdate(original, "Sosmula Krakk Star Ft. Rocket Rese.mp3", downloadedFile));

        DownloadRequest repeated = new DownloadRequest(
                "https://youtu.be/Sq_DbuY0kqU",
                new AudioClipRange(11, 122)
        );
        assertTrue(index.findDuplicate(repeated).isPresent());
    }

    @Test
    void shouldKeepDifferentClipRangesSeparate() throws Exception {
        Path musicIndexPath = tempDir.resolve("music-index.tsv");
        DownloadRequestDuplicateIndex index = new DownloadRequestDuplicateIndex(musicIndexPath.toString());
        Path downloadedFile = tempDir.resolve("clip.mp3");
        Files.writeString(downloadedFile, "fake");

        index.addOrUpdate(
                new DownloadRequest("https://www.youtube.com/watch?v=Sq_DbuY0kqU", new AudioClipRange(11, 122)),
                "clip.mp3",
                downloadedFile
        );

        assertTrue(index.findDuplicate(new DownloadRequest(
                "https://www.youtube.com/watch?v=Sq_DbuY0kqU",
                new AudioClipRange(12, 122)
        )).isEmpty());
    }
}
