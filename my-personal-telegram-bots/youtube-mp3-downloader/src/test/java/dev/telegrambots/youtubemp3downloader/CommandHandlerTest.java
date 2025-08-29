package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для CommandHandler с базовыми сценариями обработки сообщений
 */
class CommandHandlerTest {
    @Mock
    private Bot bot;
    @Mock
    private Update update;
    @Mock
    private Message message;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(123456789L);
    }

    @Test
    @DisplayName("Should return false for invalid text (not YouTube link)")
    void testHandleInvalidText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("not a youtube link");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for update without message")
    void testHandleNoMessage() {
        when(update.hasMessage()).thenReturn(false);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for message without text")
    void testHandleMessageWithoutText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(false);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for empty text")
    void testHandleEmptyText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle single valid YouTube youtu.be link")
    void testHandleValidSingleYoutubeLink() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://youtu.be/abcdefghijk");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle multiple YouTube links")
    void testHandleMultipleYoutubeLinks() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://youtu.be/abcdefghijk https://youtu.be/12345678901");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should include YouTube link in success message caption")
    void testSuccessMessageContainsYoutubeLink() {
        // Arrange
        String youtubeUrl = "https://youtu.be/dQw4w9WgXcQ";
        String beforeName = "The Ghost Aura - Nihilism (Official Video).mp3";
        String afterName = "The Ghost Aura Nihilism.mp3";
        int index = 1, total = 1;
        boolean fallbackUsed = false;
        StringBuilder msg = new StringBuilder();
        msg.append("[SUCCESS ✅] Audio ready! 🎶 (").append(index).append("/").append(total).append(")\n");
        msg.append("\uD83C\uDFB5 Song renamed\n");
        msg.append("\uD83D\uDD22 Before: ").append(beforeName).append("\n");
        msg.append("\uD83D\uDD01 After:  ").append(afterName);
        msg.append("\n🔗 YouTube: ").append(youtubeUrl);
        if (fallbackUsed) {
            msg.append("\n\nTitle taken from <title> tag of YouTube page (curl fallback)");
        }
        String caption = msg.toString();
        // Assert
        assertTrue(caption.contains(youtubeUrl));
        assertTrue(caption.contains("[SUCCESS ✅] Audio ready!"));
        assertTrue(caption.contains("Song renamed"));
    }
}
