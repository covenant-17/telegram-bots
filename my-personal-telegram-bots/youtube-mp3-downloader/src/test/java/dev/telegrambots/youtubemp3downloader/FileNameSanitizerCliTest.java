package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для класса FileNameSanitizerCli
 */
class FileNameSanitizerCliTest {
    
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should show help when no arguments provided")
    void testShowHelpNoArgs() {
        // Act
        FileNameSanitizerCli.main(new String[]{});
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Usage: java -cp"));
        assertTrue(output.contains("Example: java -cp"));
    }

    @Test
    @DisplayName("Should show help when --help argument provided")
    void testShowHelpWithHelpFlag() {
        // Act
        FileNameSanitizerCli.main(new String[]{"--help"});
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Usage: java -cp"));
        assertTrue(output.contains("Example: java -cp"));
    }

    @Test
    @DisplayName("Should call sanitizeAllInDirectory with directory only")
    void testSanitizeWithDirectoryOnly() {
        try (MockedStatic<FileNameSanitizer> mockedSanitizer = mockStatic(FileNameSanitizer.class)) {
            // Act
            FileNameSanitizerCli.main(new String[]{"/test/dir"});
            
            // Assert
            mockedSanitizer.verify(() -> FileNameSanitizer.sanitizeAllInDirectory("/test/dir", ".mp3", false));
        }
    }

    @Test
    @DisplayName("Should call sanitizeAllInDirectory with directory and extension")
    void testSanitizeWithDirectoryAndExtension() {
        try (MockedStatic<FileNameSanitizer> mockedSanitizer = mockStatic(FileNameSanitizer.class)) {
            // Act
            FileNameSanitizerCli.main(new String[]{"/test/dir", ".wav"});
            
            // Assert
            mockedSanitizer.verify(() -> FileNameSanitizer.sanitizeAllInDirectory("/test/dir", ".wav", false));
        }
    }

    @Test
    @DisplayName("Should call sanitizeAllInDirectory with all parameters")
    void testSanitizeWithAllParameters() {
        try (MockedStatic<FileNameSanitizer> mockedSanitizer = mockStatic(FileNameSanitizer.class)) {
            // Act
            FileNameSanitizerCli.main(new String[]{"/test/dir", ".flac", "true"});
            
            // Assert
            mockedSanitizer.verify(() -> FileNameSanitizer.sanitizeAllInDirectory("/test/dir", ".flac", true));
        }
    }

    @Test
    @DisplayName("Should handle dry run false")
    void testSanitizeWithDryRunFalse() {
        try (MockedStatic<FileNameSanitizer> mockedSanitizer = mockStatic(FileNameSanitizer.class)) {
            // Act
            FileNameSanitizerCli.main(new String[]{"/test/dir", ".mp3", "false"});
            
            // Assert
            mockedSanitizer.verify(() -> FileNameSanitizer.sanitizeAllInDirectory("/test/dir", ".mp3", false));
        }
    }

    @Test
    @DisplayName("Should handle invalid boolean as false")
    void testSanitizeWithInvalidBoolean() {
        try (MockedStatic<FileNameSanitizer> mockedSanitizer = mockStatic(FileNameSanitizer.class)) {
            // Act
            FileNameSanitizerCli.main(new String[]{"/test/dir", ".mp3", "invalid"});
            
            // Assert
            mockedSanitizer.verify(() -> FileNameSanitizer.sanitizeAllInDirectory("/test/dir", ".mp3", false));
        }
    }
}
