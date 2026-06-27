package dev.telegrambots.youtubemp3downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

public class YtDlpService {
    private static final Logger logger = LoggerFactory.getLogger(YtDlpService.class);
    private final String ytDlpPath;
    private final String ffmpegPath;
    private final String ffprobePath;
    private final long maxFileSize;
    private final double maxDurationMinutes;
    private final String cookiesPath;

    public YtDlpService(String ytDlpPath, String ffmpegPath, String ffprobePath, long maxFileSize, double maxDurationMinutes) {
        this(ytDlpPath, ffmpegPath, ffprobePath, maxFileSize, maxDurationMinutes, null);
    }

    public YtDlpService(String ytDlpPath, String ffmpegPath, String ffprobePath, long maxFileSize, double maxDurationMinutes, String cookiesPath) {
        this.ytDlpPath = ytDlpPath;
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
        this.maxFileSize = maxFileSize;
        this.maxDurationMinutes = maxDurationMinutes;
        this.cookiesPath = cookiesPath;
    }

    private java.util.List<String> commonYtDlpArgs() {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("--remote-components");
        args.add("ejs:github");
        if (cookiesPath != null && !cookiesPath.trim().isEmpty()) {
            args.add("--cookies");
            args.add(cookiesPath);
        }
        return args;
    }

