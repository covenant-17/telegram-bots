package dev.telegrambots.youtubemp3downloader;

public class FileNameSanitizerCli {
    public static void main(String[] args) {
        if (args.length < 1 || args[0].equals("--help")) {
            System.out.println("Usage: java -cp <jar> dev.telegrambots.youtubemp3downloader.FileNameSanitizerCli <dir> [ext] [dryRun]");
            System.out.println("Example: java -cp ... FileNameSanitizerCli termuxserver/youtube_mp3_downloader_workzone .mp3 true");
            return;
        }
        String dir = args[0];
        String ext = args.length > 1 ? args[1] : ".mp3";
        boolean dryRun = args.length > 2 && Boolean.parseBoolean(args[2]);
        FileNameSanitizer.sanitizeAllInDirectory(dir, ext, dryRun);
    }
}
