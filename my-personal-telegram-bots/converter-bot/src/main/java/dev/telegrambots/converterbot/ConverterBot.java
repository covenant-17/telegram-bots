package dev.telegrambots.converterbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

// Third-party libraries
import org.json.JSONArray;
import org.json.JSONObject;

// Telegram Bots API - Core functionality
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

// Telegram API Objects
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

// Logging framework
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Telegram Bot for converting WebM/GIF files to MP4 format.
 * This implementation provides file conversion functionality with enhanced error handling.
 *
 * @author Your Name
 * @version 1.0
 * @since 2025-08-29
 */
public class ConverterBot extends TelegramLongPollingBot {

  private static final Logger logger = LoggerFactory.getLogger(ConverterBot.class);
  private final BotConfig config;

  @SuppressWarnings("deprecation") // Suppress deprecation warning for constructor
  // TODO: Update to new constructor when upgrading to newer Telegram Bots API version
  public ConverterBot() {
    super();
    this.config = new BotConfig();
  }

  @Override
  public String getBotUsername() {
    return config.botUsername;
  }

  @Override
  public String getBotToken() {
    return config.botToken;
  }

  @Override
  public void onUpdateReceived(Update update) {
    // Custom update processing with enhanced error handling for ConverterBot
    try {
      if (update.hasMessage()) {
        Message message = update.getMessage();
        if (message.hasText() && "/start".equals(message.getText())) {
          handleStartCommand(message);
          return;
        }
        if (message.hasDocument()) {
          handleDocument(message);
          return;
        }
      }
    } catch (Exception e) {
      logger.error("Critical error in ConverterBot update processing: {}", e.getMessage(), e);
      // Send error message to user if possible
      if (update.hasMessage()) {
        sendErrorMessage(update.getMessage().getChatId(), "An error occurred while processing your request. Please try again.");
      }
    }
  }

  private void handleStartCommand(Message message) {
    try {
      String welcome = "[SUCCESS ✅] " + getRandomText(
        "welcome",
        "bot_texts_welcome.json"
      ) + " 👋";
      SendMessage sendMessage = new SendMessage();
      sendMessage.setChatId(message.getChatId().toString());
      sendMessage.setText(welcome);
      execute(sendMessage);
    } catch (Exception e) {
      logger.error("Error sending welcome message: {}", e.getMessage(), e);
    }
  }

  private void handleDocument(Message message) {
    Document document = message.getDocument();
    String fileName = document.getFileName();
    
    if (fileName == null || (!fileName.endsWith(".webm") && !fileName.endsWith(".gif"))) {
      sendErrorMessage(message.getChatId(), "Please send a .webm or .gif file to convert.");
      return;
    }

    try {
      System.out.println("[bot] File received: " + fileName);
      
      // Show typing indicator
      SendChatAction loader = new SendChatAction();
      loader.setChatId(message.getChatId().toString());
      loader.setAction(ActionType.UPLOADDOCUMENT);
      execute(loader);
      
      // Download file
      File file = execute(new GetFile(document.getFileId()));
      java.io.File inputFile = downloadFile(file.getFilePath());
      System.out.println("[bot] Start conversion: " + inputFile.getAbsolutePath());
      
      // Convert to mp4
      java.io.File mp4File = convertToMp4(inputFile, fileName);
      logger.info("Conversion finished: {}", mp4File.getAbsolutePath());
      
      // Send result back
      SendDocument sendDocument = new SendDocument();
      sendDocument.setChatId(message.getChatId().toString());
      sendDocument.setDocument(new InputFile(mp4File));
      
      // Success message
      String doneMsg = "[SUCCESS ✅] " + getRandomText(
        "done",
        "bot_texts_done.json"
      ) + " 🎬";
      sendDocument.setCaption(doneMsg);
      execute(sendDocument);
      
      logger.info("MP4 sent to user: {}", message.getChatId());
      
      // Clean up temporary files
      if (!inputFile.delete()) {
        logger.warn("Failed to delete temporary input file: {}", inputFile.getAbsolutePath());
      }
      if (!mp4File.delete()) {
        logger.warn("Failed to delete temporary output file: {}", mp4File.getAbsolutePath());
      }
      
      // Stop typing indicator
      SendChatAction done = new SendChatAction();
      done.setChatId(message.getChatId().toString());
      done.setAction(ActionType.TYPING);
      execute(done);
      
    } catch (Exception e) {
      logger.error("Error during file conversion for user {}: {}", message.getChatId(), e.getMessage(), e);
      sendErrorMessage(message.getChatId(), "[ERROR ☢️☣️] An error occurred during file conversion. Please try again. ❌");
    }
  }

  // Convert webm or gif to mp4
  private java.io.File convertToMp4(java.io.File inputFile, String fileName)
    throws IOException, InterruptedException {
    String mp4Path;
    String name = inputFile.getAbsolutePath();
    // Remove .tmp extension if present
    if (name.endsWith(".tmp")) {
      name = name.substring(0, name.length() - 4);
    }
    if (fileName.endsWith(".webm")) {
      mp4Path = name.substring(0, name.length() - 5) + ".mp4";
    } else if (fileName.endsWith(".gif")) {
      mp4Path = name.substring(0, name.length() - 4) + ".mp4";
    } else {
      mp4Path = name + ".mp4";
    }
    ProcessBuilder pb = new ProcessBuilder(
      config.ffmpegPath,
      "-y",
      "-i",
      inputFile.getAbsolutePath(),
      "-r",
      "30",
      mp4Path
    );
    pb.redirectErrorStream(true);
    Process process = pb.start();
    try (
      java.io.BufferedReader reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(process.getInputStream())
      )
    ) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("[ffmpeg] " + line);
      }
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) throw new IOException("ffmpeg conversion failed");
    return new java.io.File(mp4Path);
  }

  private String getRandomText(String key, String filePath) {
    try {
      InputStream is = getClass()
        .getClassLoader()
        .getResourceAsStream(filePath.replace("src/main/resources/", ""));
      if (is == null) throw new IOException("Resource not found: " + filePath);
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      JSONObject obj = new JSONObject(json);
      JSONArray arr = obj.getJSONArray(key);
      int idx = new Random().nextInt(arr.length());
      return arr.getString(idx);
    } catch (Exception e) {
      if ("welcome".equals(key)) {
        return "Welcome! Send me a .webm and I'll turn it into an .mp4 for you.";
      } else if ("done".equals(key)) {
        return "Done! Your mp4 is ready.";
      } else if ("wrongtype".equals(key)) {
        return "Unsupported file type. Please send a .webm or .gif file.";
      } else {
        return "Message not found.";
      }
    }
  }

  private void sendErrorMessage(Long chatId, String message) {
    try {
      SendMessage errorMessage = new SendMessage();
      errorMessage.setChatId(chatId.toString());
      errorMessage.setText(message);
      execute(errorMessage);
    } catch (Exception e) {
      logger.error("Error sending error message: {}", e.getMessage(), e);
    }
  }
}