    private static String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }    public boolean downloadAudio(String url, String outputPath) throws IOException, InterruptedException {
        if (url == null || url.trim().isEmpty() || outputPath == null || outputPath.trim().isEmpty()) {
            return false;
        }
        
        String ffmpegDir = new File(ffmpegPath).getParent();
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add(ytDlpPath);
        if (ffmpegDir != null) {
            cmd.add("--ffmpeg-location");
            cmd.add(ffmpegDir);
        }
        cmd.addAll(java.util.Arrays.asList(
            "--force-overwrites",
            "-f", "bestaudio[ext=webm]/bestaudio/best",
            "--extract-audio",
            "--audio-format", "mp3",
            "--audio-quality", "320K",
            "--postprocessor-args", "-b:a 320k",
            "--max-filesize", maxFileSize + "",
            "--no-playlist",
            "--max-downloads", "1",
            "--output", outputPath
        ));
        cmd.addAll(commonYtDlpArgs());
        cmd.add(url);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        logger.info("[{}] [yt-dlp] Command: {}", now(), String.join(" ", pb.command()));
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[{}] [yt-dlp] {}", now(), line);
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        logger.info("[{}] [yt-dlp] Exit code: {}", now(), exitCode);
        
        // Enhanced error code handling
        if (exitCode == 101) {
            logger.warn("[{}] [yt-dlp] Exit code 101 - Video unavailable, access denied, or empty file: {}", now(), url);
            logger.error("[{}] [yt-dlp] Full output:\n{}", now(), output);
        } else if (exitCode == 1) {
            logger.error("[{}] [yt-dlp] Exit code 1 - General error: {}", now(), url);
            logger.error("[{}] [yt-dlp] Full output:\n{}", now(), output);
        } else if (exitCode != 0) {
            logger.error("[{}] [yt-dlp] Exit code {} - Unknown error: {}", now(), exitCode, url);
            logger.error("[{}] [yt-dlp] Full output:\n{}", now(), output);
        }
        
        return exitCode == 0 || exitCode == 101;
    }    public boolean downloadAudioWithThumbnail(String url, String outputPath) throws IOException, InterruptedException {
        if (url == null || url.trim().isEmpty() || outputPath == null || outputPath.trim().isEmpty()) {
            return false;
        }
        
        // Create temporary directory for temp files
        File audioFile = new File(outputPath);
        File tempDir = new File(audioFile.getParent(), "temp_mp3");
        if (!tempDir.exists()) tempDir.mkdirs();
        String baseName = audioFile.getName().replace(".mp3", "");
        String tempFileName = baseName + "_" + System.currentTimeMillis() + ".mp3";
        File tempFile = new File(tempDir, tempFileName);

        // Download audio to temporary file
        boolean audioOk = downloadAudio(url, tempFile.getAbsolutePath());
        if (!audioOk) return false;

        // Download thumbnail via HTTP (faster than yt-dlp --skip-download)
        String thumbPath = tempFile.getAbsolutePath() + ".jpg";
        String videoId = extractVideoId(url);
        if (videoId != null) {
            downloadThumbnailHttp(videoId, thumbPath);
        }

        File sourceMp3 = tempFile;
        File outWithCoverFile = new File(tempDir, tempFileName.replace(".mp3", "_cover.mp3"));

        // Embed thumbnail into mp3 if downloaded
        File thumbFile = new File(thumbPath);
        if (thumbFile.exists() && tempFile.exists()) {
            String outWithCover = tempFile.getAbsolutePath().replace(".mp3", "_cover.mp3");
            logger.info("[{}] FFMPEG: tempFile={}, thumbFile={}, outWithCover={}", now(), tempFile.getAbsolutePath(), thumbFile.getAbsolutePath(), outWithCover);
            ProcessBuilder pbFfmpeg = new ProcessBuilder(
                    ffmpegPath,
                    "-i", tempFile.getAbsolutePath(),
                    "-i", thumbFile.getAbsolutePath(),
                    "-map", "0:a",
                    "-map", "1:v",
                    "-c:a", "copy",
                    "-c:v", "mjpeg",
                    "-id3v2_version", "3",
                    "-metadata:s:v", "title=Album cover",
                    "-metadata:s:v", "comment=Cover (front)",
                    "-disposition:v", "attached_pic",
                    outWithCover
            );
            pbFfmpeg.redirectErrorStream(true);
            Process ffmpegProc = pbFfmpeg.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(ffmpegProc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[{}] [ffmpeg] {}", now(), line);
                }
            }
            int ffmpegExit = ffmpegProc.waitFor();

            if (!thumbFile.exists() || thumbFile.length() == 0) {
                logger.error("[{}] Thumbnail file not found or empty: {}", now(), thumbFile.getAbsolutePath());
            }
            if (ffmpegExit != 0) {
                logger.error("[{}] ffmpeg failed to embed cover art, exit code: {}", now(), ffmpegExit);
            }

            if (ffmpegExit == 0) {
                sourceMp3 = outWithCoverFile.exists() ? outWithCoverFile : tempFile;
            }
            // Delete thumbnail
            thumbFile.delete();
        }

        if (!sourceMp3.exists() || sourceMp3.length() == 0) {
            logger.error("[{}] Downloaded temp audio file not found or empty: {}", now(), sourceMp3.getAbsolutePath());
            return false;
        }

        File finalFile = new File(audioFile.getParentFile(), baseName + ".mp3");
        java.nio.file.Files.copy(sourceMp3.toPath(), finalFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        logger.info("[{}] File saved to: {}", now(), finalFile.getAbsolutePath());

        // Delete tempFile and all temp jpg after completion
        tempFile.delete();
        outWithCoverFile.delete();
        File[] tempJpgs = tempDir.listFiles((d, name) -> name.startsWith(baseName) && name.endsWith(".jpg"));
        if (tempJpgs != null) {
            for (File jpg : tempJpgs) jpg.delete();
        }

        return true;
    }    public double getAudioDurationSeconds(String filePath) throws IOException, InterruptedException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return -1;
        }
        
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
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
                logger.error("[{}] [ffprobe] Failed to parse duration: {}", now(), line);
                return -1;
            }
        }
        return -1;
    }    public boolean isFileSizeWithinLimit(File file) {
        return Utils.isFileSizeWithinLimit(file, maxFileSize);
    }

    public boolean trimAudioRange(File audioFile, AudioClipRange range) throws IOException, InterruptedException {
        if (audioFile == null || range == null || !audioFile.exists() || audioFile.length() == 0) {
            return false;
        }

        File parent = audioFile.getParentFile();
        String name = audioFile.getName();
        String baseName = name.toLowerCase().endsWith(".mp3") ? name.substring(0, name.length() - 4) : name;
        File trimmedFile = new File(parent, baseName + "_clip_" + System.currentTimeMillis() + ".mp3");

        double clipDuration = range.durationSeconds();
        double fadeDuration = Math.min(AudioClipRange.FADE_SECONDS, clipDuration / 2.0);
        double fadeOutStart = Math.max(0.0, clipDuration - fadeDuration);
        String audioFilter = String.format(java.util.Locale.US,
                "afade=t=in:st=0:d=%.3f,afade=t=out:st=%.3f:d=%.3f",
                fadeDuration, fadeOutStart, fadeDuration);

        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
                ffmpegPath,
                "-y",
                "-i", audioFile.getAbsolutePath(),
                "-ss", formatSeconds(range.startSeconds()),
                "-t", formatSeconds(clipDuration),
                "-map", "0:a:0",
                "-vn",
                "-af", audioFilter,
                "-c:a", "libmp3lame",
                "-b:a", "320k",
                trimmedFile.getAbsolutePath()
        ));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        logger.info("[{}] [ffmpeg-trim] Command: {}", now(), String.join(" ", pb.command()));
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[{}] [ffmpeg-trim] {}", now(), line);
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0 || !trimmedFile.exists() || trimmedFile.length() == 0) {
            logger.error("[{}] [ffmpeg-trim] Failed to trim {} to {}. Exit code: {}. Output:\n{}",
                    now(), audioFile.getAbsolutePath(), range.formatLabel(), exitCode, output);
            trimmedFile.delete();
            return false;
        }

        java.nio.file.Files.move(
                trimmedFile.toPath(),
                audioFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
        logger.info("[{}] [ffmpeg-trim] Trimmed file saved to: {} | Range: {}",
                now(), audioFile.getAbsolutePath(), range.formatLabel());
        return true;
    }

    private static String formatSeconds(double seconds) {
        return String.format(java.util.Locale.US, "%.3f", seconds);
    }

    public boolean isDurationWithinLimit(double durationSeconds) {
        return Utils.isDurationWithinLimit(durationSeconds, maxDurationMinutes);
    }public String[] getVideoInfo(String url) throws IOException, InterruptedException {
        if (url == null || url.trim().isEmpty()) {
            return new String[]{"channel", "video"};
        }
        
        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
                ytDlpPath,
                "--print", "uploader", "--print", "title"
        ));
        cmd.addAll(commonYtDlpArgs());
        cmd.add(url);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
        String channel = null, title = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (channel == null && !line.trim().isEmpty() && !line.startsWith("WARNING")) {
                channel = line.trim();
                continue;
            }
            if (channel != null && title == null && !line.trim().isEmpty() && !line.startsWith("WARNING")) {
                title = line.trim();
                break;
            }
        }
        process.waitFor();
        return new String[]{channel != null ? channel : "channel", title != null ? title : "video"};
    }    /**
     * Check video file size and duration before downloading to avoid wasting time on large videos
     * @param url YouTube video URL
     * @return array: [filesize in bytes (-1 if unknown), duration in seconds (-1 if unknown)]
     */
    public long[] getVideoSizeAndDuration(String url) throws IOException, InterruptedException {
        if (url == null || url.trim().isEmpty()) {
            return new long[]{-1, -1};
        }
        
        // First try to get audio-only size (more accurate for our use case)
        long[] audioSizeResult = getAudioOnlySize(url);
        if (audioSizeResult[0] > 0) {
            return audioSizeResult;
        }
        
        // Fallback to regular video size if audio size is not available
        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
                ytDlpPath,
                "--dump-json",
                "--no-download"
        ));
        cmd.addAll(commonYtDlpArgs());
        cmd.add(url);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder jsonBuilder = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("[{}] yt-dlp failed to get video info for URL: {} (exit code: {})", now(), url, exitCode);
            return new long[]{-1, -1};
        }
        
        String json = jsonBuilder.toString().trim();
        if (json.isEmpty() || !json.startsWith("{")) {
            logger.warn("[{}] Invalid JSON response from yt-dlp for URL: {}", now(), url);
            return new long[]{-1, -1};
        }
        
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            long filesize = obj.optLong("filesize", obj.optLong("filesize_approx", -1));
            long duration = obj.optLong("duration", -1);
            
            logger.info("[{}] Video info for URL: {} | Size: {} bytes | Duration: {} seconds", 
                       now(), url, filesize, duration);
            
            return new long[]{filesize, duration};
        } catch (Exception e) {
            logger.error("[{}] Error parsing JSON response from yt-dlp for URL: {}", now(), url, e);
            return new long[]{-1, -1};
        }
    }
    
    /**
     * Get audio-only file size using yt-dlp format selection
     * @param url YouTube video URL
     * @return array: [audio filesize in bytes (-1 if unknown), duration in seconds (-1 if unknown)]
     */
    private long[] getAudioOnlySize(String url) throws IOException, InterruptedException {
        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
                ytDlpPath,
                "--dump-json",
                "--no-download",
                "--format", "bestaudio[ext=webm]/bestaudio/best",
                "--extract-audio",
                "--audio-format", "mp3"
        ));
        cmd.addAll(commonYtDlpArgs());
        cmd.add(url);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder jsonBuilder = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.debug("[{}] yt-dlp failed to get audio size for URL: {} (exit code: {})", now(), url, exitCode);
            return new long[]{-1, -1};
        }
        
        String json = jsonBuilder.toString().trim();
        if (json.isEmpty() || !json.startsWith("{")) {
            logger.debug("[{}] Invalid JSON response for audio size from yt-dlp for URL: {}", now(), url);
            return new long[]{-1, -1};
        }
        
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            long filesize = obj.optLong("filesize", -1);
            long duration = obj.optLong("duration", -1);
            
            if (filesize > 0) {
                logger.info("[{}] Audio size for URL: {} | Size: {} bytes | Duration: {} seconds", 
                           now(), url, filesize, duration);
                return new long[]{filesize, duration};
            }
            
            return new long[]{-1, -1};
        } catch (Exception e) {
            logger.debug("[{}] Error parsing audio size JSON response from yt-dlp for URL: {}", now(), url, e);
            return new long[]{-1, -1};
        }
    }

    public void deleteFileIfExists(File file) {
        Utils.deleteFileIfExists(file);
    }

    /** Extracts YouTube video ID from various URL formats. */
    private String extractVideoId(String url) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:v=|youtu\\.be/|/embed/|/shorts/)([A-Za-z0-9_-]{11})")
                .matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /** Downloads YouTube thumbnail via HTTP. Tries maxresdefault, falls back to hqdefault. */
    private void downloadThumbnailHttp(String videoId, String destPath) {
        String[] urls = {
            "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg",
            "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"
        };
        for (String thumbUrl : urls) {
            try {
                java.net.URL u = new java.net.URL(thumbUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                if (conn.getResponseCode() == 200) {
                    try (java.io.InputStream in = conn.getInputStream();
                         java.io.FileOutputStream out = new java.io.FileOutputStream(destPath)) {
                        in.transferTo(out);
                    }
                    logger.info("[{}] [thumb-http] Downloaded thumbnail for {}: {}", now(), videoId, thumbUrl);
                    return;
                }
                conn.disconnect();
            } catch (Exception e) {
                logger.warn("[{}] [thumb-http] Failed to download {}: {}", now(), thumbUrl, e.getMessage());
            }
        }
        logger.warn("[{}] [thumb-http] Could not download thumbnail for video: {}", now(), videoId);
    }
}

// Note: not all players display embedded cover art in mp3. Embedding image via ffmpeg works (ID3v2 APIC), but display depends on the client (Telegram, VLC, Windows Media Player, etc.).
