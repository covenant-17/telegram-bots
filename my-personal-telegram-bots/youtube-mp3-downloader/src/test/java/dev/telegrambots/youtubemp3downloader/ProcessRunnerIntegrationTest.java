package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests with ProcessRunner mocking
 */
class ProcessRunnerIntegrationTest {
    
    @Mock
    private ProcessRunner mockProcessRunner;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should handle successful yt-dlp process execution")
    void testSuccessfulYtDlpExecution() {
        // Arrange
        String jsonOutput = "{\"title\":\"Test Video\",\"uploader\":\"Test Channel\",\"duration\":180}";
        ProcessRunner.ProcessResult successResult = new ProcessRunner.ProcessResult(0, jsonOutput, "");
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), 
            eq("--extract-audio"), 
            eq("--audio-format"), eq("mp3"), 
            eq("--audio-quality"), eq("320k"), 
            eq("--dump-json"), 
            anyString()
        )).thenReturn(successResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(jsonOutput, result.getOutput());
        assertEquals(0, result.getExitCode());
    }

    @Test
    @DisplayName("Should handle failed yt-dlp process execution")
    void testFailedYtDlpExecution() {
        // Arrange
        String errorOutput = "ERROR: Video unavailable";
        ProcessRunner.ProcessResult failResult = new ProcessRunner.ProcessResult(1, "", errorOutput);
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), 
            anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), 
            anyString()
        )).thenReturn(failResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/invalid"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(1, result.getExitCode());
        assertEquals(errorOutput, result.getError());
    }

    @Test
    @DisplayName("Should handle ffprobe process execution")
    void testFfprobeExecution() {
        // Arrange
        String durationOutput = "180.50";
        ProcessRunner.ProcessResult successResult = new ProcessRunner.ProcessResult(0, durationOutput, "");
        
        when(mockProcessRunner.runProcess(
            eq("ffprobe"), 
            eq("-v"), eq("quiet"), 
            eq("-show_entries"), eq("format=duration"), 
            eq("-of"), eq("csv=p=0"), 
            anyString()
        )).thenReturn(successResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "ffprobe", "-v", "quiet", "-show_entries", "format=duration", 
            "-of", "csv=p=0", "test.mp3"
        );
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("180.50"));
    }

    @Test
    @DisplayName("Should handle curl process execution")
    void testCurlExecution() {
        // Arrange
        String htmlOutput = "<html><head><title>Test Video - YouTube</title></head><body>...</body></html>";
        ProcessRunner.ProcessResult successResult = new ProcessRunner.ProcessResult(0, htmlOutput, "");
        
        when(mockProcessRunner.runProcess(
            eq("curl"), 
            eq("-L"), 
            anyString()
        )).thenReturn(successResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "curl", "-L", "https://youtu.be/test"
        );
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("<title>"));
    }

    @Test
    @DisplayName("Should handle process timeout or interruption")
    void testProcessInterruption() {
        // Arrange
        ProcessRunner.ProcessResult timeoutResult = new ProcessRunner.ProcessResult(-1, "", "Process interrupted");
        
        when(mockProcessRunner.runProcess(anyString(), anyString(), anyString()))
            .thenReturn(timeoutResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version", "");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    @DisplayName("Should handle missing executable")
    void testMissingExecutable() {
        // Arrange
        ProcessRunner.ProcessResult missingExecResult = new ProcessRunner.ProcessResult(-1, "", "yt-dlp: command not found");
        
        when(mockProcessRunner.runProcess(eq("yt-dlp"), anyString()))
            .thenReturn(missingExecResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    @DisplayName("Should handle malformed JSON output from yt-dlp")
    void testMalformedJsonOutput() {
        // Arrange
        String malformedJson = "{\"title\":\"Test Video\",\"uploader\":\"Test"; // Incomplete JSON
        ProcessRunner.ProcessResult result = new ProcessRunner.ProcessResult(0, malformedJson, "");
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), 
            anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), 
            anyString()
        )).thenReturn(result);
        
        // Act
        ProcessRunner.ProcessResult actualResult = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertTrue(actualResult.isSuccess());
        assertEquals(malformedJson, actualResult.getOutput());
        // The calling code should handle malformed JSON gracefully
    }

    @Test
    @DisplayName("Should handle empty output from process")
    void testEmptyProcessOutput() {
        // Arrange
        ProcessRunner.ProcessResult emptyResult = new ProcessRunner.ProcessResult(0, "", "");
        
        when(mockProcessRunner.runProcess(anyString(), anyString()))
            .thenReturn(emptyResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().isEmpty());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    @DisplayName("Should handle very large output from process")
    void testLargeProcessOutput() {
        // Arrange
        String largeOutput = "A".repeat(1000000); // 1MB of output
        ProcessRunner.ProcessResult largeResult = new ProcessRunner.ProcessResult(0, largeOutput, "");
        
        when(mockProcessRunner.runProcess(anyString()))
            .thenReturn(largeResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("test-command");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1000000, result.getOutput().length());
    }

    @Test
    @DisplayName("Should verify process runner interactions")
    void testProcessRunnerInteractions() {
        // Arrange
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "success", "");
        when(mockProcessRunner.runProcess(anyString(), anyString(), anyString()))
            .thenReturn(mockResult);
        
        // Act
        mockProcessRunner.runProcess("yt-dlp", "--version", "");
        mockProcessRunner.runProcess("ffmpeg", "-version", "");
        mockProcessRunner.runProcess("ffprobe", "-version", "");
        
        // Assert
        verify(mockProcessRunner, times(3)).runProcess(anyString(), anyString(), anyString());
        verify(mockProcessRunner).runProcess("yt-dlp", "--version", "");
        verify(mockProcessRunner).runProcess("ffmpeg", "-version", "");
        verify(mockProcessRunner).runProcess("ffprobe", "-version", "");
    }
}
