package dev.telegrambots.youtubemp3downloader;

public record AudioClipRange(double startSeconds, double endSeconds) {
    public static final double FADE_SECONDS = 0.5;

    public AudioClipRange {
        if (startSeconds < 0) {
            throw new IllegalArgumentException("startSeconds must be >= 0");
        }
        if (endSeconds <= startSeconds) {
            throw new IllegalArgumentException("endSeconds must be greater than startSeconds");
        }
    }

    public double durationSeconds() {
        return endSeconds - startSeconds;
    }

    public String formatLabel() {
        return formatTime(startSeconds) + " - " + formatTime(endSeconds);
    }

    static double parseTimeSeconds(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Time value is blank");
        }
        String[] parts = value.trim().split(":");
        if (parts.length == 1) {
            return Double.parseDouble(parts[0]);
        }
        if (parts.length == 2) {
            return Integer.parseInt(parts[0]) * 60.0 + Double.parseDouble(parts[1]);
        }
        if (parts.length == 3) {
            return Integer.parseInt(parts[0]) * 3600.0
                    + Integer.parseInt(parts[1]) * 60.0
                    + Double.parseDouble(parts[2]);
        }
        throw new IllegalArgumentException("Unsupported time format: " + value);
    }

    private static String formatTime(double seconds) {
        int roundedMillis = (int) Math.round(seconds * 1000);
        int totalSeconds = roundedMillis / 1000;
        int millis = roundedMillis % 1000;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        String base = hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, secs)
                : String.format("%d:%02d", minutes, secs);
        return millis == 0 ? base : base + String.format(".%03d", millis);
    }
}
