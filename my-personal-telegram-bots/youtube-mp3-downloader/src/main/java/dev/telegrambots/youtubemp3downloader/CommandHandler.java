package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final BotConfig config = new BotConfig();
    private static final YtDlpService ytDlpService = new YtDlpService(
            config.ytDlpPath, config.ffmpegPath, config.ffprobePath, config.maxFileSize, config.maxDurationMinutes);

    /**
     * Returns the current date and time as a formatted string (yyyy-MM-dd HH:mm:ss).
     */
    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Handles incoming Telegram updates. Detects YouTube links in the message, processes single or multiple links,
     * sends progress updates, and triggers audio download and conversion.
     *
     * @param bot    The bot instance
     * @param update The incoming update from Telegram
     * @return true if the update was handled, false otherwise
     */    public static boolean handle(Bot bot, Update update) {
        TelegramService telegram = new TelegramService(bot);
        if (update.hasMessage() && update.getMessage() != null && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            
            // Check for null text
            if (text == null) {
                return false;
            }
            java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("https?://(?:www\\.)?(?:youtube\\.com/(?:watch\\?v=|shorts/)|youtu\\.be/)[\\w-]{11}");
            java.util.regex.Matcher matcher = urlPattern.matcher(text);
            java.util.Set<String> urlSet = new java.util.LinkedHashSet<>();
            while (matcher.find()) {
                urlSet.add(matcher.group());
            }
            java.util.List<String> urls = new java.util.ArrayList<>(urlSet);
            if (urls.size() > 1) {
                long batchStart = System.currentTimeMillis();
                int approxSec = (int)Math.ceil(urls.size() * 60.0 / config.maxParallelDownloads); // 1 minute per link, parallel processing
                telegram.sendText(message.getChatId(), "🤯 Detected " + urls.size() + " YouTube links! Up to " + config.maxParallelDownloads + " will be processed in parallel. Files will be sent as soon as each is ready.\nApproximate export time: " + approxSec + " seconds (" + (approxSec/60) + " min)");
                new Thread(() -> {
                    int total = urls.size();
                    int[] done = {0};
                    int[] error = {0};
                    java.util.List<String> errorDetails = new java.util.ArrayList<>();
                    java.util.concurrent.ExecutorService batchExec = java.util.concurrent.Executors.newFixedThreadPool(config.maxParallelDownloads);
                    java.util.List<java.util.concurrent.Callable<Void>> tasks = new java.util.ArrayList<>();
                    for (int i = 0; i < urls.size(); i++) {
                        final int idx = i;
                        final String url = urls.get(i);
                        tasks.add(() -> {
                            try {
                                boolean result = processDownloadWithStatus(telegram, message, url, idx + 1, total);
                                if (result) {
                                    synchronized (done) { done[0]++; }
                                } else {
                                    synchronized (error) { error[0]++; }
                                    synchronized (errorDetails) { errorDetails.add(url); }
                                }
                            } catch (Exception ex) {
                                synchronized (error) { error[0]++; }
                                synchronized (errorDetails) { errorDetails.add(url + " (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")"); }
                                logger.error("[{}] Error processing URL: {}\n{}", now(), url, ex.getMessage(), ex);
                            }
                            return null;
                        });
                    }
                    try {
                        for (int i = 0; i < tasks.size(); i += config.maxParallelDownloads) {
                            int end = Math.min(i + config.maxParallelDownloads, tasks.size());
                            batchExec.invokeAll(tasks.subList(i, end));
                        }
                    } catch (InterruptedException e) {
                        logger.error("[{}] Batch interrupted", now(), e);
                    }
                    batchExec.shutdown();
                    long batchEnd = System.currentTimeMillis();
                    long elapsedSec = (batchEnd - batchStart) / 1000;
                    StringBuilder summary = new StringBuilder();
                    summary.append("\uD83C\uDF89 [SUMMARY] Batch complete!\n");
                    summary.append("[SUCCESS ✅] Processed: ").append(done[0]).append("\n");
                    summary.append("[ERROR ☢️☣️] Failed: ").append(error[0]).append("\n");
                    summary.append("⏱️ Export time: ").append(elapsedSec).append(" seconds (" + (elapsedSec/60) + " min)\n");
                    if (!errorDetails.isEmpty()) {
                        summary.append("\nFailed URLs:\n");
                        for (String err : errorDetails) summary.append(err).append("\n");
                    }
                    telegram.sendText(message.getChatId(), summary.toString());
                }).start();
                return true;
            } else if (urls.size() == 1 && Utils.isValidYouTubeUrl(text)) {
                telegram.sendText(message.getChatId(), "[SUCCESS ✅] Link accepted! 🎬 Starting processing...");
                telegram.sendChatAction(message.getChatId(), ActionType.UPLOADDOCUMENT);
                executor.submit(() -> processDownloadWithStatus(telegram, message, text, 1, 1));
                return true;
            } else {
                telegram.sendText(message.getChatId(), "[ERROR ☢️☣️] Please send a valid YouTube video link. 🚫");
                return false;
            }
        }
        return false;
    }

    /**
     * Extracts the original YouTube title and author from the HTML page using curl.
     * Returns a String array: [ytTitleRaw, ytAuthorRaw].
     */
    private static String[] extractTitleAuthorFromCurl(String url) {
        try {
            Process curl = new ProcessBuilder("curl", "-L", url).redirectErrorStream(true).start();
            StringBuilder html = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(curl.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) html.append(line);
            }
            curl.waitFor();
            String htmlStr = html.toString();
            String ytTitle = null;
            String ytAuthor = null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<title>(.*?)</title>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlStr);
            if (m.find()) {
                ytTitle = m.group(1).replaceAll(" - YouTube$", "");
            }
            m = java.util.regex.Pattern.compile("\"author\":\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlStr);
            if (m.find()) {
                ytAuthor = m.group(1);
            }
            if (ytTitle == null || ytTitle.isBlank()) {
                m = java.util.regex.Pattern.compile("<meta name=\"title\" content=\"(.*?)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlStr);
                if (m.find()) ytTitle = m.group(1);
            }
            return new String[]{ytTitle, ytAuthor};
        } catch (Exception ex) {
            logger.error("[{}] [yt-dlp-info] Fallback curl failed", now(), ex);
            return new String[]{null, null};
        }
    }

    /**
     * Downloads and processes a YouTube video by URL, saves the resulting mp3 file, and sends it to the user.
     * Handles fallback for title/author extraction, file size and duration checks, and error reporting.
     *
     * @param telegram TelegramService for sending messages/files
     * @param message  The original Telegram message
     * @param url      The YouTube video URL
     * @param index    The index of the current URL in the batch
     * @param total    The total number of URLs in the batch
     * @return true if the download was successful, false otherwise
     */
    private static boolean processDownloadWithStatus(TelegramService telegram, Message message, String url, int index, int total) {
        String chatId = message.getChatId().toString();
        final boolean[] sending = {true};
        Thread progressThread = new Thread(() -> {
            while (sending[0]) {
                telegram.sendChatAction(message.getChatId(), ActionType.UPLOADDOCUMENT);
                try { Thread.sleep(1000); } catch (InterruptedException e) {
                    logger.warn("[{}] InterruptedException occurred: {}", now(), e.getMessage(), e);
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        });
        logger.debug("[{}] Starting progress thread for chatId: {}", now(), chatId);
        progressThread.start();
        try {
            // Pre-check video size and duration to avoid wasting time on large videos
            logger.info("[{}] Pre-checking video size and duration for URL: {}", now(), url);
            long[] sizeAndDuration = ytDlpService.getVideoSizeAndDuration(url);
            long filesize = sizeAndDuration[0];
            long duration = sizeAndDuration[1];
            
            // Check duration limit
            if (duration > 0 && duration > config.maxDurationMinutes * 60) {
                logger.warn("[{}] Audio duration exceeds limit: {} seconds for URL: {} | Expected limit: {} seconds", now(), duration, url, config.maxDurationMinutes * 60);
                String errMsg = "[ERROR ☢️☣️] Audio is too long (over " + config.maxDurationMinutes + " minutes). Try another video. (" + index + "/" + total + ")\nURL: " + url + " ⏳";
                telegram.sendText(Long.valueOf(chatId), errMsg);
                return false;
            }
            
            // Check file size limit
            if (filesize > 0 && filesize > config.maxFileSize) {
                logger.warn("[{}] Audio file size exceeds limit: {} bytes for URL: {} | Expected limit: {} bytes", now(), filesize, url, config.maxFileSize);
                String errMsg = "[ERROR ☢️☣️] Audio file is too large (over " + (config.maxFileSize / 1024 / 1024) + " MB). Try another video. (" + index + "/" + total + ")\nURL: " + url + " 💾";
                telegram.sendText(Long.valueOf(chatId), errMsg);
                return false;
            }
            
            logger.info("[{}] Pre-check passed for URL: {} | Size: {} bytes | Duration: {} seconds", now(), url, filesize, duration);
            
            // Update yt-dlp parameters to extract audio only (for getting metadata)
            ProcessBuilder pb = new ProcessBuilder(
                config.ytDlpPath,
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "320k",
                "--dump-json",
                url
            );
            pb.redirectErrorStream(true);
            logger.debug("[{}] yt-dlp command: {}", now(), String.join(" ", pb.command()));
            Process proc = pb.start();
            StringBuilder jsonBuilder = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }
            proc.waitFor();
            String json = jsonBuilder.toString();
            logger.debug("[{}] yt-dlp output: {}", now(), json);

            if (!json.isBlank()) {
                if (json.trim().startsWith("{")) {
                    // JSON response received, continue processing
                    logger.info("[{}] Valid JSON metadata received for URL: {}", now(), url);
                } else {
                    logger.warn("[{}] Non-JSON response received. Attempting fallback processing.", now());
                    telegram.sendText(Long.valueOf(chatId), "[WARNING ⚠️] Metadata could not be parsed, but we will attempt to process the audio.");
                    telegram.sendChatAction(message.getChatId(), ActionType.UPLOADDOCUMENT); // Продолжаем крутить лоудер
                    // Continue processing without metadata
                }
            } else {
                logger.warn("[{}] Empty JSON response for URL: {} | yt-dlp output: {}", now(), url, json);
                telegram.sendText(Long.valueOf(chatId), "[ERROR ☢️☣️] No metadata received. Try another video.");
                return false;
            }
            logger.debug("[{}] Metadata check completed for URL: {}", now(), url);
        } catch (Exception e) {
            logger.error("[{}] Exception during metadata check for URL: {}", now(), url, e);
        } finally {
            sending[0] = false; // Завершаем поток статуса загрузки
            logger.debug("[{}] Stopping progress thread for chatId: {}", now(), chatId);
            try {
                progressThread.join(); // Дожидаемся завершения потока
                logger.debug("[{}] Progress thread joined successfully for chatId: {}", now(), chatId);
            } catch (InterruptedException e) {
                logger.warn("[{}] Progress thread interrupted for chatId: {}", now(), chatId, e);
                Thread.currentThread().interrupt();
            }
            // Отправляем финальный статус
            logger.debug("[{}] Sending final chat action for chatId: {}", now(), chatId);
        }

        try {
            // 1. Get info via yt-dlp
            String[] ytDlpInfo = ytDlpService.getVideoInfo(url);
            logger.info("[{}] [yt-dlp-info] RAW channel: {} | RAW title: {}", now(), ytDlpInfo[0], ytDlpInfo[1]);
            String sanitizedChannel = FileNameSanitizer.sanitize(ytDlpInfo[0]);
            String sanitizedTitle = FileNameSanitizer.sanitize(ytDlpInfo[1]);
            logger.info("[{}] [yt-dlp-info] SANITIZED channel: {} | SANITIZED title: {}", now(), sanitizedChannel, sanitizedTitle);

            // 2. Check if fallback via curl is needed
            boolean fallbackUsed = false;
            String ytTitleRaw = null;
            String ytAuthorRaw = null;
            if (sanitizedChannel == null || sanitizedChannel.isBlank() || sanitizedChannel.matches(".*[=\\/\\:*?\"<>|].*") || sanitizedChannel.contains("http")
                || sanitizedTitle == null || sanitizedTitle.isBlank() || sanitizedTitle.matches(".*[=\\/\\:*?\"<>|].*") || sanitizedTitle.contains("http")) {
                String[] curlInfo = extractTitleAuthorFromCurl(url);
                ytTitleRaw = curlInfo[0];
                ytAuthorRaw = curlInfo[1];
                if (ytTitleRaw != null && !ytTitleRaw.isBlank()) {
                    sanitizedTitle = FileNameSanitizer.sanitize(ytTitleRaw);
                    if (ytAuthorRaw != null && !ytAuthorRaw.isBlank()) {
                        sanitizedChannel = FileNameSanitizer.sanitize(ytAuthorRaw);
                    } else {
                        sanitizedChannel = null;
                    }
                    fallbackUsed = true;
                    logger.info("[{}] [yt-dlp-info] Fallback title/author from curl: {} / {}", now(), ytTitleRaw, ytAuthorRaw);
                } else {
                    sanitizedTitle = "video";
                    sanitizedChannel = null;
                }
            }

            // 3. Build final file name
            String baseFileName = (sanitizedChannel != null && !sanitizedChannel.isBlank())
                ? FileNameSanitizer.composeFileName(sanitizedChannel, sanitizedTitle)
                : sanitizedTitle;
            if (baseFileName == null || baseFileName.isBlank()) baseFileName = "audio";
            java.io.File saveDir = new java.io.File("termuxserver/youtube_mp3_downloader_workzone");
            if (!saveDir.exists()) saveDir.mkdirs();
            String finalFile = baseFileName + ".mp3";
            java.io.File finalAudioFile = new java.io.File(saveDir, finalFile);
            ytDlpService.deleteFileIfExists(finalAudioFile);

            // 4. Download audio
            boolean success = ytDlpService.downloadAudioWithThumbnail(url, finalAudioFile.getAbsolutePath());
            if (!success && !finalAudioFile.exists()) {
                telegram.sendText(message.getChatId(), "[ERROR ☢️☣️] Error downloading or converting audio. Check the link or try another video. (" + index + "/" + total + ")\nURL: " + url + " ❌");
                return false;
            }

            // 5. Check limits
            telegram.sendChatAction(message.getChatId(), ActionType.TYPING);
            double durationAfterDownload = ytDlpService.getAudioDurationSeconds(finalAudioFile.getAbsolutePath());
            if (!ytDlpService.isDurationWithinLimit(durationAfterDownload)) {
                logger.warn("[{}] [DurationLimit] Video too long: {} seconds | URL: {} | Expected limit: {} seconds", now(), durationAfterDownload, url, 30 * 60);
                String errMsg = "[ERROR ☢️☣️] Video is too long (over 30 minutes). Try another video. (" + index + "/" + total + ")\nURL: " + url + " ⏳";
                telegram.sendText(message.getChatId(), errMsg);
                ytDlpService.deleteFileIfExists(finalAudioFile);
                return false;
            }
            if (!ytDlpService.isFileSizeWithinLimit(finalAudioFile)) {
                long fileSize = finalAudioFile.exists() ? finalAudioFile.length() : -1;
                
                // Check if file is empty (download failed)
                if (fileSize == 0) {
                    logger.error("[{}] [DownloadFailed] Downloaded file is empty: {} | URL: {} | Possible causes: video unavailable, age-restricted, or blocked", 
                               now(), finalAudioFile.getAbsolutePath(), url);
                    String errMsg = "[ERROR ☢️☣️] Failed to download video. Video may be unavailable, age-restricted, or blocked by YouTube. (" + index + "/" + total + ")\nURL: " + url + " 🚫\n\n🔍 Debug: Empty file (0 bytes) - usually means YouTube blocked access or video is restricted.";
                    telegram.sendText(message.getChatId(), errMsg);
                } else if (fileSize > 0) {
                    logger.warn("[{}] [FileSizeLimit] File too large: {} bytes | URL: {} | Expected limit: {} bytes", 
                               now(), fileSize, url, 50 * 1024 * 1024);
                    String errMsg = "[ERROR ☢️☣️] Audio file exceeds 50 MB (" + String.format("%.2f MB", fileSize / 1024.0 / 1024.0) + "). Try another video. (" + index + "/" + total + ")\nURL: " + url + " 💾";
                    telegram.sendText(message.getChatId(), errMsg);
                } else {
                    logger.error("[{}] [FileNotFound] Downloaded file does not exist: {} | URL: {}", 
                                now(), finalAudioFile.getAbsolutePath(), url);
                    String errMsg = "[ERROR ☢️☣️] Download failed. File not found. (" + index + "/" + total + ")\nURL: " + url + " ❓";
                    telegram.sendText(message.getChatId(), errMsg);
                }
                
                ytDlpService.deleteFileIfExists(finalAudioFile);
                return false;
            }

            // 6. Build message and send audio
            String beforeName;
            if (fallbackUsed && ytTitleRaw != null && !ytTitleRaw.isBlank()) {
                beforeName = ytTitleRaw + ".mp3";
            } else {
                beforeName = (ytDlpInfo[1] != null ? ytDlpInfo[1] : "(unknown)") + ".mp3";
            }
            String afterName = baseFileName + ".mp3";
            StringBuilder msg = new StringBuilder();
            msg.append("[SUCCESS ✅] Audio ready! 🎶 (").append(index).append("/").append(total).append(")\n");
            msg.append("\uD83C\uDFB5 Song renamed\n");
            msg.append("\uD83D\uDD22 Before: ").append(beforeName).append("\n");
            msg.append("\uD83D\uDD01 After:  ").append(afterName);
            msg.append("\n🔗 YouTube: ").append(url); // Добавляем ссылку на YouTube
            if (fallbackUsed) {
                msg.append("\n\nTitle taken from <title> tag of YouTube page (curl fallback)");
            }
            telegram.sendAudio(chatId, finalAudioFile, msg.toString());
            logger.info("[{}] [SendAudio] Sent audio for URL: {}", now(), url);
            return true;
        } catch (IOException e) {
            logger.error("[{}] IOException occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] File or disk access error: (" + index + "/" + total + ")\nURL: " + url + " 💾";
            telegram.sendText(message.getChatId(), errMsg);
        } catch (InterruptedException e) {
            logger.error("[{}] InterruptedException occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] Operation was interrupted: (" + index + "/" + total + ")\nURL: " + url + " ⏹️";
            telegram.sendText(message.getChatId(), errMsg);
        } catch (Exception e) {
            logger.error("[{}] General exception occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] An unexpected error occurred: (" + index + "/" + total + ")\nURL: " + url + " ❌";
            telegram.sendText(message.getChatId(), errMsg);
        } finally {
            sending[0] = false; // Завершаем поток статуса загрузки
            try {
                progressThread.join(); // Дожидаемся завершения потока
            } catch (InterruptedException e) {
                logger.warn("[{}] Progress thread interrupted: {}", now(), e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    // Note: Bot authorization on YouTube is not possible using standard yt-dlp/ffmpeg methods. yt-dlp supports cookies (--cookies), but for a Telegram bot, this is highly inconvenient and not recommended. YouTube actively fights automation and authorization through third-party services.
}
