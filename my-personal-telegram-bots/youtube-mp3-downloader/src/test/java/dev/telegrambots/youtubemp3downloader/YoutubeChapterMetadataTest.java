package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeChapterMetadataTest {
    @Test
    void parsesValidChaptersFromYtDlpJson() {
        String json = """
                {
                  "uploader": "Ashla",
                  "title": "The Poisoned Waltz",
                  "duration": 1200,
                  "chapters": [
                    {"start_time": 0.0, "end_time": 120.0, "title": "Intro"},
                    {"start_time": 120.0, "end_time": 300.0, "title": "Main Track"}
                  ]
                }
                """;

        YoutubeVideoMetadata metadata = YtDlpService.parseVideoMetadataJson(json);

        assertEquals("Ashla", metadata.channel());
        assertEquals("The Poisoned Waltz", metadata.title());
        assertTrue(metadata.hasMultipleChapters());
        assertEquals(2, metadata.chapters().size());
        assertEquals("Intro", metadata.chapters().get(0).title());
        assertEquals(120.0, metadata.chapters().get(1).startSeconds());
        assertEquals(300.0, metadata.chapters().get(1).endSeconds());
    }

    @Test
    void fillsMissingChapterEndFromNextStartOrVideoDuration() {
        String json = """
                {
                  "channel": "Channel",
                  "title": "Album",
                  "duration": 360,
                  "chapters": [
                    {"start_time": 0.0, "title": "One"},
                    {"start_time": 100.0, "title": "Two"}
                  ]
                }
                """;

        YoutubeVideoMetadata metadata = YtDlpService.parseVideoMetadataJson(json);

        assertEquals(2, metadata.chapters().size());
        assertEquals(100.0, metadata.chapters().get(0).endSeconds());
        assertEquals(360.0, metadata.chapters().get(1).endSeconds());
    }

    @Test
    void filtersInvalidChapters() {
        String json = """
                {
                  "uploader": "Channel",
                  "title": "Album",
                  "duration": 100,
                  "chapters": [
                    {"start_time": 0.0, "end_time": 20.0, "title": "Valid"},
                    {"start_time": 20.0, "end_time": 10.0, "title": ""},
                    {"start_time": -1.0, "end_time": 30.0, "title": "Bad"}
                  ]
                }
                """;

        YoutubeVideoMetadata metadata = YtDlpService.parseVideoMetadataJson(json);

        assertEquals(1, metadata.chapters().size());
        assertEquals("Valid", metadata.chapters().get(0).title());
    }
}
