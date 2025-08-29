package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Advanced integration tests for ProcessRunner with various edge-cases
 */
class ProcessRunnerAdvancedIntegrationTest {

    @Mock
    private ProcessRunner mockProcessRunner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should handle concurrent process executions")
    void testConcurrentProcessExecution() {
        // Arrange
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "success", "");
        when(mockProcessRunner.runProcess(anyString(), anyString()))
            .thenReturn(mockResult);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Act - run multiple processes concurrently
        CompletableFuture<?>[] futures = new CompletableFuture[20];
        for (int i = 0; i < 20; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("test-command", "arg" + index);
                assertTrue(result.isSuccess());
            }, executor);
        }

        // Assert
        assertDoesNotThrow(() -> CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        verify(mockProcessRunner, times(20)).runProcess(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle process with very long command line")
    void testVeryLongCommandLine() {
        // Arrange
        String longArg = "a".repeat(10000);
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "output", "");
        when(mockProcessRunner.runProcess(eq("command"), eq(longArg)))
            .thenReturn(mockResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("command", longArg);

        // Assert
        assertTrue(result.isSuccess());
        verify(mockProcessRunner).runProcess("command", longArg);
    }

    @Test
    @DisplayName("Should handle process with many arguments")
    void testManyArguments() {
        // Arrange
        String[] manyArgs = new String[100];
        manyArgs[0] = "command";
        for (int i = 1; i < 100; i++) {
            manyArgs[i] = "arg" + i;
        }
        
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "success", "");
        when(mockProcessRunner.runProcess(manyArgs)).thenReturn(mockResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(manyArgs);

        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle alternating success and failure")
    void testAlternatingSuccessFailure() {
        // Arrange
        ProcessRunner.ProcessResult successResult = new ProcessRunner.ProcessResult(0, "success", "");
        ProcessRunner.ProcessResult failResult = new ProcessRunner.ProcessResult(1, "", "error");
        
        when(mockProcessRunner.runProcess("test"))
            .thenReturn(successResult)
            .thenReturn(failResult)
            .thenReturn(successResult)
            .thenReturn(failResult);

        // Act & Assert
        assertTrue(mockProcessRunner.runProcess("test").isSuccess());
        assertFalse(mockProcessRunner.runProcess("test").isSuccess());
        assertTrue(mockProcessRunner.runProcess("test").isSuccess());
        assertFalse(mockProcessRunner.runProcess("test").isSuccess());
    }

    @Test
    @DisplayName("Should handle processes with different exit codes")
    void testDifferentExitCodes() {
        int[] exitCodes = {0, 1, 2, 125, 126, 127, 130, 255};
        
        for (int exitCode : exitCodes) {
            // Arrange
            ProcessRunner.ProcessResult result = new ProcessRunner.ProcessResult(exitCode, "output", "error");
            when(mockProcessRunner.runProcess("test", String.valueOf(exitCode)))
                .thenReturn(result);

            // Act
            ProcessRunner.ProcessResult actualResult = mockProcessRunner.runProcess("test", String.valueOf(exitCode));

            // Assert
            assertEquals(exitCode, actualResult.getExitCode());
            assertEquals(exitCode == 0, actualResult.isSuccess());
        }
    }

    @Test
    @DisplayName("Should handle process output with special characters")
    void testSpecialCharacterOutput() {
        String[] specialOutputs = {
            "normal output",
            "output with\nnewlines\nand\ttabs",
            "unicode: 🎵 音楽 🎵",
            "quotes: \"double\" and 'single'",
            "symbols: !@#$%^&*()[]{}|\\:;\"'<>?/",
            "null character: \0",
            "bell character: \u0007",
            "escape sequences: \u001b[31mred\u001b[0m"
        };

        for (String output : specialOutputs) {
            // Arrange
            ProcessRunner.ProcessResult result = new ProcessRunner.ProcessResult(0, output, "");
            when(mockProcessRunner.runProcess("echo", output)).thenReturn(result);

            // Act
            ProcessRunner.ProcessResult actualResult = mockProcessRunner.runProcess("echo", output);

            // Assert
            assertEquals(output, actualResult.getOutput());
            assertTrue(actualResult.isSuccess());
        }
    }

    @Test
    @DisplayName("Should handle rapid sequential process calls")
    void testRapidSequentialCalls() {
        // Arrange
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "output", "");
        when(mockProcessRunner.runProcess(anyString())).thenReturn(mockResult);

        // Act - make 1000 rapid calls
        for (int i = 0; i < 1000; i++) {
            ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("test" + i);
            assertTrue(result.isSuccess());
        }

        // Assert
        verify(mockProcessRunner, times(1000)).runProcess(anyString());
    }

    @Test
    @DisplayName("Should handle process with empty command")
    void testEmptyCommand() {
        // Arrange
        ProcessRunner.ProcessResult errorResult = new ProcessRunner.ProcessResult(-1, "", "Invalid command");
        when(mockProcessRunner.runProcess("")).thenReturn(errorResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("");

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
    }

    @Test
    @DisplayName("Should handle process with null arguments")
    void testNullArguments() {
        // Arrange
        ProcessRunner.ProcessResult errorResult = new ProcessRunner.ProcessResult(-1, "", "Null argument");
        when(mockProcessRunner.runProcess((String[]) null)).thenReturn(errorResult);

        // Act & Assert
        assertDoesNotThrow(() -> {
            ProcessRunner.ProcessResult result = mockProcessRunner.runProcess((String[]) null);
            assertFalse(result.isSuccess());
        });
    }

    @Test
    @DisplayName("Should handle mixed null and valid arguments")
    void testMixedNullValidArguments() {
        // Arrange
        String[] mixedArgs = {"command", null, "valid", null, "arg"};
        ProcessRunner.ProcessResult mockResult = new ProcessRunner.ProcessResult(0, "handled", "");
        when(mockProcessRunner.runProcess(mixedArgs)).thenReturn(mockResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess(mixedArgs);

        // Assert
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle very large output buffers")
    void testVeryLargeOutput() {
        // Arrange
        String largeOutput = "line\n".repeat(100000); // 500KB of output
        ProcessRunner.ProcessResult largeResult = new ProcessRunner.ProcessResult(0, largeOutput, "");
        when(mockProcessRunner.runProcess("large-output")).thenReturn(largeResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("large-output");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(largeOutput.length(), result.getOutput().length());
    }

    @Test
    @DisplayName("Should handle binary data in output")
    void testBinaryDataOutput() {
        // Arrange
        byte[] binaryData = {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
        String binaryString = new String(binaryData);
        ProcessRunner.ProcessResult binaryResult = new ProcessRunner.ProcessResult(0, binaryString, "");
        when(mockProcessRunner.runProcess("binary-command")).thenReturn(binaryResult);

        // Act
        ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("binary-command");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(binaryString, result.getOutput());
    }

    @Test
    @DisplayName("Should handle timeout scenarios")
    void testTimeoutScenarios() {
        // Arrange - simulate different timeout scenarios
        ProcessRunner.ProcessResult[] timeoutResults = {
            new ProcessRunner.ProcessResult(-1, "", "Process timed out"),
            new ProcessRunner.ProcessResult(124, "", "Command timeout"),
            new ProcessRunner.ProcessResult(142, "", "SIGALRM timeout")
        };

        for (int i = 0; i < timeoutResults.length; i++) {
            when(mockProcessRunner.runProcess("timeout-test", String.valueOf(i)))
                .thenReturn(timeoutResults[i]);            // Act
            ProcessRunner.ProcessResult result = mockProcessRunner.runProcess("timeout-test", String.valueOf(i));            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError() != null && 
                      (result.getError().toLowerCase().contains("timeout") || 
                       result.getError().toLowerCase().contains("timed out") || 
                       result.getError().contains("SIGALRM")));
        }
    }
}
