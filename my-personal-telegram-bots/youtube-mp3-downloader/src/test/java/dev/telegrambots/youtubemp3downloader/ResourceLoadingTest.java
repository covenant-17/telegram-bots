package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.io.InputStream;

/**
 * Simple test to validate resource loading
 */
class ResourceLoadingTest {

    @Test
    void testResourceLoadingDirectly() {
        ClassLoader classLoader = ResourceLoadingTest.class.getClassLoader();
        
        // Try to find config.properties directly
        URL resource = classLoader.getResource("config.properties");
        assertNotNull(resource, "config.properties should be found by classloader");
        
        // Try to read it as InputStream
        try (InputStream is = classLoader.getResourceAsStream("config.properties")) {
            assertNotNull(is, "config.properties should be readable as stream");
        } catch (Exception e) {
            fail("Failed to read config.properties: " + e.getMessage());
        }
        
        // Try ResourceBundle
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("config", 
                java.util.Locale.getDefault(), classLoader);
            assertNotNull(bundle, "ResourceBundle should be created");
            assertTrue(bundle.containsKey("bot.token"), "ResourceBundle should contain bot.token");
            assertEquals("test_token_12345", bundle.getString("bot.token"));
        } catch (Exception e) {
            fail("Failed to load ResourceBundle: " + e.getMessage());
        }
    }
}
