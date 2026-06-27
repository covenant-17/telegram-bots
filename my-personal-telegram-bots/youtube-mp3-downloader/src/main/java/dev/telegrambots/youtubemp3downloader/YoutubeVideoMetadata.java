package dev.telegrambots.youtubemp3downloader;

import java.util.List;

public record YoutubeVideoMetadata(String channel, String title, double durationSeconds, List<YoutubeChapter> chapters) {
    public YoutubeVideoMetadata {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }

    public boolean hasMultipleChapters() {
        return chapters.size() >= 2;
    }
}
