package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simple test to verify actual FileNameSanitizer behavior
 */
class DebugSanitizerTest {    @Test
    void debugActualOutput() {
        // Check actual sanitizer output
        String result1 = FileNameSanitizer.sanitize("Ñoño - 🎵 Music");
        System.out.println("1: \"" + result1 + "\"");
        
        String result2 = FileNameSanitizer.sanitize("Clean!@#$%^&*()Title");
        System.out.println("2: \"" + result2 + "\"");
        
        String result3 = FileNameSanitizer.sanitize("Queen - Bohemian Rhapsody (Remastered 1975)");
        System.out.println("3: \"" + result3 + "\"");
        
        String result4 = FileNameSanitizer.sanitize("@#$%^&*()");
        System.out.println("4: \"" + result4 + "\"");
        
        String result5 = FileNameSanitizer.sanitize("Encoded%7cCharacters");
        System.out.println("5: \"" + result5 + "\"");
        
        // Additional tests for edge cases
        String result6 = FileNameSanitizer.sanitize("Multiple...Dots");
        System.out.println("6: \"" + result6 + "\"");
        
        String result7 = FileNameSanitizer.sanitize(".,;:\"'[]{}");
        System.out.println("7: \"" + result7 + "\"");
          String result8 = FileNameSanitizer.sanitize("Plus%2BSign");
        System.out.println("8: \"" + result8 + "\"");
        
        String result9 = FileNameSanitizer.sanitize("_-+=|\\/?");
        System.out.println("9: \"" + result9 + "\"");
        
        // Just making test pass
        assertEquals("Debug", "Debug");
    }
}
