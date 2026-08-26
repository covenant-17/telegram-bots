package dev.telegrambots.converterbot;

import dev.telegrambots.shared.BaseBotConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Third-party libraries
import org.json.JSONArray;
import org.json.JSONObject;

// Telegram Bots API - Core functionality
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;

// Telegram API Objects
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.games.Animation;

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
  private final ExecutorService executor = Executors.newFixedThreadPool(3);

  static String getBotTokenFromConfig() {
    try {
      return BaseBotConfig.loadConfig().getString("bot.token");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load bot token from config", e);
    }
  }

  // Updated constructor for newer Telegram Bots API version (6.9.7.1)
  // Token is passed to superclass constructor, no deprecated method overrides needed
  public ConverterBot() {
    super(getBotTokenFromConfig());
    this.config = new BotConfig();
  }

  @Override
  public String getBotUsername() {
    return config.botUsername;
  }

  @Override
  public void onUpdateReceived(Update update) {
    // Custom update processing with enhanced error handling for ConverterBot
    try {
      if (update.hasMessage()) {
        Message message = update.getMessage();
        logger.info(
          "Received message from chat {}: document={}, video={}, animation={}, text={}",
          message.getChatId(),
          message.hasDocument(),
          message.hasVideo(),
          message.hasAnimation(),
          message.hasText()
        );
        if (message.hasText() && "/start".equals(message.getText())) {
          handleStartCommand(message);
          return;
        }
        if (message.hasDocument()) {
          Document document = message.getDocument();
          handleFile(
            message,
            document.getFileId(),
            supportedFileName(document.getFileName(), document.getMimeType(), null)
          );
          return;
        }
        if (message.hasVideo()) {
          Video video = message.getVideo();
          handleFile(
            message,
            video.getFileId(),
            supportedFileName(video.getFileName(), video.getMimeType(), null)
          );
          return;
        }
        if (message.hasAnimation()) {
          Animation animation = message.getAnimation();
          handleFile(
            message,
            animation.getFileId(),
            supportedFileName(animation.getFileName(), null, ".gif")
          );
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

  private void handleFile(Message message, String fileId, String fileName) {
    if (fileName == null) {
      logger.info("Unsupported media received from chat {}", message.getChatId());
      sendErrorMessage(message.getChatId(), "Please send a .webm or .gif file to convert.");
      return;
    }

    // Acknowledge receipt immediately so user knows the bot is working
    try {
      SendMessage ack = new SendMessage();
      ack.setChatId(message.getChatId().toString());
      ack.setText("⏳ File received! Converting, please wait...");
      execute(ack);
    } catch (Exception e) {
      logger.warn("Failed to send acknowledgement: {}", e.getMessage());
    }

    executor.submit(() -> {
      try {
        System.out.println("[bot] File received: " + fileName);
        
        // Show typing indicator (non-critical, don't abort conversion if it fails)
        try {
          SendChatAction loader = new SendChatAction();
          loader.setChatId(message.getChatId().toString());
          loader.setAction(ActionType.UPLOADDOCUMENT);
          execute(loader);
        } catch (Exception chatActionEx) {
          logger.warn("Failed to send chat action (non-critical): {}", chatActionEx.getMessage());
        }

        // Download file
        File file = execute(new GetFile(fileId));
        java.io.File inputFile = downloadFile(file.getFilePath());
        System.out.println("[bot] Start conversion: " + inputFile.getAbsolutePath());
        
        // Convert to mp4
        java.io.File mp4File = convertToMp4(inputFile, fileName);
        logger.info("Conversion finished: {}", mp4File.getAbsolutePath());
        
        // Send result back
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(message.getChatId().toString());
        sendVideo.setVideo(new InputFile(mp4File));
        sendVideo.setSupportsStreaming(true);
        
        // Success message
        String doneMsg = "[SUCCESS ✅] " + getRandomText(
          "done",
          "bot_texts_done.json"
        ) + " 🎬";
        sendVideo.setCaption(doneMsg);
        execute(sendVideo);
        
        logger.info("MP4 sent to user: {}", message.getChatId());
        
        // Clean up temporary files
        if (!inputFile.delete()) {
          logger.warn("Failed to delete temporary input file: {}", inputFile.getAbsolutePath());
        }
        if (!mp4File.delete()) {
          logger.warn("Failed to delete temporary output file: {}", mp4File.getAbsolutePath());
        }
        
      } catch (Exception e) {
        logger.error("Error during file conversion for user {}: {}", message.getChatId(), e.getMessage(), e);
        sendErrorMessage(message.getChatId(), "[ERROR ☢️☣️] An error occurred during file conversion. Please try again. ❌");
      }
    });
  }

  static String supportedFileName(String fileName, String mimeType, String fallbackExtension) {
    if (fileName != null) {
      String lowerCaseName = fileName.toLowerCase(java.util.Locale.ROOT);
      if (lowerCaseName.endsWith(".webm") || lowerCaseName.endsWith(".gif")) {
        return fileName;
      }
    }
    if (mimeType != null) {
      if ("video/webm".equalsIgnoreCase(mimeType)) {
        return "telegram-upload.webm";
      }
      if ("image/gif".equalsIgnoreCase(mimeType)) {
        return "telegram-upload.gif";
      }
    }
    return fallbackExtension == null ? null : "telegram-upload" + fallbackExtension;
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
      "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
      "-r", "30",
      mp4Path
    );
    pb.redirectErrorStream(false);
    Process process = pb.start();
    StringBuilder ffmpegOutput = new StringBuilder();
    try (
      java.io.BufferedReader stdout = new java.io.BufferedReader(
        new java.io.InputStreamReader(process.getInputStream())
      );
      java.io.BufferedReader stderr = new java.io.BufferedReader(
        new java.io.InputStreamReader(process.getErrorStream())
      )
    ) {
      String line;
      while ((line = stdout.readLine()) != null) {
        System.out.println("[ffmpeg] " + line);
      }
      while ((line = stderr.readLine()) != null) {
        System.out.println("[ffmpeg] " + line);
        ffmpegOutput.append(line).append("\n");
      }
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      logger.error("ffmpeg failed (exit {}). Last output:\n{}", exitCode, ffmpegOutput);
      throw new IOException("ffmpeg conversion failed (exit code " + exitCode + ")");
    }
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
