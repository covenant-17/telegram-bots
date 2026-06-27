package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DownloadRequestParserTest {
    @Test
    void parsesDefaultOneUrlPerLine() {
        List<DownloadRequest> requests = DownloadRequestParser.parse("""
                https://www.youtube.com/watch?v=9tRYfgi5mZ0
                https://www.youtube.com/watch?v=KPNZp8x4sxs
                """);

        assertEquals(2, requests.size());
        assertEquals("https://www.youtube.com/watch?v=9tRYfgi5mZ0", requests.get(0).url());
        assertNull(requests.get(0).clipRange());
        assertEquals("https://www.youtube.com/watch?v=KPNZp8x4sxs", requests.get(1).url());
        assertNull(requests.get(1).clipRange());
    }

    @Test
    void parsesOptionalClipRangeAfterUrl() {
        List<DownloadRequest> requests = DownloadRequestParser.parse("""
                https://www.youtube.com/watch?v=9tRYfgi5mZ0
                https://www.youtube.com/watch?v=KPNZp8x4sxs 0:00 2:00
                https://www.youtube.com/watch?v=JKP5Tq0wkqI
                """);

        assertEquals(3, requests.size());
        assertNull(requests.get(0).clipRange());
        assertNotNull(requests.get(1).clipRange());
        assertEquals(0.0, requests.get(1).clipRange().startSeconds());
        assertEquals(120.0, requests.get(1).clipRange().endSeconds());
        assertNull(requests.get(2).clipRange());
    }

    @Test
    void parsesNonZeroStartRange() {
        DownloadRequest request = DownloadRequestParser.parse(
                "https://www.youtube.com/watch?v=KPNZp8x4sxs 0:01 2:00").get(0);

        assertEquals(1.0, request.clipRange().startSeconds());
        assertEquals(120.0, request.clipRange().endSeconds());
    }

    @Test
    void deduplicatesSameUrlWithSameRangeButKeepsDifferentRanges() {
        List<DownloadRequest> requests = DownloadRequestParser.parse("""
                https://www.youtube.com/watch?v=KPNZp8x4sxs 0:00 2:00
                https://www.youtube.com/watch?v=KPNZp8x4sxs 0:00 2:00
                https://www.youtube.com/watch?v=KPNZp8x4sxs 0:01 2:00
                """);

        assertEquals(2, requests.size());
        assertEquals(0.0, requests.get(0).clipRange().startSeconds());
        assertEquals(1.0, requests.get(1).clipRange().startSeconds());
    }

    @Test
    void ignoresUnsupportedShortsLinks() {
        List<DownloadRequest> requests = DownloadRequestParser.parse(
                "https://www.youtube.com/shorts/KPNZp8x4sxs 0:00 2:00");

        assertTrue(requests.isEmpty());
    }

    @Test
    void ignoresInvalidRangeWithoutDroppingUrl() {
        List<DownloadRequest> requests = DownloadRequestParser.parse(
                "https://www.youtube.com/watch?v=KPNZp8x4sxs 2:00 0:00");

        assertEquals(1, requests.size());
        assertNull(requests.get(0).clipRange());
    }
}
