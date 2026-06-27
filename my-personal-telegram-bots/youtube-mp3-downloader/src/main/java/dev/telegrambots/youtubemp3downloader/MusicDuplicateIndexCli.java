package dev.telegrambots.youtubemp3downloader;

import java.nio.file.Path;

public class MusicDuplicateIndexCli {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java -cp youtube-mp3-downloader.jar dev.telegrambots.youtubemp3downloader.MusicDuplicateIndexCli <music-dir> <output-tsv>");
            System.exit(1);
        }
        Path musicRoot = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);
        MusicDuplicateIndex.writeIndex(musicRoot, outputPath);
        int count = MusicDuplicateIndex.scanMusicRoot(musicRoot).size();
        System.out.println("Wrote duplicate index: " + outputPath.toAbsolutePath() + " (" + count + " tracks)");
    }
}
