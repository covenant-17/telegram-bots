package dev.telegrambots.youtubemp3downloader;

public record YoutubeChapter(String title, double startSeconds, double endSeconds) {
    public YoutubeChapter {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startSeconds < 0) {
            throw new IllegalArgumentException("startSeconds must be >= 0");
        }
        if (endSeconds <= startSeconds) {
            throw new IllegalArgumentException("endSeconds must be greater than startSeconds");
        }
    }

    public AudioClipRange clipRange() {
        return new AudioClipRange(startSeconds, endSeconds);
    }

    public double durationSeconds() {
        return endSeconds - startSeconds;
    }
}
