package dev.telegrambots.youtubemp3downloader;

/**
 * Интерфейс для запуска внешних процессов. 
 * Выделен в отдельный интерфейс для возможности мокирования в тестах.
 */
public interface ProcessRunner {
    /**
     * Запускает процесс с заданными параметрами
     * @param command команда и аргументы для запуска
     * @return результат выполнения процесса
     */
    ProcessResult runProcess(String... command);
    
    /**
     * Результат выполнения процесса
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
