package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for various utility scenarios
 */
class UtilityAndEdgeCaseTest {

    private YtDlpService service;

    @BeforeEach
    void setUp() {
        service = new YtDlpService("yt-dlp", "ffmpeg", "ffprobe", 50 * 1024 * 1024, 10.0);
    }

    @Nested
    @DisplayName("ProcessResult Edge Cases")
    class ProcessResultEdgeCases {

        @Test
        @DisplayName("Should handle ProcessResult with extreme values")
        void testProcessResultExtremeValues() {
            // Test with maximum integer values
            ProcessRunner.ProcessResult maxResult = new ProcessRunner.ProcessResult(
                    Integer.MAX_VALUE, "max output", "max error");

            assertEquals(Integer.MAX_VALUE, maxResult.getExitCode());
            assertFalse(maxResult.isSuccess());
            assertEquals("max output", maxResult.getOutput());
            assertEquals("max error", maxResult.getError());

            // Test with minimum integer values
            ProcessRunner.ProcessResult minResult = new ProcessRunner.ProcessResult(
                    Integer.MIN_VALUE, "", "");

            assertEquals(Integer.MIN_VALUE, minResult.getExitCode());
            assertFalse(minResult.isSuccess());
        }

        @Test
        @DisplayName("Should handle ProcessResult with null values")
        void testProcessResultNullValues() {
            ProcessRunner.ProcessResult nullResult = new ProcessRunner.ProcessResult(0, null, null);

            assertEquals(0, nullResult.getExitCode());
            assertTrue(nullResult.isSuccess());
            assertNull(nullResult.getOutput());
            assertNull(nullResult.getError());
        }

        @Test
        @DisplayName("Should handle ProcessResult with empty strings")
        void testProcessResultEmptyStrings() {
            ProcessRunner.ProcessResult emptyResult = new ProcessRunner.ProcessResult(0, "", "");

            assertEquals(0, emptyResult.getExitCode());
            assertTrue(emptyResult.isSuccess());
            assertEquals("", emptyResult.getOutput());
            assertEquals("", emptyResult.getError());
        }

        @Test
        @DisplayName("Should handle ProcessResult boundary exit codes")
        void testProcessResultBoundaryExitCodes() {
            int[] boundaryExitCodes = { -1, 0, 1, 127, 128, 255, 256 };

            for (int exitCode : boundaryExitCodes) {
                ProcessRunner.ProcessResult result = new ProcessRunner.ProcessResult(
                        exitCode, "output", "error");

                assertEquals(exitCode, result.getExitCode());
                assertEquals(exitCode == 0, result.isSuccess());
            }
        }
    }

    @Nested
    @DisplayName("YtDlpService Boundary Tests")
    class YtDlpServiceBoundaryTests {
        @Test
        @DisplayName("Should handle negative duration limits")
        void testNegativeDurationLimits() {
            YtDlpService negativeLimitService = new YtDlpService(
                    "yt-dlp", "ffmpeg", "ffprobe", 1024 * 1024, -5.0);

            assertTrue(negativeLimitService.isDurationWithinLimit(300)); // Should allow all
            assertTrue(negativeLimitService.isDurationWithinLimit(0));
            assertTrue(negativeLimitService.isDurationWithinLimit(-1));
        }

        @Test
        @DisplayName("Should handle zero file size limits")
        void testZeroFileSizeLimits() {
            YtDlpService zeroSizeService = new YtDlpService(
                    "yt-dlp", "ffmpeg", "ffprobe", 0, 10.0);

            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.length()).thenReturn(1L);

            assertFalse(zeroSizeService.isFileSizeWithinLimit(mockFile));
        }

        @Test
        @DisplayName("Should handle maximum file size limits")
        void testMaximumFileSizeLimits() {
            YtDlpService maxSizeService = new YtDlpService(
                    "yt-dlp", "ffmpeg", "ffprobe", Long.MAX_VALUE, 10.0);

            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.length()).thenReturn(Long.MAX_VALUE - 1);

