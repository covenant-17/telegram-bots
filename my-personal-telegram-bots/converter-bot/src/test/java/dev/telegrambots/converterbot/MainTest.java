package dev.telegrambots.converterbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {
    @Test
    void testMainRuns() {
        // Simple smoke test
        Main.main(new String[] {});
        assertTrue(true);
    }
}
