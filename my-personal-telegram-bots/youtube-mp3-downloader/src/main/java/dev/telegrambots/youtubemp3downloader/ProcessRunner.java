package dev.telegrambots.youtubemp3downloader;

/**
 * Interface for running external processes.
 * Separated into interface for mocking capability in tests.
 */
public interface ProcessRunner {
    /**
     * Runs process with specified parameters
     * @param command command and arguments to run
     * @return process execution result
     */
    ProcessResult runProcess(String... command);
    
    /**
     * Process execution result
     */
    class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;
        
        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isSuccess() { return exitCode == 0; }
    }
}
