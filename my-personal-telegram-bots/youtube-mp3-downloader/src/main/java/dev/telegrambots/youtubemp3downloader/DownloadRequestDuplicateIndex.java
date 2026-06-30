package dev.telegrambots.youtubemp3downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DownloadRequestDuplicateIndex {
    private static final Logger logger = LoggerFactory.getLogger(DownloadRequestDuplicateIndex.class);

    private final Path indexPath;
    private long lastModifiedMillis = -1;
    private Map<String, Entry> exactByKey = Map.of();

    public DownloadRequestDuplicateIndex(String musicDuplicateIndexPath) {
        if (musicDuplicateIndexPath == null || musicDuplicateIndexPath.isBlank()) {
            this.indexPath = null;
        } else {
            this.indexPath = Path.of(musicDuplicateIndexPath + ".requests.tsv");
        }
    }

    public boolean isEnabled() {
        return indexPath != null;
    }

    public Optional<Entry> findDuplicate(DownloadRequest request) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String key = buildKey(request);
        if (key.isBlank()) {
            return Optional.empty();
        }
        reloadIfNeeded();
        return Optional.ofNullable(exactByKey.get(key));
    }

    public synchronized boolean addOrUpdate(DownloadRequest request, String displayName, Path filePath) {
        if (!isEnabled() || request == null || displayName == null || displayName.isBlank()) {
            return false;
        }
        String key = buildKey(request);
        if (key.isBlank()) {
            return false;
        }
        reloadIfNeeded();
        Entry entry = new Entry(
                key,
                displayName,
                filePath == null ? "" : filePath.toAbsolutePath().toString()
        );
        Entry existing = exactByKey.get(key);
        if (existing != null && existing.path().equals(entry.path()) && existing.displayName().equals(entry.displayName())) {
            return false;
        }

        try {
            Path parent = indexPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            boolean writeHeader = !Files.exists(indexPath) || Files.size(indexPath) == 0;
            try (BufferedWriter writer = Files.newBufferedWriter(
                    indexPath,
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            )) {
                if (writeHeader) {
                    writer.write("request_key\tdisplay_name\tpath");
                    writer.newLine();
                }
                writer.write(escapeTsv(entry.key()));
                writer.write('\t');
                writer.write(escapeTsv(entry.displayName()));
                writer.write('\t');
                writer.write(escapeTsv(entry.path()));
                writer.newLine();
            }

            Map<String, Entry> updated = new HashMap<>(exactByKey);
            updated.put(entry.key(), entry);
            exactByKey = Map.copyOf(updated);
            lastModifiedMillis = Files.getLastModifiedTime(indexPath).toMillis();
            logger.info("Added downloaded request to duplicate request index: {} -> {}", entry.key(), entry.displayName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to append downloaded request to duplicate request index: {}", indexPath, e);
            return false;
        }
    }

    static String buildKey(DownloadRequest request) {
        if (request == null) {
            return "";
        }
        String videoId = Utils.extractVideoId(request.url());
        if (videoId == null || videoId.isBlank()) {
            return "";
        }
        String range = request.hasClipRange() ? request.clipRange().formatLabel() : "full";
        return videoId.toLowerCase(Locale.ROOT) + "|" + range;
    }

    private void reloadIfNeeded() {
        try {
            if (!Files.exists(indexPath)) {
                lastModifiedMillis = 0;
                exactByKey = Map.of();
                return;
            }
            long currentModified = Files.getLastModifiedTime(indexPath).toMillis();
            if (currentModified == lastModifiedMillis) {
                return;
            }
            exactByKey = Map.copyOf(loadIndex(indexPath));
            lastModifiedMillis = currentModified;
            logger.info("Loaded duplicate request index: {} entries from {}", exactByKey.size(), indexPath);
        } catch (Exception e) {
            logger.error("Failed to load duplicate request index: {}", indexPath, e);
            exactByKey = Map.of();
        }
    }

    private static Map<String, Entry> loadIndex(Path path) throws IOException {
        Map<String, Entry> loaded = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("request_key\t")) {
                    continue;
                }
                String[] parts = line.split("\t", 3);
                if (parts.length < 2 || parts[0].isBlank()) {
                    continue;
                }
                String filePath = parts.length == 3 ? parts[2].trim() : "";
                loaded.put(parts[0].trim(), new Entry(parts[0].trim(), parts[1].trim(), filePath));
            }
        }
        return loaded;
    }

    private static String escapeTsv(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
    }

    public record Entry(String key, String displayName, String path) {
    }
}
