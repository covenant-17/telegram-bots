package dev.telegrambots.youtubemp3downloader;

import java.util.List;

public record YoutubeVideoMetadata(String channel, String title, double durationSeconds, List<YoutubeChapter> chapters) {
    private static final double MIN_CHAPTER_SPLIT_DURATION_SECONDS = 5 * 60.0;

    public YoutubeVideoMetadata {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }

    public boolean hasMultipleChapters() {
        return chapters.size() >= 2 && durationSeconds >= MIN_CHAPTER_SPLIT_DURATION_SECONDS;
    }
}
