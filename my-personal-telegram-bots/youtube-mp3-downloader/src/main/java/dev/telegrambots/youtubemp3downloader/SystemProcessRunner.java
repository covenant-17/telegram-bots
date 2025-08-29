package dev.telegrambots.youtubemp3downloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Real implementation of ProcessRunner for running external processes
 */
public class SystemProcessRunner implements ProcessRunner {
    
    @Override
    public ProcessResult runProcess(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output.toString(), "");
            
        } catch (IOException | InterruptedException e) {
            return new ProcessResult(-1, "", e.getMessage());
        }
    }
}
