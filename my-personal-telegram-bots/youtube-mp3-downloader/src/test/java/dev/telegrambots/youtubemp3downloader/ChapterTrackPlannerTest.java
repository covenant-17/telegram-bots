package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterTrackPlannerTest {
    @Test
    void buildsSanitizedChannelChapterFileNames() {
        YoutubeVideoMetadata metadata = new YoutubeVideoMetadata(
                "Ashla / Bad:Channel",
                "Album",
                300,
                List.of(
                        new YoutubeChapter("The Poisoned Waltz (Official Video)", 0, 120),
                        new YoutubeChapter("The Golden/Gilded:Board", 120, 300)
                )
        );

        List<ChapterTrackPlan> plans = ChapterTrackPlanner.build(metadata);

        assertEquals("Ashla Bad Channel - The Poisoned Waltz", plans.get(0).baseName());
        assertEquals("Ashla Bad Channel - The Poisoned Waltz.mp3", plans.get(0).fileName());
        assertEquals("Ashla Bad Channel - The Golden Gilded Board.mp3", plans.get(1).fileName());
    }

    @Test
    void appliesFinalSanitizeAfterComposeAndAddsSuffixForDuplicateNames() {
        YoutubeVideoMetadata metadata = new YoutubeVideoMetadata(
                "Channel",
                "Album",
                300,
                List.of(
                        new YoutubeChapter("Track???", 0, 100),
                        new YoutubeChapter("Track", 100, 200),
                        new YoutubeChapter("@#$%^&*()", 200, 300)
                )
        );

        List<ChapterTrackPlan> plans = ChapterTrackPlanner.build(metadata);

        assertEquals("Channel - Track", plans.get(0).baseName());
        assertEquals("Channel - Track (2)", plans.get(1).baseName());
        assertEquals("Channel", plans.get(2).baseName());
    }

    @Test
    void usesTrackFallbackWhenComposedNameIsBlank() {
        YoutubeVideoMetadata metadata = new YoutubeVideoMetadata(
                "",
                "Album",
                30,
                List.of(new YoutubeChapter("@#$%^&*()", 0, 30))
        );

        List<ChapterTrackPlan> plans = ChapterTrackPlanner.build(metadata);

        assertEquals("track-1.mp3", plans.get(0).fileName());
    }
}
