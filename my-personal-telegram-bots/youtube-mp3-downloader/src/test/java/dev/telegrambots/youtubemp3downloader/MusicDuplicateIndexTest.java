package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MusicDuplicateIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldNormalizeUnsanitizedNamesForMatching() {
        String left = MusicDuplicateIndex.normalizeForMatch("The Ghost Aura - Nihilism (Official Video).mp3");
        String right = MusicDuplicateIndex.normalizeForMatch("the ghost aura nihilism");

        assertEquals(right, left);
    }

    @Test
    void shouldFindExactDuplicateFromGeneratedIndex() throws Exception {
        Path musicRoot = tempDir.resolve("music");
        Files.createDirectories(musicRoot);
        Files.writeString(musicRoot.resolve("The Ghost Aura - Nihilism (Official Video).mp3"), "fake");

        Path indexPath = tempDir.resolve("music-index.tsv");
        MusicDuplicateIndex.writeIndex(musicRoot, indexPath);

        MusicDuplicateIndex index = new MusicDuplicateIndex(indexPath.toString());

        assertTrue(index.findDuplicate("The Ghost Aura Nihilism").isPresent());
    }

    @Test
    void shouldFindTokenOrderDuplicate() throws Exception {
        Path indexPath = tempDir.resolve("music-index.tsv");
        Files.writeString(indexPath, "match_key\tdisplay_name\tpath\nartist song\tArtist - Song\tC:\\Music\\Artist - Song.mp3\n");

        MusicDuplicateIndex index = new MusicDuplicateIndex(indexPath.toString());

        assertTrue(index.findDuplicate("Song Artist").isPresent());
    }

    @Test
    void disabledIndexShouldNotMatch() {
        MusicDuplicateIndex index = new MusicDuplicateIndex("");

        assertFalse(index.isEnabled());
        assertTrue(index.findDuplicate("Artist Song").isEmpty());
    }
}
