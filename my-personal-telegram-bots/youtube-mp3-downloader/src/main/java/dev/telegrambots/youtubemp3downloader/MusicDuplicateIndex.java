package dev.telegrambots.youtubemp3downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class MusicDuplicateIndex {
    private static final Logger logger = LoggerFactory.getLogger(MusicDuplicateIndex.class);
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".flac", ".m4a", ".ogg", ".opus", ".wav", ".aac");
    private static final double FUZZY_THRESHOLD = 0.92;

    private final Path indexPath;
    private long lastModifiedMillis = -1;
    private List<Entry> entries = List.of();
    private Map<String, Entry> exactByKey = Map.of();
    private Map<String, Entry> exactByTokenKey = Map.of();

    public MusicDuplicateIndex(String indexPath) {
        this.indexPath = indexPath == null || indexPath.isBlank() ? null : Path.of(indexPath);
    }

    public boolean isEnabled() {
        return indexPath != null;
    }

    public Optional<DuplicateMatch> findDuplicate(String candidateName) {
        if (!isEnabled() || candidateName == null || candidateName.isBlank()) {
            return Optional.empty();
        }
        reloadIfNeeded();
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        String key = normalizeForMatch(candidateName);
        if (key.isBlank()) {
            return Optional.empty();
        }

        Entry exact = exactByKey.get(key);
        if (exact != null) {
            return Optional.of(new DuplicateMatch(exact.displayName(), exact.path(), 1.0, "exact"));
        }

        String tokenKey = tokenSortKey(key);
        Entry tokenExact = exactByTokenKey.get(tokenKey);
        if (tokenExact != null) {
            return Optional.of(new DuplicateMatch(tokenExact.displayName(), tokenExact.path(), 1.0, "token-exact"));
        }

        Entry partialTokenMatch = findPartialTokenMatch(key);
        if (partialTokenMatch != null) {
            return Optional.of(new DuplicateMatch(partialTokenMatch.displayName(), partialTokenMatch.path(), 1.0, "partial-token"));
        }

        Entry bestEntry = null;
        double bestScore = 0.0;
        for (Entry entry : entries) {
            double score = normalizedLevenshteinSimilarity(compact(key), compact(entry.key()));
            if (score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        if (bestEntry != null && bestScore >= FUZZY_THRESHOLD) {
            return Optional.of(new DuplicateMatch(bestEntry.displayName(), bestEntry.path(), bestScore, "fuzzy"));
        }
        return Optional.empty();
    }

    private Entry findPartialTokenMatch(String candidateKey) {
        Set<String> candidateTokens = tokenSet(candidateKey);
        if (candidateTokens.size() < 4) {
            return null;
        }
        Entry bestEntry = null;
        double bestScore = 0.0;
        for (Entry entry : entries) {
            Set<String> entryTokens = tokenSet(entry.key());
            if (entryTokens.size() < 4) {
                continue;
            }
            int overlap = 0;
            for (String token : candidateTokens) {
                if (entryTokens.contains(token)) {
                    overlap++;
                }
            }
            int smallerSize = Math.min(candidateTokens.size(), entryTokens.size());
            double score = overlap / (double) smallerSize;
            if (overlap >= 4 && score >= 0.8 && score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }
        return bestEntry;
    }

    public synchronized boolean addOrUpdateDownloadedFile(String displayName, Path filePath) {
        if (!isEnabled() || displayName == null || displayName.isBlank() || filePath == null) {
            return false;
        }

        String key = normalizeForMatch(displayName);
        if (key.isBlank()) {
            return false;
        }

        reloadIfNeeded();
        Path absolutePath = filePath.toAbsolutePath();
        Entry entry = new Entry(key, stripExtension(displayName), absolutePath.toString());
        boolean samePathAlreadyIndexed = entries.stream()
                .anyMatch(existing -> existing.key().equals(key) && existing.path().equals(entry.path()));
        if (samePathAlreadyIndexed) {
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
                    writer.write("match_key\tdisplay_name\tpath");
                    writer.newLine();
                }
                writer.write(escapeTsv(entry.key()));
                writer.write('\t');
                writer.write(escapeTsv(entry.displayName()));
                writer.write('\t');
                writer.write(escapeTsv(entry.path()));
                writer.newLine();
            }

            List<Entry> updatedEntries = new ArrayList<>(entries);
            updatedEntries.add(entry);
            Map<String, Entry> updatedByKey = new HashMap<>(exactByKey);
            Map<String, Entry> updatedByTokenKey = new HashMap<>(exactByTokenKey);
            updatedByKey.putIfAbsent(entry.key(), entry);
            updatedByTokenKey.putIfAbsent(tokenSortKey(entry.key()), entry);
            entries = List.copyOf(updatedEntries);
            exactByKey = Map.copyOf(updatedByKey);
            exactByTokenKey = Map.copyOf(updatedByTokenKey);
            lastModifiedMillis = Files.getLastModifiedTime(indexPath).toMillis();
            logger.info("Added downloaded file to duplicate music index: {} -> {}", entry.displayName(), entry.path());
            return true;
        } catch (Exception e) {
            logger.error("Failed to append downloaded file to duplicate music index: {}", indexPath, e);
            return false;
        }
    }

    public static void writeIndex(Path musicRoot, Path outputPath) throws IOException {
        List<Entry> scanned = scanMusicRoot(musicRoot);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("match_key\tdisplay_name\tpath");
            writer.newLine();
            for (Entry entry : scanned) {
                writer.write(escapeTsv(entry.key()));
                writer.write('\t');
                writer.write(escapeTsv(entry.displayName()));
                writer.write('\t');
                writer.write(escapeTsv(entry.path()));
                writer.newLine();
            }
        }
    }

    public static List<Entry> scanMusicRoot(Path musicRoot) throws IOException {
        if (musicRoot == null || !Files.isDirectory(musicRoot)) {
            throw new IOException("Music directory not found: " + musicRoot);
        }
        List<Entry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (Stream<Path> paths = Files.walk(musicRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(MusicDuplicateIndex::isAudioFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        String displayName = stripExtension(path.getFileName().toString());
                        String key = normalizeForMatch(displayName);
                        if (!key.isBlank() && seen.add(key + "\t" + path.toString())) {
                            result.add(new Entry(key, displayName, path.toAbsolutePath().toString()));
                        }
                    });
        }
        return result;
    }

    public static String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = FileNameSanitizer.sanitize(stripExtension(value));
        if (sanitized == null) {
            return "";
        }
        return sanitized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void reloadIfNeeded() {
        try {
            if (!Files.exists(indexPath)) {
                if (lastModifiedMillis != 0) {
                    logger.warn("Duplicate index file not found: {}", indexPath);
                }
                lastModifiedMillis = 0;
                entries = List.of();
                exactByKey = Map.of();
                exactByTokenKey = Map.of();
                return;
            }
            long currentModified = Files.getLastModifiedTime(indexPath).toMillis();
            if (currentModified == lastModifiedMillis) {
                return;
            }
            List<Entry> loaded = loadIndex(indexPath);
            Map<String, Entry> byKey = new HashMap<>();
            Map<String, Entry> byTokenKey = new HashMap<>();
            for (Entry entry : loaded) {
                byKey.putIfAbsent(entry.key(), entry);
                byTokenKey.putIfAbsent(tokenSortKey(entry.key()), entry);
            }
            entries = List.copyOf(loaded);
            exactByKey = Map.copyOf(byKey);
            exactByTokenKey = Map.copyOf(byTokenKey);
            lastModifiedMillis = currentModified;
            logger.info("Loaded duplicate music index: {} entries from {}", entries.size(), indexPath);
        } catch (Exception e) {
            logger.error("Failed to load duplicate music index: {}", indexPath, e);
            entries = List.of();
            exactByKey = Map.of();
            exactByTokenKey = Map.of();
        }
    }

    private static List<Entry> loadIndex(Path path) throws IOException {
        List<Entry> loaded = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("match_key\t")) {
                    continue;
                }
                String[] parts = line.split("\t", 3);
                if (parts.length < 2) {
                    continue;
                }
                String key = normalizeForMatch(parts[0]);
                String displayName = parts[1].trim();
                String filePath = parts.length == 3 ? parts[2].trim() : "";
                if (!key.isBlank()) {
                    loaded.add(new Entry(key, displayName.isBlank() ? parts[0].trim() : displayName, filePath));
                }
            }
        }
        return loaded;
    }

    private static boolean isAudioFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static String stripExtension(String value) {
        if (value == null) {
            return "";
        }
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        if (dot > slash && dot > 0) {
            String extension = value.substring(dot).toLowerCase(Locale.ROOT);
            if (AUDIO_EXTENSIONS.contains(extension) || ".mp3".equals(extension)) {
                return value.substring(0, dot);
            }
        }
        return value;
    }

    private static String tokenSortKey(String key) {
        return Stream.of(key.split("\\s+"))
                .filter(token -> !token.isBlank())
                .sorted()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static Set<String> tokenSet(String key) {
        Set<String> tokens = new HashSet<>();
        for (String token : key.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String compact(String key) {
        return key.replace(" ", "");
    }

    private static double normalizedLevenshteinSimilarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - (levenshteinDistance(left, right) / (double) maxLength);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] tmp = previous;
            previous = current;
            current = tmp;
        }
        return previous[right.length()];
    }

    private static String escapeTsv(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
    }

    public record Entry(String key, String displayName, String path) {
    }

    public record DuplicateMatch(String displayName, String path, double score, String matchType) {
    }
}