            assertTrue(maxSizeService.isFileSizeWithinLimit(mockFile));
        }

        @Test
        @DisplayName("Should handle file length overflow scenarios")
        void testFileLengthOverflow() {
            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.length()).thenReturn(Long.MAX_VALUE);

            // Should handle gracefully without overflow
            assertDoesNotThrow(() -> service.isFileSizeWithinLimit(mockFile));
        }
    }

    @Nested
    @DisplayName("String Processing Edge Cases")
    class StringProcessingEdgeCases {

        @Test
        @DisplayName("Should handle FileNameSanitizer with control characters")
        void testControlCharacters() {
            String controlChars = "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\n\u000B\u000C\r\u000E\u000F";
            String result = FileNameSanitizer.sanitize("Test" + controlChars + "Song");

            assertNotNull(result);
            assertTrue(result.contains("Test"));
            assertTrue(result.contains("Song"));
        }

        @Test
        @DisplayName("Should handle very long strings")
        void testVeryLongStrings() {
            String veryLong = "A".repeat(100000);
            String result = FileNameSanitizer.sanitize(veryLong);

            assertNotNull(result);
            assertTrue(result.length() <= veryLong.length());
        }

        @Test
        @DisplayName("Should handle strings with only whitespace")
        void testWhitespaceOnlyStrings() {
            String[] whitespaceStrings = {
                    " ", "  ", "\t", "\n", "\r", "\r\n", "\t\n\r   "
            };

            for (String ws : whitespaceStrings) {
                String result = FileNameSanitizer.sanitize(ws);
                assertTrue(result == null || result.trim().isEmpty());
            }
        }

        @Test
        @DisplayName("Should handle mixed encoding strings")
        void testMixedEncodingStrings() {
            String mixed = "ASCII + UTF-8: àáâãäå + Cyrillic: test + Emoji: 🎵🎶";
            String result = FileNameSanitizer.sanitize(mixed);

            assertNotNull(result);
            assertTrue(result.contains("Ascii"));
            assertTrue(result.contains("Utf 8"));
            assertTrue(result.contains("Cyrillic"));
            assertTrue(result.contains("Emoji"));
        }
    }

    @Nested
    @DisplayName("Threading and Concurrency Tests")
    class ThreadingAndConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent file size checks")
        void testConcurrentFileSizeChecks() throws InterruptedException {
            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.length()).thenReturn(1024L);

            Thread[] threads = new Thread[50];
            boolean[] results = new boolean[50];

            for (int i = 0; i < 50; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    results[index] = service.isFileSizeWithinLimit(mockFile);
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for completion
            for (Thread thread : threads) {
                thread.join(1000);
            }

            // All should return true
            for (boolean result : results) {
                assertTrue(result);
            }
        }

        @Test
        @DisplayName("Should handle concurrent duration checks")
        void testConcurrentDurationChecks() throws InterruptedException {
            Thread[] threads = new Thread[50];
            boolean[] results = new boolean[50];

            for (int i = 0; i < 50; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    results[index] = service.isDurationWithinLimit(300); // 5 minutes
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for completion
            for (Thread thread : threads) {
                thread.join(1000);
            }

            // All should return true (5 minutes < 10 minute limit)
            for (boolean result : results) {
                assertTrue(result);
            }
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should handle resource cleanup properly")
        void testResourceCleanup() {
            File mockFile = mock(File.class);
            when(mockFile.exists()).thenReturn(true);
            when(mockFile.delete()).thenReturn(true);

            // Should not throw exception during cleanup
            assertDoesNotThrow(() -> service.deleteFileIfExists(mockFile));
            verify(mockFile).delete();
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void testNullParameterHandling() {
            // Should handle null file gracefully
            assertDoesNotThrow(() -> service.deleteFileIfExists(null));
            assertFalse(service.isFileSizeWithinLimit(null));
        }
    }

    @Test
    @DisplayName("Should handle class instantiation edge cases")
    void testClassInstantiationEdgeCases() {
        // Test creating multiple service instances
        for (int i = 0; i < 10; i++) {
            YtDlpService newService = new YtDlpService(
                    "yt-dlp" + i, "ffmpeg" + i, "ffprobe" + i,
                    i * 1024 * 1024, i * 1.0);
            assertNotNull(newService);
        }
    }

    @Test
    @DisplayName("Should handle configuration edge cases")
    void testConfigurationEdgeCases() {
        // Test with extreme configuration values
        YtDlpService extremeService = new YtDlpService(
                "", "", "", -1, -1.0);

        assertNotNull(extremeService);

        // Should handle null file gracefully
        assertFalse(extremeService.isFileSizeWithinLimit(null));
        assertTrue(extremeService.isDurationWithinLimit(0));
    }

}
