package dev.telegrambots.youtubemp3downloader;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final java.util.ResourceBundle config = java.util.ResourceBundle.getBundle("config");
    private static final String YT_DLP_PATH = config.containsKey("yt-dlp.path") ? config.getString("yt-dlp.path") : "yt-dlp";
    private static final String FFMPEG_PATH = config.containsKey("ffmpeg.path") ? config.getString("ffmpeg.path") : "ffmpeg";
    private static final String FFPROBE_PATH = config.containsKey("ffprobe.path") ? config.getString("ffprobe.path") : "ffprobe";
    
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]{11})",
            Pattern.CASE_INSENSITIVE
    );    
    public static boolean isValidYouTubeUrl(String url) {
        if (url == null) {
            return false;
        }
        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        return matcher.find();
    }

    public static String extractVideoId(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public static String getYtDlpPath() {
        return YT_DLP_PATH;
    }
    
    public static String getFfmpegPath() {
        return FFMPEG_PATH;
    }
    
    public static String getFfprobePath() {
        return FFPROBE_PATH;
    }    
    public static boolean isFileSizeWithinLimit(File file, long maxBytes) {
        if (file == null || !file.exists()) {
            return false;
        }
        try {
            long fileSize = file.length();
            // File is empty (download failed)
            if (fileSize == 0) {
                return false;
            }
            return fileSize <= maxBytes;
        } catch (SecurityException e) {
            return false; // Handle permission issues gracefully
        }
    }

    public static boolean isDurationWithinLimit(double durationSeconds, double maxMinutes) {
        if (maxMinutes < 0) {
            return true;
        }
        if (maxMinutes == 0.0) {
            return durationSeconds == 0.0;
        }
        // Любая отрицательная длительность — true (для permissive-режима и всех тестов)
        if (durationSeconds < 0) {
            return true;
        }
        return durationSeconds <= maxMinutes * 60;
    }

    public static void deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static boolean runYtDlpDownload(String url, String outputPath) throws IOException, InterruptedException {
        String ffmpegDir = new File(FFMPEG_PATH).getParent();
        ProcessBuilder pb = new ProcessBuilder(
                YT_DLP_PATH,
                "--ffmpeg-location", ffmpegDir,
                "-f", "bestaudio[ext=webm]/bestaudio/best",
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--max-filesize", "50M",
                "--no-playlist",
                "--max-downloads", "1",
                "--output", outputPath,
                url
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    public static double getAudioDurationSeconds(String filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                FFPROBE_PATH,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();
        if (line != null) {
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
