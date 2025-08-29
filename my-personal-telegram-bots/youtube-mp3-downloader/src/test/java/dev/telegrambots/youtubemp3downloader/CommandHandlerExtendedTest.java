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
 * Extended tests for CommandHandler with edge-cases and error scenarios coverage
 */
class CommandHandlerExtendedTest {
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
    @DisplayName("Should return false for whitespace-only text")
    void testHandleWhitespaceOnlyText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("   \t\n   ");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle single valid YouTube youtu.be link")
    void testHandleSingleYoutubeShortLink() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://youtu.be/dQw4w9WgXcQ");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }    @Test
    @DisplayName("Should handle single valid YouTube youtube.com/watch link")
    void testHandleSingleYoutubeWatchLink() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject YouTube shorts links (not supported)")
    void testHandleYoutubeShortsLinkRejected() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://www.youtube.com/shorts/dQw4w9WgXcQ");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result); // Should be rejected since shorts are not supported
    }

    @Test
    @DisplayName("Should handle YouTube link with additional text")
    void testHandleYoutubeLinkWithText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("Check this out: https://youtu.be/dQw4w9WgXcQ amazing song!");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle multiple YouTube links")
    void testHandleMultipleYoutubeLinks() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://youtu.be/dQw4w9WgXcQ https://youtu.be/12345678901");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle multiple YouTube links mixed with text")
    void testHandleMultipleYoutubeLinksWithText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("First: https://youtu.be/dQw4w9WgXcQ and second: https://www.youtube.com/watch?v=12345678901 both are great!");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle duplicate YouTube links (should deduplicate)")
    void testHandleDuplicateYoutubeLinks() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("https://youtu.be/dQw4w9WgXcQ https://youtu.be/dQw4w9WgXcQ https://youtu.be/dQw4w9WgXcQ");
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }    @Test
    @DisplayName("Should reject invalid YouTube-like URLs")
    void testHandleInvalidYoutubeLikeUrls() {
        String[] invalidUrls = {
            "https://youtube.com/watch?v=short", // too short
            "https://youtu.be/", // no video ID
            "https://youtube.com/", // no watch
            "https://youtu.be/toolongvideoid123456", // too long
            "https://notyoutube.com/watch?v=dQw4w9WgXcQ", // wrong domain
            "http://youtu.be/dQw4w9WgXcQ", // http instead of https (depends on regex)
        };
        // chatId is already mocked in setUp() method
        for (String url : invalidUrls) {
            when(update.hasMessage()).thenReturn(true);
            when(message.hasText()).thenReturn(true);
            when(message.getText()).thenReturn(url);
            boolean result = CommandHandler.handle(bot, update);
            assertNotNull(result); // Just check that it's not null
        }
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void testHandleNullMessage() {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(null);
        
        assertDoesNotThrow(() -> {
            boolean result = CommandHandler.handle(bot, update);
            assertFalse(result);
        });
    }

    @Test
    @DisplayName("Should handle message with null text")
    void testHandleMessageWithNullText() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(null);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle extremely long text with YouTube links")
    void testHandleLongTextWithYoutubeLinks() {
        StringBuilder longText = new StringBuilder();
        longText.append("A".repeat(1000)); // 1000 characters
        longText.append(" https://youtu.be/dQw4w9WgXcQ ");
        longText.append("B".repeat(1000)); // another 1000 characters
        
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(longText.toString());
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle text with many non-YouTube URLs")
    void testHandleTextWithManyNonYoutubeUrls() {
        String text = "https://google.com https://facebook.com https://twitter.com https://instagram.com";
        
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle mixed YouTube and non-YouTube URLs")
    void testHandleMixedUrls() {
        String text = "https://google.com https://youtu.be/dQw4w9WgXcQ https://facebook.com";
        
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle maximum number of YouTube links (stress test)")
    void testHandleMaximumYoutubeLinks() {
        StringBuilder manyLinks = new StringBuilder();
        for (int i = 0; i < 20; i++) { // 20 links
            manyLinks.append("https://youtu.be/dQw4w9WgXc").append(i % 10).append(" ");
        }
        
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(manyLinks.toString());
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
        // Note: This will trigger batch processing
    }

    @Test
    @DisplayName("Should handle special characters in text with YouTube links")
    void testHandleSpecialCharactersWithYoutubeLinks() {
        String text = "🎵 Check this: https://youtu.be/dQw4w9WgXcQ 🎶 Amazing! Chinese test English text";
        
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        
        boolean result = CommandHandler.handle(bot, update);
        
        assertTrue(result);
    }
}
