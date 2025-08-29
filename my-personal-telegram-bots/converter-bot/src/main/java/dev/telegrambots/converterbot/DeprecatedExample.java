package dev.telegrambots.converterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;

/**
 * Тестовый класс для демонстрации автоматического контроля устаревшего кода
 */
public class DeprecatedExample extends TelegramLongPollingBot {

    // Этот конструктор устарел в новых версиях API
    @SuppressWarnings("deprecation") // Временно подавляем предупреждение
    public DeprecatedExample() {
        super(); // Устаревший конструктор без параметров
    }

    @Override
    public void onUpdateReceived(org.telegram.telegrambots.meta.api.objects.Update update) {
        // Пустая реализация для теста
    }

    @Override
    public String getBotUsername() {
        return "test_bot";
    }

    // Этот метод устарел в новых версиях
    @Override
    @Deprecated
    public String getBotToken() {
        return "test_token";
    }
}
