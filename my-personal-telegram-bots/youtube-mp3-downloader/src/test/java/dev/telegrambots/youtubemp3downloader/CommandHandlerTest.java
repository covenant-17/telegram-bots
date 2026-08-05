package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CommandHandler with basic message processing scenarios
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
    @DisplayName("Should handle start command and log sender details")
    void testHandleStartCommand() {
        User user = new User(42L, "Boss", false);
        user.setLastName("Sender");
        user.setUserName("boss_suck_my_bot");
        Chat chat = new Chat(123456789L, "private");

        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        when(message.getFrom()).thenReturn(user);
        when(message.getChat()).thenReturn(chat);

        boolean result = CommandHandler.handle(bot, update);

        assertTrue(result);
        verify(bot).sendTextMessage(eq(123456789L), contains("YouTube link"));
        assertEquals(
                "userId=42 username=@boss_suck_my_bot firstName=Boss lastName=Sender chatId=123456789 chatType=private",
                CommandHandler.senderLogLine(message)
        );
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

    @Test
    @DisplayName("Should reject URL-like metadata names")
    void testRejectsUrlLikeMetadataNames() {
        assertTrue(CommandHandler.isUnsafeMetadataName("https://www.youtube.com/watch?v=4DVdqY5KwXw"));
        assertTrue(CommandHandler.isUnsafeMetadataName("Https:"));
        assertTrue(CommandHandler.isUnsafeMetadataName("www.youtube.com watch v 4DVdqY5KwXw"));
        assertFalse(CommandHandler.isUnsafeMetadataName("Билборды Стертые Слова"));
    }

    @Test
    @DisplayName("Should build stable fallback file name from YouTube id")
    void testFallbackBaseFileNameUsesVideoId() {
        assertEquals("video-4DVdqY5KwXw",
                CommandHandler.fallbackBaseFileName("https://www.youtube.com/watch?v=4DVdqY5KwXw"));
        assertEquals("video", CommandHandler.fallbackBaseFileName("not-a-youtube-url"));
    }

    @Test
    @DisplayName("Should format metadata fallback warning line")
    void testMetadataFallbackWarningLine() {
        assertEquals("- https://www.youtube.com/watch?v=4DVdqY5KwXw -> video-4DVdqY5KwXw.mp3",
                CommandHandler.metadataFallbackWarningLine(
                        "https://www.youtube.com/watch?v=4DVdqY5KwXw",
                        "video-4DVdqY5KwXw.mp3"));
    }

    @Test
    @DisplayName("Should detect MP3 sanitize Telegram commands")
    void testDetectsSanitizeMp3Commands() {
        assertTrue(CommandHandler.isSanitizeMp3Command("/sanitize_mp3"));
        assertTrue(CommandHandler.isSanitizeMp3Command("/sanitize_mp3@YoutubeMp3Bot dry"));
        assertFalse(CommandHandler.isSanitizeMp3Command("/delete_mp3"));
        assertTrue(CommandHandler.isDeleteMp3Command("/delete_mp3"));
        assertTrue(CommandHandler.isDeleteMp3Command("/delete_mp3@YoutubeMp3Bot"));
        assertFalse(CommandHandler.isSanitizeMp3Command("/start"));
        assertFalse(CommandHandler.isDeleteMp3Command("/sanitize_mp3"));
        assertFalse(CommandHandler.isSanitizeMp3Command("https://youtu.be/dQw4w9WgXcQ"));
    }

    @Test
    @DisplayName("Should delete MP3 files recursively in workzone")
    void testDeleteMp3FilesInDirectory(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path nested = java.nio.file.Files.createDirectory(tempDir.resolve("nested"));
        java.nio.file.Path first = java.nio.file.Files.writeString(tempDir.resolve("first.mp3"), "audio");
        java.nio.file.Path second = java.nio.file.Files.writeString(nested.resolve("second.mp3"), "audio");
        java.nio.file.Path untouched = java.nio.file.Files.writeString(tempDir.resolve("notes.txt"), "text");

        CommandHandler.DeleteMp3Result result = CommandHandler.deleteMp3FilesInDirectory(tempDir.toFile());

        assertEquals(2, result.total());
        assertEquals(2, result.deleted());
        assertEquals(0, result.failed());
        assertFalse(java.nio.file.Files.exists(first));
        assertFalse(java.nio.file.Files.exists(second));
        assertTrue(java.nio.file.Files.exists(untouched));
        assertTrue(CommandHandler.buildDeleteMp3Summary(result).contains("MP3 delete complete"));
    }

    @Test
    @DisplayName("Should detect dry-run flag for MP3 sanitize command")
    void testDetectsSanitizeMp3DryRun() {
        assertTrue(CommandHandler.isSanitizeMp3DryRun("/sanitize_mp3 dry"));
        assertTrue(CommandHandler.isSanitizeMp3DryRun("/sanitize_mp3 preview"));
        assertTrue(CommandHandler.isSanitizeMp3DryRun("/sanitize_mp3 true"));
        assertFalse(CommandHandler.isSanitizeMp3DryRun("/sanitize_mp3"));
    }

    @Test
    @DisplayName("Should build MP3 sanitize summary")
    void testBuildSanitizeMp3Summary() {
        FileNameSanitizer.SanitizeDirectoryResult result = new FileNameSanitizer.SanitizeDirectoryResult(
                "/tmp/music",
                false,
                false,
                false,
                3,
                2,
                2,
                1,
                0,
                java.util.List.of("Renamed: Dirty Name.mp3 -> Clean Name.mp3")
        );

        String summary = CommandHandler.buildSanitizeMp3Summary(result);

        assertTrue(summary.contains("MP3 sanitize complete"));
        assertTrue(summary.contains("Files renamed: 2 out of 2"));
        assertTrue(summary.contains("Already clean: 1"));
        assertTrue(summary.contains("Dirty Name.mp3 -> Clean Name.mp3"));
    }

    @Test
    @DisplayName("Should parse cut command range")
    void testParseCutCommandRange() {
        AudioClipRange range = CommandHandler.parseCutCommandRange("/cut 0:00 2:50");

        assertNotNull(range);
        assertEquals(0.0, range.startSeconds());
        assertEquals(170.0, range.endSeconds());
        assertEquals("0:00 - 2:50", range.formatLabel());
        assertTrue(CommandHandler.isCutCommand("/cut@YoutubeMp3Bot 0:00 2:50"));
    }

    @Test
    @DisplayName("Should reject invalid cut command range")
    void testRejectsInvalidCutCommandRange() {
        assertNull(CommandHandler.parseCutCommandRange("/cut 2:50 0:00"));
        assertNull(CommandHandler.parseCutCommandRange("/cut nope 2:50"));
        assertNull(CommandHandler.parseCutCommandRange("/start 0:00 2:50"));
        assertFalse(CommandHandler.isCutCommand("/sanitize_mp3"));
    }

    @Test
    @DisplayName("Should detect audio document attachments for cut command")
    void testDetectsAudioDocumentAttachment() {
        org.telegram.telegrambots.meta.api.objects.Document document =
                mock(org.telegram.telegrambots.meta.api.objects.Document.class);
        when(document.getFileId()).thenReturn("file-id-123");
        when(document.getFileName()).thenReturn("Track Name.mp3");
        when(document.getMimeType()).thenReturn("audio/mpeg");
        when(message.hasDocument()).thenReturn(true);
        when(message.getDocument()).thenReturn(document);

        CommandHandler.TelegramAudioAttachment attachment = CommandHandler.extractAudioAttachment(message);

        assertNotNull(attachment);
        assertEquals("file-id-123", attachment.fileId());
        assertEquals("Track Name.mp3", attachment.fileName());
        assertTrue(CommandHandler.isAudioDocument("Track Name.mp3", null));
        assertFalse(CommandHandler.isAudioDocument("notes.txt", "text/plain"));
    }

    @Test
    @DisplayName("Should sanitize uploaded cut base name")
    void testCutBaseNameSanitizesFileName() {
        assertEquals("The_Ghost_Aura_Nihilism", CommandHandler.cutBaseName("The Ghost Aura - Nihilism (Official Video).mp3"));
        assertEquals("Audio", CommandHandler.cutBaseName(null));
    }

    @Test
    @DisplayName("Should keep clean output file name for uploaded cut")
    void testCutOutputFileNameKeepsCleanName() {
        assertEquals("Dollwave Beyond.mp3", CommandHandler.cutOutputFileName("Dollwave_Beyond.mp3"));
        assertEquals("Dollwave Beyond.mp3", CommandHandler.cutOutputFileName("Dollwave Beyond.mp3"));
        assertEquals("Audio.mp3", CommandHandler.cutOutputFileName(null));
    }
}
