package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import dev.telegrambots.shared.BaseBotConfig;
import java.util.ResourceBundle;
import static org.junit.jupiter.api.Assertions.*;

class ClassLoaderDebugTest {
    
    @Test
    void testDirectResourceLoading() {
        // Тест прямой загрузки ресурса (работает)
        ResourceBundle config = ResourceBundle.getBundle("config");
        assertNotNull(config);
        String botToken = config.getString("bot.token");
        assertEquals("test_token_12345", botToken);
        System.out.println("Direct loading works: " + botToken);
    }
    
    @Test
    void testFromBaseBotConfig() {
        // Тест через BaseBotConfig (не работает)
        try {
            // Создаем минимальный класс, наследующий BaseBotConfig
            new MinimalConfig();
            System.out.println("BaseBotConfig loading works!");
        } catch (Exception e) {
            System.out.println("BaseBotConfig loading failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Минимальный наследник BaseBotConfig для тестирования
    private static class MinimalConfig extends BaseBotConfig {
        @Override
        protected void validateConfiguration() {
            // Не делаем валидацию, чтобы увидеть, загружается ли ResourceBundle
            System.out.println("Config keys: " + config.keySet());
        }
    }
}
