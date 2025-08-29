package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TelegramService class
 */
class TelegramServiceTest {
    
    @Mock
    private Bot mockBot;
    
    private TelegramService telegramService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telegramService = new TelegramService(mockBot);
    }

    @Test
    @DisplayName("Should send text message successfully")
    void testSendText() {
        // Arrange
        Long chatId = 123456789L;
        String text = "Test message";
        
        // Act
        telegramService.sendText(chatId, text);
        
        // Assert
        verify(mockBot).sendTextMessage(chatId, text);
    }

    @Test
    @DisplayName("Should handle exception when sending text")
    void testSendTextWithException() {
        // Arrange
        Long chatId = 123456789L;
        String text = "Test message";
        doThrow(new RuntimeException("Test exception")).when(mockBot).sendTextMessage(chatId, text);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> telegramService.sendText(chatId, text));
    }

    @Test
    @DisplayName("Should send audio file successfully")
    void testSendAudio() {
        // Arrange
        String chatId = "123456789";
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("test.mp3");
        
        // Act
        telegramService.sendAudio(chatId, mockFile);
        
        // Assert
        verify(mockBot).sendAudioFile(chatId, mockFile);
    }

    @Test
    @DisplayName("Should handle exception when sending audio")
    void testSendAudioWithException() {
        // Arrange
        String chatId = "123456789";
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("test.mp3");
        doThrow(new RuntimeException("Test exception")).when(mockBot).sendAudioFile(chatId, mockFile);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> telegramService.sendAudio(chatId, mockFile));
    }

    @Test
    @DisplayName("Should send audio file with caption successfully")
    void testSendAudioWithCaption() {
        // Arrange
        String chatId = "123456789";
        File mockFile = mock(File.class);
        String caption = "Custom caption";
        when(mockFile.getName()).thenReturn("test.mp3");
        
        // Act
        telegramService.sendAudio(chatId, mockFile, caption);
        
        // Assert
        verify(mockBot).sendAudioFile(chatId, mockFile, caption);
    }

    @Test
    @DisplayName("Should handle exception when sending audio with caption")
    void testSendAudioWithCaptionException() {
        // Arrange
        String chatId = "123456789";
        File mockFile = mock(File.class);
        String caption = "Custom caption";
        when(mockFile.getName()).thenReturn("test.mp3");
        doThrow(new RuntimeException("Test exception")).when(mockBot).sendAudioFile(chatId, mockFile, caption);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> telegramService.sendAudio(chatId, mockFile, caption));
    }

    @Test
    @DisplayName("Should send audio file with caption containing YouTube link")
    void testSendAudioWithYoutubeLinkInCaption() {
        String chatId = "123456789";
        File mockFile = mock(File.class);
        String youtubeUrl = "https://youtu.be/dQw4w9WgXcQ";
        String caption = "[SUCCESS ✅] Audio ready! 🎶 (1/1)\n\uD83C\uDFB5 Song renamed\n\uD83D\uDD22 Before: The Ghost Aura - Nihilism (Official Video).mp3\n\uD83D\uDD01 After:  The Ghost Aura Nihilism.mp3\n🔗 YouTube: " + youtubeUrl;
        when(mockFile.getName()).thenReturn("The Ghost Aura Nihilism.mp3");
        // Act
        telegramService.sendAudio(chatId, mockFile, caption);
        // Assert
        verify(mockBot).sendAudioFile(chatId, mockFile, caption);
    }

    @Test
    @DisplayName("Should send chat action successfully")
    void testSendChatAction() {
        // Arrange
        Long chatId = 123456789L;
        ActionType action = ActionType.TYPING;
        
        // Act
        telegramService.sendChatAction(chatId, action);
        
        // Assert
        verify(mockBot).sendChatAction(chatId, action);
    }

    @Test
    @DisplayName("Should handle exception when sending chat action")
    void testSendChatActionWithException() {
        // Arrange
        Long chatId = 123456789L;
        ActionType action = ActionType.TYPING;
        doThrow(new RuntimeException("Test exception")).when(mockBot).sendChatAction(chatId, action);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> telegramService.sendChatAction(chatId, action));
    }
}
