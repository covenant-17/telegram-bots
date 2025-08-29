package dev.telegrambots.converterbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ConverterBot extends TelegramLongPollingBot {

  @SuppressWarnings("deprecation") // подавляем предупреждение о deprecated конструкторе
  public ConverterBot() {
    super();
  }

  @Override
  public String getBotUsername() {
    return "webm_to_mp4_converter_r_bot"; // Bot name, do not change
  }

  @Override
  public String getBotToken() {
    return "7531354169:AAFzf547M4Y4MzavRZkI_vPJo7pde5QxbAc"; // Bot token, keep it secret
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      Message message = update.getMessage();
      if (message.hasText() && "/start".equals(message.getText())) {
        // Welcome message
        String welcome = "[SUCCESS ✅] " + getRandomText(
          "welcome",
          "src/main/resources/bot_texts_welcome.json"
        ) + " 👋";
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(welcome);
        try {
          execute(sendMessage);
        } catch (Exception ignored) {}
        return;
      }
      if (message.hasDocument()) {
        Document document = message.getDocument();
        String fileName = document.getFileName();
        if (
          fileName != null &&
          (fileName.endsWith(".webm") || fileName.endsWith(".gif"))
        ) {
          try {
            System.out.println("[bot] File received: " + fileName);
            // Loader on
            SendChatAction loader = new SendChatAction();
            loader.setChatId(message.getChatId().toString());
            loader.setAction(ActionType.UPLOADDOCUMENT);
            execute(loader);
            // Downloading file
            File file = execute(new GetFile(document.getFileId()));
            java.io.File inputFile = downloadFile(file.getFilePath());
            System.out.println(
              "[bot] Start conversion: " + inputFile.getAbsolutePath()
            );
            // Convert to mp4
            java.io.File mp4File = convertToMp4(inputFile, fileName);
            System.out.println(
              "[bot] Conversion finished: " + mp4File.getAbsolutePath()
            );
            // Send back
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(message.getChatId().toString());
            sendDocument.setDocument(new InputFile(mp4File));
            // Send done message
            String doneMsg = "[SUCCESS ✅] " + getRandomText(
              "done",
              "src/main/resources/bot_texts_done.json"
            ) + " 🎬";
            sendDocument.setCaption(doneMsg); // подпись к файлу
            execute(sendDocument);
            System.out.println("[bot] MP4 sent to user.");
            // Cleanup
            inputFile.delete();
            mp4File.delete();
            // Loader off
            SendChatAction done = new SendChatAction();
            done.setChatId(message.getChatId().toString());
            done.setAction(ActionType.TYPING);
            execute(done);
          } catch (Exception e) {
            System.out.println("[bot] Error during conversion.");
            e.printStackTrace();
            SendMessage errorMsg = new SendMessage();
            errorMsg.setChatId(message.getChatId().toString());
            errorMsg.setText("[ERROR ☢️☣️] Произошла ошибка при конвертации файла. Попробуйте еще раз. ❌");
            try {
              execute(errorMsg);
            } catch (Exception ignored) {}
          }
        } else {
          // Unsupported file type
          String wrongTypeMsg = "[ERROR ☢️☣️] " + getRandomText(
            "wrongtype",
            "src/main/resources/bot_texts_wrongtype.json"
          ) + " 🚫";
          SendMessage wrongTypeMessage = new SendMessage();
          wrongTypeMessage.setChatId(message.getChatId().toString());
          wrongTypeMessage.setText(wrongTypeMsg);
          try {
            execute(wrongTypeMessage);
          } catch (Exception ignored) {}
        }
      }
    }
  }

  // Convert webm or gif to mp4
  private java.io.File convertToMp4(java.io.File inputFile, String fileName)
    throws IOException, InterruptedException {
    String mp4Path;
    String name = inputFile.getAbsolutePath();
    // Remove .tmp if present
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
      "ffmpeg",
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
}
