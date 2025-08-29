package dev.telegrambots.converterbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {
    @Test
    void testMainRuns() {
        // Простой smoke-тест
        Main.main(new String[] {});
        assertTrue(true);
    }
}
