package dev.telegrambots.youtube;

import java.io.File;
import java.io.IOException;

public class YtDlpRunner {

    private static final String YT_DLP_PATH = "path/to/yt-dlp";
    private static final String FFMPEG_PATH = "path/to/ffmpeg";

    private static String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
        // Логируем рабочий каталог и команду
        System.out.println("[" + now() + "] [yt-dlp] Working dir: " + new File("").getAbsolutePath());
        System.out.println("[" + now() + "] [yt-dlp] Command: " + String.join(" ", pb.command()));
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[" + now() + "] [yt-dlp] " + line);
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        System.out.println("[" + now() + "] [yt-dlp] Exit code: " + exitCode);
        if (exitCode != 0) {
            System.out.println("[" + now() + "] [yt-dlp] Full output:\n" + output);
        }
        return exitCode == 0;
    }
}