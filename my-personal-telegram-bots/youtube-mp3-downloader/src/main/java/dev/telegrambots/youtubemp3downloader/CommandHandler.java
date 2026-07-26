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
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final BotConfig config = new BotConfig();
    private static final YtDlpService ytDlpService = new YtDlpService(
            config.ytDlpPath, config.ffmpegPath, config.ffprobePath, config.maxFileSize, config.maxDurationMinutes,
            config.cookiesFilePath);
    private static final MusicDuplicateIndex duplicateIndex = new MusicDuplicateIndex(config.duplicateIndexPath);
    private static final DownloadRequestDuplicateIndex requestDuplicateIndex = new DownloadRequestDuplicateIndex(config.duplicateIndexPath);
    private static final ConcurrentHashMap<String, PendingDownload> pendingDuplicateDownloads = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PendingChapterDownload> pendingChapterDownloads = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> downloadFileLocks = new ConcurrentHashMap<>();
    private static final String FORCE_DOWNLOAD_CALLBACK_PREFIX = "dupdl:";
    private static final String CHAPTER_DOWNLOAD_CALLBACK_PREFIX = "chapdl:";
    private static final long PENDING_DOWNLOAD_TTL_MILLIS = 24L * 60L * 60L * 1000L;
    private static final java.util.regex.Pattern CUT_COMMAND_PATTERN = java.util.regex.Pattern.compile(
            "^\\s*/cut(?:@\\w+)?\\s+(\\d+(?::\\d{1,2}){0,2}(?:\\.\\d+)?)\\s+(\\d+(?::\\d{1,2}){0,2}(?:\\.\\d+)?)\\s*$",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

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

    static boolean isUnsafeMetadataName(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("http://")
                || normalized.contains("https://")
                || normalized.contains("youtube.com")
                || normalized.contains("youtu.be")
                || value.matches(".*[=\\/\\:*?\"<>|].*");
    }

    static String fallbackBaseFileName(String url) {
        String videoId = Utils.extractVideoId(url);
        return videoId == null || videoId.isBlank() ? "video" : "video-" + videoId;
    }

    static String metadataFallbackWarningLine(String url, String fileName) {
        return "- " + url + " -> " + fileName;
    }

    static boolean isCutCommand(String text) {
        if (text == null) {
            return false;
        }
        String command = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int botMention = command.indexOf('@');
        if (botMention >= 0) {
            command = command.substring(0, botMention);
        }
        return "/cut".equals(command);
    }

    static AudioClipRange parseCutCommandRange(String text) {
        java.util.regex.Matcher matcher = CUT_COMMAND_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.matches()) {
            return null;
        }
        try {
            double start = AudioClipRange.parseTimeSeconds(matcher.group(1));
            double end = AudioClipRange.parseTimeSeconds(matcher.group(2));
            return new AudioClipRange(start, end);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static boolean isSanitizeMp3Command(String text) {
        if (text == null) {
            return false;
        }
        String command = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int botMention = command.indexOf('@');
        if (botMention >= 0) {
            command = command.substring(0, botMention);
        }
        return "/sanitize_mp3".equals(command) || "/delete_mp3".equals(command);
    }

    static boolean isSanitizeMp3DryRun(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(" dry")
                || normalized.contains(" dry-run")
                || normalized.contains(" preview")
                || normalized.endsWith(" true");
    }

    static String buildSanitizeMp3Summary(FileNameSanitizer.SanitizeDirectoryResult result) {
        if (result.directoryMissing()) {
            return "[ERROR ☢️☣️] MP3 workzone not found:\n" + result.directory();
        }
        if (result.directoryUnreadable()) {
            return "[ERROR ☢️☣️] MP3 workzone cannot be read:\n" + result.directory();
        }
        StringBuilder msg = new StringBuilder();
        msg.append(result.dryRun() ? "[DRY RUN] MP3 sanitize preview complete.\n" : "[SUCCESS ✅] MP3 sanitize complete.\n");
        msg.append("Directory: ").append(result.directory()).append("\n");
        msg.append("Total checked: ").append(result.total()).append("\n");
        if (result.dryRun()) {
            msg.append("Files to rename: ").append(result.affected()).append("\n");
        } else {
            msg.append("Files renamed: ").append(result.changed()).append(" out of ").append(result.affected()).append("\n");
            msg.append("Files failed: ").append(result.failed()).append("\n");
        }
        msg.append("Already clean: ").append(result.unchanged());
        if (!result.details().isEmpty()) {
            msg.append("\n\nDetails:\n");
            int limit = Math.min(result.details().size(), 20);
            for (int i = 0; i < limit; i++) {
                msg.append("- ").append(result.details().get(i)).append("\n");
            }
            if (result.details().size() > limit) {
                msg.append("...and ").append(result.details().size() - limit).append(" more");
            }
        }
        return msg.toString().trim();
    }

    private static boolean handleSanitizeMp3Command(TelegramService telegram, Long chatId, String text) {
        boolean dryRun = isSanitizeMp3DryRun(text);
        java.io.File workzone = Utils.getYoutubeMp3WorkzoneDir();
        telegram.sendText(chatId, dryRun
                ? "[DRY RUN] Checking MP3 names in workzone..."
                : "[STARTED] Sanitizing MP3 names in workzone...");
        executor.submit(() -> {
            try {
                FileNameSanitizer.SanitizeDirectoryResult result =
                        FileNameSanitizer.sanitizeAllInDirectoryWithResult(workzone.getAbsolutePath(), ".mp3", dryRun);
                telegram.sendText(chatId, buildSanitizeMp3Summary(result));
                logger.info("[{}] MP3 sanitize command finished. dryRun={}, dir={}, total={}, affected={}, changed={}, failed={}",
                        now(), dryRun, result.directory(), result.total(), result.affected(), result.changed(), result.failed());
            } catch (Exception e) {
                logger.error("[{}] MP3 sanitize command failed", now(), e);
                telegram.sendText(chatId, "[ERROR ☢️☣️] MP3 sanitize failed: " + e.getMessage());
            }
        });
        return true;
    }

    private static String messageCommandText(Message message) {
        if (message == null) {
            return null;
        }
        if (message.hasText()) {
            return message.getText();
        }
        String caption = message.getCaption();
        if (caption != null && !caption.isBlank()) {
            return caption;
        }
        return null;
    }

    static TelegramAudioAttachment extractAudioAttachment(Message message) {
        if (message == null) {
            return null;
        }
        if (message.hasAudio() && message.getAudio() != null) {
            org.telegram.telegrambots.meta.api.objects.Audio audio = message.getAudio();
            String fileName = audio.getFileName();
            if (fileName == null || fileName.isBlank()) {
                fileName = audio.getTitle();
            }
            return new TelegramAudioAttachment(audio.getFileId(), fileName, thumbnailFileId(audio.getThumbnail()));
        }
        if (message.hasDocument() && message.getDocument() != null) {
            org.telegram.telegrambots.meta.api.objects.Document document = message.getDocument();
            String fileName = document.getFileName();
            String mimeType = document.getMimeType();
            if (isAudioDocument(fileName, mimeType)) {
                return new TelegramAudioAttachment(document.getFileId(), fileName, thumbnailFileId(document.getThumbnail()));
            }
        }
        return null;
    }

    private static String thumbnailFileId(org.telegram.telegrambots.meta.api.objects.PhotoSize thumbnail) {
        return thumbnail == null ? null : thumbnail.getFileId();
    }

    static boolean isAudioDocument(String fileName, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".mp3")
                || normalized.endsWith(".m4a")
                || normalized.endsWith(".wav")
                || normalized.endsWith(".flac")
                || normalized.endsWith(".ogg")
                || normalized.endsWith(".opus")
                || normalized.endsWith(".aac");
    }

    private static boolean handleCutCommand(TelegramService telegram, Long chatId, Message message, String text) {
        AudioClipRange range = parseCutCommandRange(text);
        if (range == null) {
            telegram.sendText(chatId, "[ERROR ☢️☣️] Usage: /cut 0:00 2:50 with an attached audio file.");
            return true;
        }
        TelegramAudioAttachment attachment = extractAudioAttachment(message);
        if (attachment == null || attachment.fileId() == null || attachment.fileId().isBlank()) {
            telegram.sendText(chatId, "[ERROR ☢️☣️] Attach an audio file with /cut " + range.formatLabel().replace(" - ", " ") + ".");
            return true;
        }
        telegram.sendText(chatId, "[SUCCESS ✅] File accepted! ✂️ Starting trim " + range.formatLabel() + "...");
        telegram.sendChatAction(chatId, ActionType.UPLOADDOCUMENT);
        executor.submit(() -> processUploadedAudioCut(telegram, chatId, attachment, range));
        return true;
    }

    private static void processUploadedAudioCut(TelegramService telegram, Long chatId, TelegramAudioAttachment attachment, AudioClipRange range) {
        java.io.File sourceFile = null;
        java.io.File outputFile = null;
        java.io.File thumbnailFile = null;
        java.io.File jobDir = null;
        final boolean[] sending = {true};
        Thread progressThread = new Thread(() -> {
            while (sending[0]) {
                telegram.sendChatAction(chatId, ActionType.UPLOADDOCUMENT);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warn("[{}] Uploaded cut progress thread interrupted: {}", now(), e.getMessage(), e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        progressThread.start();
        try {
            java.io.File saveDir = Utils.getYoutubeMp3WorkzoneDir();
            java.io.File tempDir = new java.io.File(saveDir, "temp_mp3");
            if (!tempDir.exists()) tempDir.mkdirs();

            String token = Long.toString(System.currentTimeMillis());
            jobDir = new java.io.File(tempDir, "cut_" + token);
            if (!jobDir.exists()) jobDir.mkdirs();
            String tempBaseName = cutBaseName(attachment.fileName());
            String outputFileName = cutOutputFileName(attachment.fileName());
            sourceFile = new java.io.File(jobDir, tempBaseName + "_upload" + sourceExtension(attachment.fileName()));
            outputFile = new java.io.File(saveDir, outputFileName);

            telegram.downloadFile(attachment.fileId(), sourceFile);
            if (!sourceFile.exists() || sourceFile.length() == 0) {
                telegram.sendText(chatId, "[ERROR ☢️☣️] Could not download the attached audio file.");
                return;
            }

            Object outputFileLock = downloadFileLocks.computeIfAbsent(outputFileName.toLowerCase(Locale.ROOT), ignored -> new Object());
            synchronized (outputFileLock) {
                ytDlpService.deleteFileIfExists(outputFile);
                boolean cutOk = ytDlpService.splitAudioRange(sourceFile, range, outputFile);
                if (!cutOk || !outputFile.exists() || outputFile.length() == 0) {
                    telegram.sendText(chatId, "[ERROR ☢️☣️] Error trimming audio range " + range.formatLabel() + ". ✂️");
                    return;
                }
                if (attachment.thumbnailFileId() != null && !attachment.thumbnailFileId().isBlank()) {
                    thumbnailFile = new java.io.File(jobDir, tempBaseName + "_thumbnail.jpg");
                    telegram.downloadFile(attachment.thumbnailFileId(), thumbnailFile);
                    if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                        boolean coverOk = ytDlpService.attachCoverArt(outputFile, thumbnailFile);
                        if (!coverOk) {
                            logger.warn("[{}] Could not attach Telegram thumbnail for cut upload: {}", now(), attachment.fileName());
                        }
                    }
                }
                double duration = ytDlpService.getAudioDurationSeconds(outputFile.getAbsolutePath());
                if (!ytDlpService.isDurationWithinLimit(duration)) {
                    telegram.sendText(chatId, "[ERROR ☢️☣️] Trimmed audio is too long: " + formatDuration(duration) + ".");
                    ytDlpService.deleteFileIfExists(outputFile);
                    return;
                }
                if (!ytDlpService.isFileSizeWithinLimit(outputFile)) {
                    long fileSize = outputFile.exists() ? outputFile.length() : -1;
                    telegram.sendText(chatId, "[ERROR ☢️☣️] Trimmed audio exceeds 50 MB (" + String.format(Locale.US, "%.2f MB", fileSize / 1024.0 / 1024.0) + ").");
                    ytDlpService.deleteFileIfExists(outputFile);
                    return;
                }
                duplicateIndex.addOrUpdateDownloadedFile(outputFileName, outputFile.toPath());

                StringBuilder msg = new StringBuilder();
                msg.append("[SUCCESS ✅] Audio cut ready! ✂️\n");
                msg.append("File: ").append(outputFileName).append("\n");
                msg.append("Saved: ").append(outputFile.getAbsolutePath()).append("\n");
                msg.append("Range: ").append(range.formatLabel());
                telegram.sendAudio(chatId.toString(), outputFile, msg.toString());
            }
            logger.info("[{}] [SendAudio] Sent cut upload: {} | Range: {}", now(), attachment.fileName(), range.formatLabel());
        } catch (Exception e) {
            logger.error("[{}] Uploaded audio cut failed", now(), e);
            telegram.sendText(chatId, "[ERROR ☢️☣️] Audio cut failed: " + e.getMessage());
        } finally {
            sending[0] = false;
            if (sourceFile != null) {
                ytDlpService.deleteFileIfExists(sourceFile);
            }
            if (thumbnailFile != null) {
                ytDlpService.deleteFileIfExists(thumbnailFile);
            }
            if (jobDir != null && jobDir.exists()) {
                jobDir.delete();
            }
            try {
                progressThread.join();
            } catch (InterruptedException e) {
                logger.warn("[{}] Uploaded cut progress thread join interrupted: {}", now(), e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    static String cutBaseName(String fileName) {
        String displayName = displayFileName(fileName);
        int dot = displayName.lastIndexOf('.');
        if (dot > 0) {
            displayName = displayName.substring(0, dot);
        }
        String sanitized = FileNameSanitizer.sanitize(displayName);
        return sanitized == null || sanitized.isBlank() ? "Audio" : sanitized.replaceAll("[^A-Za-z0-9._ -]", "").replaceAll("\\s+", "_");
    }

    static String cutOutputFileName(String fileName) {
        String displayName = displayFileName(fileName);
        int dot = displayName.lastIndexOf('.');
        if (dot > 0) {
            displayName = displayName.substring(0, dot);
        }
        String sanitized = FileNameSanitizer.sanitize(displayName);
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "Audio";
        }
        return sanitized + ".mp3";
    }

    private static String displayFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "audio" : fileName;
    }

    private static String sourceExtension(String fileName) {
        if (fileName == null) {
            return ".audio";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return ".audio";
        }
        String ext = fileName.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : ".audio";
    }

    private static void sendMetadataFallbackWarningIfAny(TelegramService telegram, Long chatId, java.util.List<String> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        StringBuilder warning = new StringBuilder();
        warning.append("[WARNING ⚠️] Downloaded, but normal title metadata was unavailable:\n");
        synchronized (details) {
            for (String detail : details) warning.append(detail).append("\n");
        }
        telegram.sendText(chatId, warning.toString());
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
        if (update.hasMessage() && update.getMessage() != null) {
            Message message = update.getMessage();
            String text = messageCommandText(message);
            
            // Check for null text
            if (text == null) {
                return false;
            }
            if (isCutCommand(text)) {
                return handleCutCommand(telegram, message.getChatId(), message, text);
            }
            if (!message.hasText()) {
                return false;
            }
            if (isSanitizeMp3Command(text)) {
                return handleSanitizeMp3Command(telegram, message.getChatId(), text);
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
                    AtomicInteger duplicateCount = new AtomicInteger(0);
                    java.util.List<String> errorDetails = new java.util.ArrayList<>();
                    java.util.List<String> metadataFallbackDetails = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                    java.util.concurrent.ExecutorService batchExec = java.util.concurrent.Executors.newFixedThreadPool(config.maxParallelDownloads);
                    java.util.List<java.util.concurrent.Callable<Void>> tasks = new java.util.ArrayList<>();
                    for (int i = 0; i < requests.size(); i++) {
                        final int idx = i;
                        final DownloadRequest request = requests.get(i);
                        tasks.add(() -> {
                            try {
                                boolean result = processRequestWithPreflight(telegram, message.getChatId(), request, idx + 1, total, duplicateCount, metadataFallbackDetails);
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
                    summary.append("[DUPLICATE ⚠️] Skipped: ").append(duplicateCount.get()).append("\n");
                    summary.append("[ERROR ☢️☣️] Failed: ").append(error[0]).append("\n");
                    summary.append("⏱️ Export time: ").append(elapsedSec).append(" seconds (" + (elapsedSec/60) + " min)\n");
                    if (!errorDetails.isEmpty()) {
                        summary.append("\nFailed URLs:\n");
                        for (String err : errorDetails) summary.append(err).append("\n");
                    }
                    if (!metadataFallbackDetails.isEmpty()) {
                        summary.append("\n[WARNING ⚠️] Downloaded, but normal title metadata was unavailable:\n");
                        synchronized (metadataFallbackDetails) {
                            for (String detail : metadataFallbackDetails) summary.append(detail).append("\n");
                        }
                    }
                    telegram.sendText(message.getChatId(), summary.toString());
                }).start();
                return true;
            } else if (requests.size() == 1) {
                telegram.sendText(message.getChatId(), "[SUCCESS ✅] Link accepted! 🎬 Starting processing...");
                telegram.sendChatAction(message.getChatId(), ActionType.UPLOADDOCUMENT);
                executor.submit(() -> {
                    java.util.List<String> metadataFallbackDetails = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                    processRequestWithPreflight(telegram, message.getChatId(), requests.get(0), 1, 1, new AtomicInteger(0), metadataFallbackDetails);
                    sendMetadataFallbackWarningIfAny(telegram, message.getChatId(), metadataFallbackDetails);
                });
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
                true,
                new AtomicInteger(0),
                null
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
                pending.total(),
                new AtomicInteger(0)
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
    private static boolean processDownloadWithStatus(TelegramService telegram, Long chatIdLong, DownloadRequest request, int index, int total, boolean forceDownload, AtomicInteger duplicateCount, java.util.List<String> metadataFallbackDetails) {
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
            boolean unsafeMetadataFallbackUsed = false;
            String ytTitleRaw = null;
            String ytAuthorRaw = null;
            if (isUnsafeMetadataName(sanitizedChannel) || isUnsafeMetadataName(sanitizedTitle)) {
                String[] curlInfo = extractTitleAuthorFromCurl(url);
                ytTitleRaw = curlInfo[0];
                ytAuthorRaw = curlInfo[1];
                if (ytTitleRaw != null && !ytTitleRaw.isBlank()) {
                    String fallbackTitle = FileNameSanitizer.sanitize(ytTitleRaw);
                    String fallbackChannel = FileNameSanitizer.sanitize(ytAuthorRaw);
                    if (!isUnsafeMetadataName(fallbackTitle)) {
                        sanitizedTitle = fallbackTitle;
                        sanitizedChannel = isUnsafeMetadataName(fallbackChannel) ? null : fallbackChannel;
                        fallbackUsed = true;
                        logger.info("[{}] [yt-dlp-info] Fallback title/author from curl: {} / {}", now(), ytTitleRaw, ytAuthorRaw);
                    } else {
                        sanitizedTitle = fallbackBaseFileName(url);
                        sanitizedChannel = null;
                        unsafeMetadataFallbackUsed = true;
                        logger.warn("[{}] [yt-dlp-info] Ignoring unsafe fallback title from curl: {} | URL: {}", now(), ytTitleRaw, url);
                    }
                } else {
                    sanitizedTitle = fallbackBaseFileName(url);
                    sanitizedChannel = null;
                    unsafeMetadataFallbackUsed = true;
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
            Object downloadFileLock = downloadFileLocks.computeIfAbsent(finalFile.toLowerCase(Locale.ROOT), ignored -> new Object());
            synchronized (downloadFileLock) {

            if (!forceDownload) {
                if (finalAudioFile.exists() && finalAudioFile.length() > 0) {
                    duplicateIndex.addOrUpdateDownloadedFile(finalFile, finalAudioFile.toPath());
                }

                java.util.Optional<MusicDuplicateIndex.DuplicateMatch> duplicate = duplicateIndex.findDuplicate(baseFileName);
                if (duplicate.isPresent()) {
                    duplicateCount.incrementAndGet();
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
            if (unsafeMetadataFallbackUsed) {
                msg.append("\n\n[WARNING ⚠️] Normal title metadata was unavailable; saved with fallback name.");
            }
            duplicateIndex.addOrUpdateDownloadedFile(afterName, finalAudioFile.toPath());
            requestDuplicateIndex.addOrUpdate(request, afterName, finalAudioFile.toPath());
            telegram.sendAudio(chatId, finalAudioFile, msg.toString());
            if (unsafeMetadataFallbackUsed && metadataFallbackDetails != null) {
                metadataFallbackDetails.add(metadataFallbackWarningLine(url, afterName));
            }
            logger.info("[{}] [SendAudio] Sent audio for URL: {}", now(), url);
            return true;
            }
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

    private static boolean processRequestWithPreflight(TelegramService telegram, Long chatId, DownloadRequest request, int index, int total, AtomicInteger duplicateCount, java.util.List<String> metadataFallbackDetails) {
        java.util.Optional<DownloadRequestDuplicateIndex.Entry> requestDuplicate = requestDuplicateIndex.findDuplicate(request);
        if (requestDuplicate.isPresent()) {
            duplicateCount.incrementAndGet();
            sendRequestDuplicateWarning(telegram, chatId, request, index, total, requestDuplicate.get());
            return true;
        }
        if (request.hasClipRange()) {
            return processDownloadWithStatus(telegram, chatId, request, index, total, false, duplicateCount, metadataFallbackDetails);
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
        return processDownloadWithStatus(telegram, chatId, request, index, total, false, duplicateCount, metadataFallbackDetails);
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

    private static boolean processChapterDownloadWithStatus(TelegramService telegram, Long chatIdLong, DownloadRequest request, int index, int total, AtomicInteger duplicateCount) {
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
                duplicateCount.addAndGet(skipped.size());
                telegram.sendText(chatIdLong, buildChapterSummary("All chapter tracks already exist. Nothing downloaded.", plans.size(), 0, skipped, java.util.List.of(), url));
                return true;
            }

            fullAudioFile = new java.io.File(tempDir, "chapters_source_" + System.currentTimeMillis() + ".mp3");
            boolean audioOk = ytDlpService.downloadAudioWithThumbnail(url, fullAudioFile.getAbsolutePath(), false);
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
                requestDuplicateIndex.addOrUpdate(request, plan.fileName(), chapterFile.toPath());
                telegram.sendAudio(chatId, chapterFile, msg.toString());
                sent++;
            }

            duplicateCount.addAndGet(skipped.size());
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

    private static void sendRequestDuplicateWarning(
            TelegramService telegram,
            Long chatId,
            DownloadRequest request,
            int index,
            int total,
            DownloadRequestDuplicateIndex.Entry duplicate
    ) {
        cleanupExpiredPendingDownloads();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        pendingDuplicateDownloads.put(id, new PendingDownload(chatId, request, index, total, System.currentTimeMillis()));

        StringBuilder message = new StringBuilder();
        message.append("[DUPLICATE ⚠️] Skipped download: this YouTube request was already processed. (")
                .append(index).append("/").append(total).append(")\n");
        message.append("Requested: ").append(request.url());
        if (request.hasClipRange()) {
            message.append(" ").append(request.clipRange().formatLabel());
        }
        message.append("\nFound: ").append(duplicate.displayName());
        if (duplicate.path() != null && !duplicate.path().isBlank()) {
            message.append("\nPath: ").append(duplicate.path());
        }
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

    record TelegramAudioAttachment(String fileId, String fileName, String thumbnailFileId) {
    }

    private record SkippedChapter(ChapterTrackPlan plan, MusicDuplicateIndex.DuplicateMatch duplicate) {
    }
}
