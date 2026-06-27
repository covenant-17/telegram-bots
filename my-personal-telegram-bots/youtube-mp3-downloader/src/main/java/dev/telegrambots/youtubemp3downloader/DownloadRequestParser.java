package dev.telegrambots.youtubemp3downloader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DownloadRequestParser {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)[A-Za-z0-9_-]{11}(?:[^\\s]*)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^\\s+(\\d+(?::\\d{1,2}){0,2}(?:\\.\\d+)?)\\s+(\\d+(?::\\d{1,2}){0,2}(?:\\.\\d+)?)\\s*$"
    );

    private DownloadRequestParser() {
    }

    public static List<DownloadRequest> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Map<String, DownloadRequest> unique = new LinkedHashMap<>();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            parseLine(line, unique);
        }
        return new ArrayList<>(unique.values());
    }

    private static void parseLine(String line, Map<String, DownloadRequest> unique) {
        Matcher matcher = URL_PATTERN.matcher(line);
        List<Match> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new Match(matcher.group(), matcher.start(), matcher.end()));
        }
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            int tailEnd = i + 1 < matches.size() ? matches.get(i + 1).start : line.length();
            String tail = line.substring(match.end, tailEnd);
            AudioClipRange range = parseRange(tail);
            String key = match.url + "|" + (range == null ? "" : range.formatLabel());
            unique.putIfAbsent(key, new DownloadRequest(match.url, range));
        }
    }

    private static AudioClipRange parseRange(String tail) {
        Matcher matcher = RANGE_PATTERN.matcher(tail);
        if (!matcher.matches()) {
            return null;
        }
        try {
            double start = AudioClipRange.parseTimeSeconds(matcher.group(1));
            double end = AudioClipRange.parseTimeSeconds(matcher.group(2));
            return new AudioClipRange(start, end);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record Match(String url, int start, int end) {
    }
}
