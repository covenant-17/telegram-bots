package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import java.util.ResourceBundle;
import static org.junit.jupiter.api.Assertions.*;

class SimpleResourceTest {
    
    @Test
    void testCanLoadConfigResource() {
        // Простейший тест - можем ли мы загрузить config.properties?
        ResourceBundle config = ResourceBundle.getBundle("config");
        assertNotNull(config);
        
        String botToken = config.getString("bot.token");
        assertEquals("test_token_12345", botToken);
        
        System.out.println("Successfully loaded config with bot.token: " + botToken);
    }
}
