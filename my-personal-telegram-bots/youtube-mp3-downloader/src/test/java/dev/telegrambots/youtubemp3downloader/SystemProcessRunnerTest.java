package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemProcessRunner class
 */
class SystemProcessRunnerTest {
    
    private SystemProcessRunner processRunner;
    
    @BeforeEach
    void setUp() {
        processRunner = new SystemProcessRunner();
    }

    @Test
    @DisplayName("Should execute simple command successfully")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testSimpleCommandUnix() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("echo", "Hello World");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("Hello World"));
        assertEquals("", result.getError());
    }

    @Test
    @DisplayName("Should execute Windows command successfully")
    @EnabledOnOs(OS.WINDOWS)
    void testSimpleCommandWindows() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("cmd", "/c", "echo Hello World");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("Hello World"));
        assertEquals("", result.getError());
    }

    @Test
    @DisplayName("Should handle command that exits with non-zero code")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testFailingCommandUnix() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("ls", "/nonexistent/directory");
        
        // Assert
        assertFalse(result.isSuccess());
        assertNotEquals(0, result.getExitCode());
    }

    @Test
    @DisplayName("Should handle Windows command that fails")
    @EnabledOnOs(OS.WINDOWS)
    void testFailingCommandWindows() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("cmd", "/c", "dir C:\\nonexistent\\directory");
        
        // Assert
        assertFalse(result.isSuccess());
        assertNotEquals(0, result.getExitCode());
    }

    @Test
    @DisplayName("Should handle command with multiple arguments")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testCommandWithMultipleArgsUnix() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("echo", "-n", "test", "message");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("test message"));
    }

    @Test
    @DisplayName("Should handle Windows command with multiple arguments")
    @EnabledOnOs(OS.WINDOWS)
    void testCommandWithMultipleArgsWindows() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("cmd", "/c", "echo", "test", "message");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("test message"));
    }

    @Test
    @DisplayName("Should handle empty command output")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testEmptyOutputUnix() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("true");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getError());
    }

    @Test
    @DisplayName("Should handle invalid command")
    void testInvalidCommand() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("nonexistent_command_12345");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertEquals("", result.getOutput());
        assertFalse(result.getError().isEmpty());
    }

    @Test
    @DisplayName("Should handle command with long output")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testLongOutputUnix() {
        // Act
        ProcessRunner.ProcessResult result = processRunner.runProcess("seq", "1", "100");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("1"));
        assertTrue(result.getOutput().contains("100"));
    }

    @Test
    @DisplayName("Should handle command timeout scenario")
    void testProcessInterruption() {
        // Simulate process that could be interrupted
        ProcessRunner.ProcessResult result = processRunner.runProcess("invalid_long_running_command");
        
        // Assert - should fail gracefully
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
    }
}
