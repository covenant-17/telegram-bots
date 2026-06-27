package dev.telegrambots.youtubemp3downloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChapterTrackPlanner {
    private ChapterTrackPlanner() {
    }

    static List<ChapterTrackPlan> build(YoutubeVideoMetadata metadata) {
        Map<String, Integer> seen = new HashMap<>();
        List<ChapterTrackPlan> plans = new ArrayList<>();
        String sanitizedChannel = cleanFileBaseName(FileNameSanitizer.sanitize(metadata.channel()));
        for (int i = 0; i < metadata.chapters().size(); i++) {
            YoutubeChapter chapter = metadata.chapters().get(i);
            String sanitizedChapter = cleanFileBaseName(FileNameSanitizer.sanitize(chapter.title()));
            String composed = (sanitizedChannel != null && !sanitizedChannel.isBlank())
                    ? FileNameSanitizer.composeFileName(sanitizedChannel, sanitizedChapter)
                    : sanitizedChapter;
            String baseName = cleanFileBaseName(FileNameSanitizer.sanitize(composed));
            if (shouldPreserveChannelSeparator(sanitizedChannel, sanitizedChapter)) {
                baseName = cleanFileBaseName(sanitizedChannel) + " - " + cleanFileBaseName(sanitizedChapter);
            }
            if (baseName == null || baseName.isBlank()) {
                baseName = "track-" + (i + 1);
            }
            String uniqueBaseName = uniqueChapterBaseName(baseName, seen);
            plans.add(new ChapterTrackPlan(chapter, uniqueBaseName, uniqueBaseName + ".mp3"));
        }
        return List.copyOf(plans);
    }

    private static boolean shouldPreserveChannelSeparator(String sanitizedChannel, String sanitizedChapter) {
        return sanitizedChannel != null
                && !sanitizedChannel.isBlank()
                && sanitizedChapter != null
                && !sanitizedChapter.isBlank()
                && !sanitizedChapter.toLowerCase(java.util.Locale.ROOT)
                        .contains(sanitizedChannel.toLowerCase(java.util.Locale.ROOT));
    }

    private static String cleanFileBaseName(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replaceAll("[\\\\/:*?\"<>|]+", " ")
                .replaceAll("[^\\p{L}\\p{N} .()\\-']+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            return cleaned;
        }
        StringBuilder result = new StringBuilder();
        for (String word : cleaned.split(" ")) {
            if (word.isBlank()) {
                continue;
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
            result.append(' ');
        }
        return result.toString().trim();
    }

    private static String uniqueChapterBaseName(String baseName, Map<String, Integer> seen) {
        String key = MusicDuplicateIndex.normalizeForMatch(baseName);
        int count = seen.getOrDefault(key, 0) + 1;
        seen.put(key, count);
        return count == 1 ? baseName : baseName + " (" + count + ")";
    }
}
