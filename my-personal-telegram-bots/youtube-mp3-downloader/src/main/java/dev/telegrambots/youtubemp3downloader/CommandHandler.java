package dev.telegrambots.youtubemp3downloader;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final BotConfig config = new BotConfig();
    private static final YtDlpService ytDlpService = new YtDlpService(
            config.ytDlpPath, config.ffmpegPath, config.ffprobePath, config.maxFileSize, config.maxDurationMinutes,
            config.cookiesFilePath);
    private static final MusicDuplicateIndex duplicateIndex = new MusicDuplicateIndex(config.duplicateIndexPath);
    private static final ConcurrentHashMap<String, PendingDownload> pendingDuplicateDownloads = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PendingChapterDownload> pendingChapterDownloads = new ConcurrentHashMap<>();
    private static final String FORCE_DOWNLOAD_CALLBACK_PREFIX = "dupdl:";
    private static final String CHAPTER_DOWNLOAD_CALLBACK_PREFIX = "chapdl:";
    private static final long PENDING_DOWNLOAD_TTL_MILLIS = 24L * 60L * 60L * 1000L;

    /**
     * Returns the current date and time as a formatted string (yyyy-MM-dd HH:mm:ss).
     */
    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static java.util.List<String> commonYtDlpArgs() {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("--remote-components");
        args.add("ejs:github");
        if (config.cookiesFilePath != null && !config.cookiesFilePath.trim().isEmpty()) {
            args.add("--cookies");
            args.add(config.cookiesFilePath);
        }
        return args;
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
        if (update.hasCallbackQuery()) {
            return handleCallback(telegram, update.getCallbackQuery());
        }
        if (update.hasMessage() && update.getMessage() != null && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            
            // Check for null text
            if (text == null) {
                return false;
            }
            java.util.List<DownloadRequest> requests = DownloadRequestParser.parse(text);
            if (requests.size() > 1) {
                long batchStart = System.currentTimeMillis();
                int approxSec = (int)Math.ceil(requests.size() * 60.0 / config.maxParallelDownloads); // 1 minute per link, parallel processing
                telegram.sendText(message.getChatId(), "🤯 Detected " + requests.size() + " YouTube links! Up to " + config.maxParallelDownloads + " will be processed in parallel. Files will be sent as soon as each is ready.\nApproximate export time: " + approxSec + " seconds (" + (approxSec/60) + " min)");
                new Thread(() -> {
                    int total = requests.size();
                    int[] done = {0};
                    int[] error = {0};
                    java.util.List<String> errorDetails = new java.util.ArrayList<>();
                    java.util.concurrent.ExecutorService batchExec = java.util.concurrent.Executors.newFixedThreadPool(config.maxParallelDownloads);
                    java.util.List<java.util.concurrent.Callable<Void>> tasks = new java.util.ArrayList<>();
                    for (int i = 0; i < requests.size(); i++) {
                        final int idx = i;
                        final DownloadRequest request = requests.get(i);
                        tasks.add(() -> {
                            try {
                                boolean result = processRequestWithPreflight(telegram, message.getChatId(), request, idx + 1, total);
                                if (result) {
                                    synchronized (done) { done[0]++; }
                                } else {
                                    synchronized (error) { error[0]++; }
                                    synchronized (errorDetails) { errorDetails.add(request.url()); }
                                }
                            } catch (Exception ex) {
                                synchronized (error) { error[0]++; }
                                synchronized (errorDetails) { errorDetails.add(request.url() + " (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")"); }
                                logger.error("[{}] Error processing URL: {}\n{}", now(), request.url(), ex.getMessage(), ex);
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
            } else if (requests.size() == 1) {
                telegram.sendText(message.getChatId(), "[SUCCESS ✅] Link accepted! 🎬 Starting processing...");
                telegram.sendChatAction(message.getChatId(), ActionType.UPLOADDOCUMENT);
                executor.submit(() -> processRequestWithPreflight(telegram, message.getChatId(), requests.get(0), 1, 1));
                return true;
            } else {
                telegram.sendText(message.getChatId(), "[ERROR ☢️☣️] Please send a valid YouTube video link. 🚫");
                return false;
            }
        }
        return false;
    }

    private static boolean handleCallback(TelegramService telegram, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data == null) {
            return false;
        }
        if (data.startsWith(CHAPTER_DOWNLOAD_CALLBACK_PREFIX)) {
            return handleChapterCallback(telegram, callbackQuery, data);
        }
        if (!data.startsWith(FORCE_DOWNLOAD_CALLBACK_PREFIX)) {
            return false;
        }
        String id = data.substring(FORCE_DOWNLOAD_CALLBACK_PREFIX.length());
        PendingDownload pending = pendingDuplicateDownloads.remove(id);
        if (pending == null || pending.isExpired()) {
            telegram.answerCallback(callbackQuery.getId(), "This duplicate action expired. Send the link again.");
            return true;
        }
        telegram.answerCallback(callbackQuery.getId(), "Queued for download.");
        telegram.sendText(pending.chatId(), "[SUCCESS ✅] Forced download queued. Starting processing...");
        executor.submit(() -> processDownloadWithStatus(
                telegram,
                pending.chatId(),
                pending.request(),
                pending.index(),
                pending.total(),
                true
        ));
        return true;
    }

    private static boolean handleChapterCallback(TelegramService telegram, CallbackQuery callbackQuery, String data) {
        String id = data.substring(CHAPTER_DOWNLOAD_CALLBACK_PREFIX.length());
        PendingChapterDownload pending = pendingChapterDownloads.remove(id);
        if (pending == null || pending.isExpired()) {
            telegram.answerCallback(callbackQuery.getId(), "This chapter download expired. Send the link again.");
            return true;
        }
        telegram.answerCallback(callbackQuery.getId(), "Chapter download queued.");
        telegram.sendText(pending.chatId(), "[SUCCESS ✅] Chapter download approved. Starting processing...");
        executor.submit(() -> processChapterDownloadWithStatus(
                telegram,
                pending.chatId(),
                pending.request(),
                pending.index(),
                pending.total()
        ));
        return true;
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
    private static boolean processDownloadWithStatus(TelegramService telegram, Long chatIdLong, DownloadRequest request, int index, int total, boolean forceDownload) {
        String url = request.url();
        String chatId = chatIdLong.toString();
        final boolean[] sending = {true};
        Thread progressThread = new Thread(() -> {
            while (sending[0]) {
                telegram.sendChatAction(chatIdLong, ActionType.UPLOADDOCUMENT);
                try { Thread.sleep(1000); } catch (InterruptedException e) {
                    logger.warn("[{}] InterruptedException occurred: {}", now(), e.getMessage(), e);
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        });
        logger.debug("[{}] Starting progress thread for chatId: {}", now(), chatId);
        progressThread.start();
        try {
            
            // Update yt-dlp parameters to extract audio only (for getting metadata)
            java.util.List<String> metadataCommand = new java.util.ArrayList<>(java.util.Arrays.asList(
                config.ytDlpPath,
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", "320k",
                "--dump-json"
            ));
            metadataCommand.addAll(commonYtDlpArgs());
            metadataCommand.add(url);
            ProcessBuilder pb = new ProcessBuilder(metadataCommand);
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
                    telegram.sendChatAction(chatIdLong, ActionType.UPLOADDOCUMENT); // Continue showing loader
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
            sending[0] = false; // Stop the progress thread
            logger.debug("[{}] Stopping progress thread for chatId: {}", now(), chatId);
            try {
                progressThread.join(); // Wait for the thread to finish
                logger.debug("[{}] Progress thread joined successfully for chatId: {}", now(), chatId);
            } catch (InterruptedException e) {
                logger.warn("[{}] Progress thread interrupted for chatId: {}", now(), chatId, e);
                Thread.currentThread().interrupt();
            }
            // Send final status
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
            java.io.File saveDir = Utils.getYoutubeMp3WorkzoneDir();
            if (!saveDir.exists()) saveDir.mkdirs();
            String finalFile = baseFileName + ".mp3";
            java.io.File finalAudioFile = new java.io.File(saveDir, finalFile);

            if (!forceDownload) {
                if (finalAudioFile.exists() && finalAudioFile.length() > 0) {
                    duplicateIndex.addOrUpdateDownloadedFile(finalFile, finalAudioFile.toPath());
                }

                java.util.Optional<MusicDuplicateIndex.DuplicateMatch> duplicate = duplicateIndex.findDuplicate(baseFileName);
                if (duplicate.isPresent()) {
                    sendDuplicateWarning(telegram, chatIdLong, request, index, total, baseFileName, duplicate.get());
                    return true;
                }
            }

            ytDlpService.deleteFileIfExists(finalAudioFile);

            // 4. Download audio
            boolean success = ytDlpService.downloadAudioWithThumbnail(url, finalAudioFile.getAbsolutePath());
            if (!success && !finalAudioFile.exists()) {
                telegram.sendText(chatIdLong, "[ERROR ☢️☣️] Error downloading or converting audio. Check the link or try another video. (" + index + "/" + total + ")\nURL: " + url + " ❌");
                return false;
            }

            if (request.hasClipRange()) {
                telegram.sendChatAction(chatIdLong, ActionType.TYPING);
                boolean clipOk = ytDlpService.trimAudioRange(finalAudioFile, request.clipRange());
                if (!clipOk) {
                    telegram.sendText(chatIdLong, "[ERROR ☢️☣️] Error trimming audio range " + request.clipRange().formatLabel() + ". (" + index + "/" + total + ")\nURL: " + url + " ✂️");
                    ytDlpService.deleteFileIfExists(finalAudioFile);
                    return false;
                }
            }

            // 5. Check limits
            telegram.sendChatAction(chatIdLong, ActionType.TYPING);
            if (!finalAudioFile.exists() || finalAudioFile.length() == 0) {
                logger.error("[{}] [FileNotFound] Downloaded file does not exist or is empty: {} | URL: {}", 
                            now(), finalAudioFile.getAbsolutePath(), url);
                String errMsg = "[ERROR ☢️☣️] Download failed. The audio file is too large (over " + (config.maxFileSize / 1024 / 1024) + " MB) or the video is unavailable. (" + index + "/" + total + ")\nURL: " + url + " ❓";
                telegram.sendText(chatIdLong, errMsg);
                ytDlpService.deleteFileIfExists(finalAudioFile);
                return false;
            }
            double durationAfterDownload = ytDlpService.getAudioDurationSeconds(finalAudioFile.getAbsolutePath());
            if (!ytDlpService.isDurationWithinLimit(durationAfterDownload)) {
                logger.warn("[{}] [DurationLimit] Video too long: {} seconds | URL: {} | Expected limit: {} seconds", now(), durationAfterDownload, url, 30 * 60);
                String errMsg = "[ERROR ☢️☣️] Video is too long (over 30 minutes). Try another video. (" + index + "/" + total + ")\nURL: " + url + " ⏳";
                telegram.sendText(chatIdLong, errMsg);
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
                    telegram.sendText(chatIdLong, errMsg);
                } else if (fileSize > 0) {
                    logger.warn("[{}] [FileSizeLimit] File too large: {} bytes | URL: {} | Expected limit: {} bytes", 
                               now(), fileSize, url, 50 * 1024 * 1024);
                    String errMsg = "[ERROR ☢️☣️] Audio file exceeds 50 MB (" + String.format("%.2f MB", fileSize / 1024.0 / 1024.0) + "). Try another video. (" + index + "/" + total + ")\nURL: " + url + " 💾";
                    telegram.sendText(chatIdLong, errMsg);
                } else {
                    logger.error("[{}] [FileNotFound] Downloaded file does not exist: {} | URL: {}", 
                                now(), finalAudioFile.getAbsolutePath(), url);
                    String errMsg = "[ERROR ☢️☣️] Download failed. File not found. (" + index + "/" + total + ")\nURL: " + url + " ❓";
                    telegram.sendText(chatIdLong, errMsg);
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
            if (request.hasClipRange()) {
                msg.append("\nCUTTED ").append(request.clipRange().formatLabel());
            }
            msg.append("\n🔗 YouTube: ").append(url); // Add YouTube link
            if (fallbackUsed) {
                msg.append("\n\nTitle taken from <title> tag of YouTube page (curl fallback)");
            }
            duplicateIndex.addOrUpdateDownloadedFile(afterName, finalAudioFile.toPath());
            telegram.sendAudio(chatId, finalAudioFile, msg.toString());
            logger.info("[{}] [SendAudio] Sent audio for URL: {}", now(), url);
            return true;
        } catch (IOException e) {
            logger.error("[{}] IOException occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] File or disk access error: (" + index + "/" + total + ")\nURL: " + url + " 💾";
            telegram.sendText(chatIdLong, errMsg);
        } catch (InterruptedException e) {
            logger.error("[{}] InterruptedException occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] Operation was interrupted: (" + index + "/" + total + ")\nURL: " + url + " ⏹️";
            telegram.sendText(chatIdLong, errMsg);
        } catch (Exception e) {
            logger.error("[{}] General exception occurred: {} | URL: {} ({} / {})", now(), e.getMessage(), url, index, total, e);
            String errMsg = "[ERROR ☢️☣️] An unexpected error occurred: (" + index + "/" + total + ")\nURL: " + url + " ❌";
            telegram.sendText(chatIdLong, errMsg);
        } finally {
            sending[0] = false; // Stop the progress thread
            try {
                progressThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                logger.warn("[{}] Progress thread interrupted: {}", now(), e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private static boolean processRequestWithPreflight(TelegramService telegram, Long chatId, DownloadRequest request, int index, int total) {
        if (request.hasClipRange()) {
            return processDownloadWithStatus(telegram, chatId, request, index, total, false);
        }
        try {
            YoutubeVideoMetadata metadata = ytDlpService.getVideoMetadata(request.url());
            if (metadata.hasMultipleChapters()) {
                sendChapterApproval(telegram, chatId, request, index, total, metadata);
                return true;
            }
        } catch (Exception e) {
            logger.warn("[{}] Failed to inspect chapters for URL: {}. Falling back to regular flow.",
                    now(), request.url(), e);
        }
        return processDownloadWithStatus(telegram, chatId, request, index, total, false);
    }

    private static void sendChapterApproval(
            TelegramService telegram,
            Long chatId,
            DownloadRequest request,
            int index,
            int total,
            YoutubeVideoMetadata metadata
    ) {
        cleanupExpiredPendingDownloads();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        pendingChapterDownloads.put(id, new PendingChapterDownload(chatId, request, index, total, System.currentTimeMillis()));

        java.util.List<ChapterTrackPlan> plans = buildChapterTrackPlans(metadata);
        StringBuilder message = new StringBuilder();
        message.append("[CHAPTERS 🎼] Video has ").append(plans.size()).append(" tracks. (")
                .append(index).append("/").append(total).append(")\n");
        message.append("Channel: ").append(metadata.channel()).append("\n");
        message.append("Video: ").append(metadata.title()).append("\n\n");

        int previewCount = Math.min(plans.size(), 25);
        for (int i = 0; i < previewCount; i++) {
            ChapterTrackPlan plan = plans.get(i);
            message.append(i + 1).append(". ")
                    .append(plan.baseName()).append(" — ")
                    .append(formatDuration(plan.chapter().durationSeconds()))
                    .append("\n");
        }
        if (plans.size() > previewCount) {
            message.append("...and ").append(plans.size() - previewCount).append(" more\n");
        }
        message.append("\nPress Download to split and send these tracks.");

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Download");
        button.setCallbackData(CHAPTER_DOWNLOAD_CALLBACK_PREFIX + id);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(java.util.List.of(java.util.List.of(button)));
        telegram.sendText(chatId, message.toString(), markup);
    }

    static java.util.List<ChapterTrackPlan> buildChapterTrackPlans(YoutubeVideoMetadata metadata) {
        return ChapterTrackPlanner.build(metadata);
    }

    private static boolean processChapterDownloadWithStatus(TelegramService telegram, Long chatIdLong, DownloadRequest request, int index, int total) {
        String url = request.url();
        String chatId = chatIdLong.toString();
        final boolean[] sending = {true};
        Thread progressThread = new Thread(() -> {
            while (sending[0]) {
                telegram.sendChatAction(chatIdLong, ActionType.UPLOADDOCUMENT);
                try { Thread.sleep(1000); } catch (InterruptedException e) {
                    logger.warn("[{}] Chapter progress thread interrupted: {}", now(), e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        progressThread.start();

        java.io.File fullAudioFile = null;
        try {
            YoutubeVideoMetadata metadata = ytDlpService.getVideoMetadata(url);
            if (!metadata.hasMultipleChapters()) {
                telegram.sendText(chatIdLong, "[ERROR ☢️☣️] No chapters found anymore. Try sending the link again. (" + index + "/" + total + ")\nURL: " + url);
                return false;
            }

            java.io.File saveDir = Utils.getYoutubeMp3WorkzoneDir();
            if (!saveDir.exists()) saveDir.mkdirs();
            java.io.File tempDir = new java.io.File(saveDir, "temp_mp3");
            if (!tempDir.exists()) tempDir.mkdirs();

            java.util.List<ChapterTrackPlan> plans = buildChapterTrackPlans(metadata);
            java.util.List<ChapterTrackPlan> toDownload = new java.util.ArrayList<>();
            java.util.List<SkippedChapter> skipped = new java.util.ArrayList<>();

            for (ChapterTrackPlan plan : plans) {
                java.io.File finalAudioFile = new java.io.File(saveDir, plan.fileName());
                if (finalAudioFile.exists() && finalAudioFile.length() > 0) {
                    duplicateIndex.addOrUpdateDownloadedFile(plan.fileName(), finalAudioFile.toPath());
                }
                java.util.Optional<MusicDuplicateIndex.DuplicateMatch> duplicate = duplicateIndex.findDuplicate(plan.baseName());
                if (duplicate.isPresent()) {
                    skipped.add(new SkippedChapter(plan, duplicate.get()));
                } else {
                    toDownload.add(plan);
                }
            }

            if (toDownload.isEmpty()) {
                telegram.sendText(chatIdLong, buildChapterSummary("All chapter tracks already exist. Nothing downloaded.", plans.size(), 0, skipped, java.util.List.of(), url));
                return true;
            }

            fullAudioFile = new java.io.File(tempDir, "chapters_source_" + System.currentTimeMillis() + ".mp3");
            boolean audioOk = ytDlpService.downloadAudioWithThumbnail(url, fullAudioFile.getAbsolutePath());
            if (!audioOk || !fullAudioFile.exists() || fullAudioFile.length() == 0) {
                telegram.sendText(chatIdLong, "[ERROR ☢️☣️] Error downloading source audio for chapter split. (" + index + "/" + total + ")\nURL: " + url + " ❌");
                return false;
            }

            java.util.List<String> failed = new java.util.ArrayList<>();
            int sent = 0;
            for (ChapterTrackPlan plan : toDownload) {
                java.io.File chapterFile = new java.io.File(saveDir, plan.fileName());
                ytDlpService.deleteFileIfExists(chapterFile);
                boolean splitOk = ytDlpService.splitAudioRange(fullAudioFile, plan.chapter().clipRange(), chapterFile);
                if (!splitOk || !chapterFile.exists() || chapterFile.length() == 0) {
                    failed.add(plan.fileName() + " (split failed)");
                    ytDlpService.deleteFileIfExists(chapterFile);
                    continue;
                }

                double duration = ytDlpService.getAudioDurationSeconds(chapterFile.getAbsolutePath());
                if (!ytDlpService.isDurationWithinLimit(duration)) {
                    failed.add(plan.fileName() + " (too long: " + formatDuration(duration) + ")");
                    ytDlpService.deleteFileIfExists(chapterFile);
                    continue;
                }
                if (!ytDlpService.isFileSizeWithinLimit(chapterFile)) {
                    failed.add(plan.fileName() + " (too large)");
                    ytDlpService.deleteFileIfExists(chapterFile);
                    continue;
                }

                StringBuilder msg = new StringBuilder();
                msg.append("[SUCCESS ✅] Chapter audio ready! 🎶 ")
                        .append(sent + 1).append("/").append(toDownload.size()).append("\n");
                msg.append("After: ").append(plan.fileName()).append("\n");
                msg.append("Range: ").append(plan.chapter().clipRange().formatLabel()).append("\n");
                msg.append("YouTube: ").append(url);
                duplicateIndex.addOrUpdateDownloadedFile(plan.fileName(), chapterFile.toPath());
                telegram.sendAudio(chatId, chapterFile, msg.toString());
                sent++;
            }

            telegram.sendText(chatIdLong, buildChapterSummary("Chapter split complete.", plans.size(), sent, skipped, failed, url));
            logger.info("[{}] [SendAudio] Sent {} chapter tracks for URL: {}", now(), sent, url);
            return failed.isEmpty();
        } catch (IOException e) {
            logger.error("[{}] IOException during chapter download: {} | URL: {}", now(), e.getMessage(), url, e);
            telegram.sendText(chatIdLong, "[ERROR ☢️☣️] File or disk access error during chapter split. (" + index + "/" + total + ")\nURL: " + url + " 💾");
        } catch (InterruptedException e) {
            logger.error("[{}] Chapter download interrupted: {} | URL: {}", now(), e.getMessage(), url, e);
            telegram.sendText(chatIdLong, "[ERROR ☢️☣️] Chapter operation was interrupted. (" + index + "/" + total + ")\nURL: " + url + " ⏹️");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("[{}] General chapter exception: {} | URL: {}", now(), e.getMessage(), url, e);
            telegram.sendText(chatIdLong, "[ERROR ☢️☣️] Unexpected chapter split error. (" + index + "/" + total + ")\nURL: " + url + " ❌");
        } finally {
            sending[0] = false;
            if (fullAudioFile != null) {
                ytDlpService.deleteFileIfExists(fullAudioFile);
            }
            try {
                progressThread.join();
            } catch (InterruptedException e) {
                logger.warn("[{}] Chapter progress thread join interrupted: {}", now(), e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private static String buildChapterSummary(
            String title,
            int total,
            int sent,
            java.util.List<SkippedChapter> skipped,
            java.util.List<String> failed,
            String url
    ) {
        StringBuilder msg = new StringBuilder();
        msg.append("[SUMMARY] ").append(title).append("\n");
        msg.append("Total chapters: ").append(total).append("\n");
        msg.append("Sent: ").append(sent).append("\n");
        msg.append("Skipped duplicates: ").append(skipped.size()).append("\n");
        msg.append("Failed: ").append(failed.size()).append("\n");
        appendSkippedPreview(msg, skipped);
        appendFailedPreview(msg, failed);
        msg.append("\nYouTube: ").append(url);
        return msg.toString();
    }

    private static void appendSkippedPreview(StringBuilder msg, java.util.List<SkippedChapter> skipped) {
        if (skipped.isEmpty()) {
            return;
        }
        msg.append("\nSkipped:\n");
        int count = Math.min(skipped.size(), 10);
        for (int i = 0; i < count; i++) {
            SkippedChapter skippedChapter = skipped.get(i);
            msg.append("- ").append(skippedChapter.plan().fileName())
                    .append(" -> ").append(skippedChapter.duplicate().displayName())
                    .append("\n");
        }
        if (skipped.size() > count) {
            msg.append("...and ").append(skipped.size() - count).append(" more\n");
        }
    }

    private static void appendFailedPreview(StringBuilder msg, java.util.List<String> failed) {
        if (failed.isEmpty()) {
            return;
        }
        msg.append("\nFailed:\n");
        int count = Math.min(failed.size(), 10);
        for (int i = 0; i < count; i++) {
            msg.append("- ").append(failed.get(i)).append("\n");
        }
        if (failed.size() > count) {
            msg.append("...and ").append(failed.size() - count).append(" more\n");
        }
    }

    private static String formatDuration(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            return "unknown";
        }
        int rounded = (int) Math.round(seconds);
        int hours = rounded / 3600;
        int minutes = (rounded % 3600) / 60;
        int secs = rounded % 60;
        return hours > 0
                ? String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, secs)
                : String.format(java.util.Locale.US, "%d:%02d", minutes, secs);
    }

    private static void sendDuplicateWarning(
            TelegramService telegram,
            Long chatId,
            DownloadRequest request,
            int index,
            int total,
            String candidateName,
            MusicDuplicateIndex.DuplicateMatch duplicate
    ) {
        cleanupExpiredPendingDownloads();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        pendingDuplicateDownloads.put(id, new PendingDownload(chatId, request, index, total, System.currentTimeMillis()));

        StringBuilder message = new StringBuilder();
        message.append("[DUPLICATE ⚠️] Skipped download: this track already exists in the music library. (")
                .append(index).append("/").append(total).append(")\n");
        message.append("Requested: ").append(candidateName).append(".mp3\n");
        message.append("Found: ").append(duplicate.displayName());
        if (duplicate.path() != null && !duplicate.path().isBlank()) {
            message.append("\nPath: ").append(duplicate.path());
        }
        message.append("\nMatch: ").append(duplicate.matchType())
                .append(" ").append(String.format(java.util.Locale.US, "%.0f%%", duplicate.score() * 100));
        message.append("\n\nUse the button below to download anyway.");

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Download anyway");
        button.setCallbackData(FORCE_DOWNLOAD_CALLBACK_PREFIX + id);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(java.util.List.of(java.util.List.of(button)));
        telegram.sendText(chatId, message.toString(), markup);
    }

    private static void cleanupExpiredPendingDownloads() {
        pendingDuplicateDownloads.entrySet().removeIf(entry -> entry.getValue().isExpired());
        pendingChapterDownloads.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record PendingDownload(Long chatId, DownloadRequest request, int index, int total, long createdAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > PENDING_DOWNLOAD_TTL_MILLIS;
        }
    }

    private record PendingChapterDownload(Long chatId, DownloadRequest request, int index, int total, long createdAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > PENDING_DOWNLOAD_TTL_MILLIS;
        }
    }

    private record SkippedChapter(ChapterTrackPlan plan, MusicDuplicateIndex.DuplicateMatch duplicate) {
    }
}
