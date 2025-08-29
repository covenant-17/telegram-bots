package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для YtDlpService с реальными файлами
 */
class YtDlpServiceFileIntegrationTest {

    @Mock
    private ProcessRunner mockProcessRunner;
    
    private YtDlpService service;
    
    @TempDir
    Path tempDir;    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new YtDlpService("yt-dlp", "ffmpeg", "ffprobe", 50*1024*1024, 10.0);
    }

    @Test
    @DisplayName("Should handle real file size checking")
    void testRealFileSizeChecking() throws IOException {
        // Arrange - create real temporary files
        Path smallFile = tempDir.resolve("small.mp3");
        Path largeFile = tempDir.resolve("large.mp3");
        
        Files.write(smallFile, "small content".getBytes());
        Files.write(largeFile, new byte[100*1024*1024]); // 100MB file
        
        // Act & Assert
        assertTrue(service.isFileSizeWithinLimit(smallFile.toFile()));
        assertFalse(service.isFileSizeWithinLimit(largeFile.toFile()));
    }

    @Test
    @DisplayName("Should handle file deletion correctly")
    void testRealFileDeletion() throws IOException {
        // Arrange - create real temporary file
        Path testFile = tempDir.resolve("test_delete.mp3");
        Files.write(testFile, "test content".getBytes());
        assertTrue(Files.exists(testFile));
        
        // Act
        service.deleteFileIfExists(testFile.toFile());
        
        // Assert
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("Should handle multiple file operations")
    void testMultipleFileOperations() throws IOException {
        // Arrange - create multiple files
        for (int i = 0; i < 10; i++) {
            Path file = tempDir.resolve("file" + i + ".mp3");
            Files.write(file, ("content " + i).getBytes());
        }
        
        // Act - check all files
        List<Path> files = Files.list(tempDir).toList();
        
        // Assert
        assertEquals(10, files.size());
        for (Path file : files) {
            assertTrue(service.isFileSizeWithinLimit(file.toFile()));
            service.deleteFileIfExists(file.toFile());
            assertFalse(Files.exists(file));
        }
    }

    @Test
    @DisplayName("Should handle directory with mixed file types")
    void testMixedFileTypes() throws IOException {
        // Arrange - create files with different extensions
        String[] extensions = {".mp3", ".wav", ".flac", ".m4a", ".txt", ".json"};
        for (String ext : extensions) {
            Path file = tempDir.resolve("test" + ext);
            Files.write(file, "test content".getBytes());
        }
        
        // Act & Assert
        List<Path> allFiles = Files.list(tempDir).toList();
        assertEquals(6, allFiles.size());
        
        for (Path file : allFiles) {
            assertTrue(service.isFileSizeWithinLimit(file.toFile()));
        }
    }

    @Test
    @DisplayName("Should handle empty directory")
    void testEmptyDirectory() throws IOException {
        // Arrange - empty temp directory
        List<Path> files = Files.list(tempDir).toList();
        assertTrue(files.isEmpty());
        
        // Act - try to list files
        File[] dirFiles = tempDir.toFile().listFiles();
        
        // Assert
        assertNotNull(dirFiles);
        assertEquals(0, dirFiles.length);
    }

    @Test
    @DisplayName("Should handle nested directory structure")
    void testNestedDirectories() throws IOException {
        // Arrange - create nested directories
        Path subDir1 = tempDir.resolve("sub1");
        Path subDir2 = tempDir.resolve("sub1/sub2");
        Files.createDirectories(subDir2);
        
        Path file1 = subDir1.resolve("file1.mp3");
        Path file2 = subDir2.resolve("file2.mp3");
        Files.write(file1, "content1".getBytes());
        Files.write(file2, "content2".getBytes());
        
        // Act & Assert
        assertTrue(service.isFileSizeWithinLimit(file1.toFile()));
        assertTrue(service.isFileSizeWithinLimit(file2.toFile()));
        
        service.deleteFileIfExists(file1.toFile());
        service.deleteFileIfExists(file2.toFile());
        
        assertFalse(Files.exists(file1));
        assertFalse(Files.exists(file2));
    }

    @Test
    @DisplayName("Should handle files with special characters in names")
    void testSpecialCharacterFileNames() throws IOException {
        // Arrange - create files with special characters
        String[] specialNames = {
            "file with spaces.mp3",
            "file-with-dashes.mp3",
            "file_with_underscores.mp3",
            "file.with.dots.mp3",
            "file(with)parentheses.mp3"
        };
        
        for (String name : specialNames) {
            Path file = tempDir.resolve(name);
            Files.write(file, "content".getBytes());
            
            // Act & Assert
            assertTrue(service.isFileSizeWithinLimit(file.toFile()));
            assertTrue(Files.exists(file));
        }
    }

    @Test
    @DisplayName("Should handle concurrent file operations")
    void testConcurrentFileOperations() throws IOException, InterruptedException {
        // Arrange - create multiple files
        for (int i = 0; i < 50; i++) {
            Path file = tempDir.resolve("concurrent_file" + i + ".mp3");
            Files.write(file, ("content " + i).getBytes());
        }
        
        // Act - check files concurrently
        List<Path> files = Files.list(tempDir).toList();
        
        files.parallelStream().forEach(file -> {
            assertTrue(service.isFileSizeWithinLimit(file.toFile()));
        });
        
        // Assert - all files should still exist
        assertEquals(50, Files.list(tempDir).count());
    }

    @Test
    @DisplayName("Should handle very long file paths")
    void testLongFilePaths() throws IOException {
        // Arrange - create file with very long name
        String longName = "a".repeat(200) + ".mp3";
        Path longFile = tempDir.resolve(longName);
        
        try {
            Files.write(longFile, "content".getBytes());
            
            // Act & Assert
            assertTrue(service.isFileSizeWithinLimit(longFile.toFile()));
            service.deleteFileIfExists(longFile.toFile());
            assertFalse(Files.exists(longFile));
        } catch (IOException e) {
            // Some systems may not support very long filenames
            // This is acceptable behavior
            assertTrue(e.getMessage().contains("name too long") || 
                       e.getMessage().contains("Invalid argument"));
        }
    }

    @Test
    @DisplayName("Should handle read-only files")
    void testReadOnlyFiles() throws IOException {
        // Arrange - create read-only file
        Path readOnlyFile = tempDir.resolve("readonly.mp3");
        Files.write(readOnlyFile, "readonly content".getBytes());
        readOnlyFile.toFile().setReadOnly();
        
        // Act & Assert - should still be able to check size
        assertTrue(service.isFileSizeWithinLimit(readOnlyFile.toFile()));
        
        // Note: Deletion may fail on read-only files, which is expected behavior
    }

    @Test
    @DisplayName("Should handle symbolic links if supported")
    void testSymbolicLinks() throws IOException {
        // Arrange - create original file
        Path originalFile = tempDir.resolve("original.mp3");
        Files.write(originalFile, "original content".getBytes());
        
        try {
            // Try to create symbolic link
            Path linkFile = tempDir.resolve("link.mp3");
            Files.createSymbolicLink(linkFile, originalFile);
            
            // Act & Assert
            assertTrue(service.isFileSizeWithinLimit(linkFile.toFile()));
            assertTrue(service.isFileSizeWithinLimit(originalFile.toFile()));
        } catch (UnsupportedOperationException | IOException e) {
            // Symbolic links not supported on this system - that's okay
        }
    }
}
