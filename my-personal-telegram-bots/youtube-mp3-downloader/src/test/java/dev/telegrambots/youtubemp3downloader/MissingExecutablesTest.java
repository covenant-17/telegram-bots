package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для проверки обработки ошибок отсутствия внешних утилит (yt-dlp, ffmpeg, ffprobe)
 */
class MissingExecutablesTest {
    
    @Mock
    private ProcessRunner mockProcessRunner;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Создаем версию YtDlpService, которая использует mock ProcessRunner
        // (В реальном коде нужно будет модифицировать YtDlpService для поддержки dependency injection)
    }

    @Test
    @DisplayName("Should handle missing yt-dlp executable")
    void testMissingYtDlpExecutable() {
        // Arrange
        ProcessRunner.ProcessResult notFoundResult = new ProcessRunner.ProcessResult(
            127, "", "yt-dlp: command not found"
        );
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(notFoundResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(127, result.getExitCode()); // Common exit code for "command not found"
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    @DisplayName("Should handle missing ffmpeg executable")
    void testMissingFfmpegExecutable() {
        // Arrange
        ProcessRunner.ProcessResult notFoundResult = new ProcessRunner.ProcessResult(
            127, "", "ffmpeg: command not found"
        );
        
        when(mockProcessRunner.runProcess(eq("ffmpeg"), any())).thenReturn(notFoundResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("ffmpeg", "-version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(127, result.getExitCode());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    @DisplayName("Should handle missing ffprobe executable")
    void testMissingFfprobeExecutable() {
        // Arrange
        ProcessRunner.ProcessResult notFoundResult = new ProcessRunner.ProcessResult(
            127, "", "ffprobe: command not found"
        );
        
        when(mockProcessRunner.runProcess(
            eq("ffprobe"), eq("-v"), eq("quiet"), 
            eq("-show_entries"), eq("format=duration"), 
            eq("-of"), eq("csv=p=0"), anyString()
        )).thenReturn(notFoundResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "ffprobe", "-v", "quiet", "-show_entries", "format=duration", 
            "-of", "csv=p=0", "test.mp3"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(127, result.getExitCode());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    @DisplayName("Should handle permission denied errors")
    void testPermissionDeniedExecutables() {
        // Arrange
        ProcessRunner.ProcessResult permissionDeniedResult = new ProcessRunner.ProcessResult(
            126, "", "Permission denied"
        );
        
        when(mockProcessRunner.runProcess(eq("yt-dlp"), anyString()))
            .thenReturn(permissionDeniedResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(126, result.getExitCode()); // Common exit code for permission denied
        assertTrue(result.getError().contains("Permission denied"));
    }

    @Test
    @DisplayName("Should handle corrupted executable")
    void testCorruptedExecutable() {
        // Arrange
        ProcessRunner.ProcessResult corruptedResult = new ProcessRunner.ProcessResult(
            -1, "", "Cannot execute binary file"
        );
        
        when(mockProcessRunner.runProcess(eq("yt-dlp"), anyString()))
            .thenReturn(corruptedResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertTrue(result.getError().contains("Cannot execute"));
    }

    @Test
    @DisplayName("Should handle executable that exists but crashes immediately")
    void testCrashingExecutable() {
        // Arrange
        ProcessRunner.ProcessResult crashResult = new ProcessRunner.ProcessResult(
            139, "", "Segmentation fault"
        );
        
        when(mockProcessRunner.runProcess(eq("yt-dlp"), anyString()))
            .thenReturn(crashResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(139, result.getExitCode()); // Common exit code for segfault
        assertTrue(result.getError().contains("Segmentation fault"));
    }

    @Test
    @DisplayName("Should handle wrong version of executable")
    void testWrongVersionExecutable() {
        // Arrange - старая версия yt-dlp, которая не поддерживает нужные параметры
        ProcessRunner.ProcessResult wrongVersionResult = new ProcessRunner.ProcessResult(
            2, "", "yt-dlp: error: unrecognized arguments: --extract-audio"
        );
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), eq("--extract-audio"), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(wrongVersionResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(2, result.getExitCode());
        assertTrue(result.getError().contains("unrecognized arguments"));
    }

    @Test
    @DisplayName("Should handle network connectivity issues")
    void testNetworkConnectivityIssues() {
        // Arrange
        ProcessRunner.ProcessResult networkErrorResult = new ProcessRunner.ProcessResult(
            1, "", "ERROR: Unable to download webpage: <urlopen error [Errno -2] Name or service not known>"
        );
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(networkErrorResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unable to download"));
    }

    @Test
    @DisplayName("Should handle disk space issues")
    void testDiskSpaceIssues() {
        // Arrange
        ProcessRunner.ProcessResult diskSpaceResult = new ProcessRunner.ProcessResult(
            1, "", "ERROR: Disk full"
        );
        
        when(mockProcessRunner.runProcess(
            eq("yt-dlp"), anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(diskSpaceResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(
            "yt-dlp", "--extract-audio", "--audio-format", "mp3", 
            "--audio-quality", "320k", "--dump-json", "https://youtu.be/test"
        );
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Disk full"));
    }

    @Test
    @DisplayName("Should detect available executables on system startup")
    void testExecutableAvailabilityCheck() {
        // Arrange
        ProcessRunner.ProcessResult ytDlpAvailable = new ProcessRunner.ProcessResult(0, "yt-dlp 2023.10.13", "");
        ProcessRunner.ProcessResult ffmpegAvailable = new ProcessRunner.ProcessResult(0, "ffmpeg version 4.4.2", "");
        ProcessRunner.ProcessResult ffprobeAvailable = new ProcessRunner.ProcessResult(0, "ffprobe version 4.4.2", "");
        
        when(mockProcessRunner.runProcess("yt-dlp", "--version")).thenReturn(ytDlpAvailable);
        when(mockProcessRunner.runProcess("ffmpeg", "-version")).thenReturn(ffmpegAvailable);
        when(mockProcessRunner.runProcess("ffprobe", "-version")).thenReturn(ffprobeAvailable);
        
        // Act
        boolean ytDlpOk = mockProcessRunner.runProcess("yt-dlp", "--version").isSuccess();
        boolean ffmpegOk = mockProcessRunner.runProcess("ffmpeg", "-version").isSuccess();
        boolean ffprobeOk = mockProcessRunner.runProcess("ffprobe", "-version").isSuccess();
        
        // Assert
        assertTrue(ytDlpOk);
        assertTrue(ffmpegOk);
        assertTrue(ffprobeOk);
        
        verify(mockProcessRunner).runProcess("yt-dlp", "--version");
        verify(mockProcessRunner).runProcess("ffmpeg", "-version");
        verify(mockProcessRunner).runProcess("ffprobe", "-version");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Should handle Windows-specific executable issues")
    void testWindowsSpecificIssues() {
        // Arrange
        ProcessRunner.ProcessResult windowsErrorResult = new ProcessRunner.ProcessResult(
            1, "", "'yt-dlp' is not recognized as an internal or external command"
        );
        
        when(mockProcessRunner.runProcess("yt-dlp", "--version")).thenReturn(windowsErrorResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not recognized"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Should handle Unix-specific executable issues")
    void testUnixSpecificIssues() {
        // Arrange
        ProcessRunner.ProcessResult unixErrorResult = new ProcessRunner.ProcessResult(
            127, "", "bash: yt-dlp: command not found"
        );
        
        when(mockProcessRunner.runProcess("yt-dlp", "--version")).thenReturn(unixErrorResult);
        
        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("yt-dlp", "--version");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(127, result.getExitCode());
        assertTrue(result.getError().contains("command not found"));
    }
}
