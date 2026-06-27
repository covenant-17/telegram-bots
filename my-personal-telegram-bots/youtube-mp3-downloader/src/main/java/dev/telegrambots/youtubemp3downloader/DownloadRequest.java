package dev.telegrambots.youtubemp3downloader;

public record DownloadRequest(String url, AudioClipRange clipRange) {
    public boolean hasClipRange() {
        return clipRange != null;
    }
}
