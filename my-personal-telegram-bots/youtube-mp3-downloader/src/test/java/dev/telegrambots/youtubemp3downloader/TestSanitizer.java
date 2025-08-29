package dev.telegrambots.youtubemp3downloader;

/**
 * Simple test runner for FileNameSanitizer to check actual behavior
 */
public class TestSanitizer {
    public static void main(String[] args) {
        System.out.println("Testing FileNameSanitizer...");
        
        // Test problematic cases from the test failures
        String test1 = "Song [Remix (Official) [HD]] (2023)";
        String result1 = FileNameSanitizer.sanitize(test1);
        System.out.println("Test 1: \"" + test1 + "\" -> \"" + result1 + "\"");
        
        String test2 = "Song.mp3.in.the.middle (Official).mp3";
        String result2 = FileNameSanitizer.sanitize(test2);
        System.out.println("Test 2: \"" + test2 + "\" -> \"" + result2 + "\"");
        System.out.println("Contains .mp3: " + result2.contains(".mp3"));
        
        // Test composeFileName
        String channel = "Artist Name";
        String title = "Song Title";
        String composed = FileNameSanitizer.composeFileName(channel, title);
        System.out.println("Compose: \"" + channel + "\" + \"" + title + "\" -> \"" + composed + "\"");
        
        System.out.println("Done!");
    }
}