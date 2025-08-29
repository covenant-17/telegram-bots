package dev.telegrambots.youtubemp3downloader;

import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;

public class FileNameSanitizer {
    /**
     * Map<Pattern, String> — правила очистки имени файла.
     * Ключ: регулярное выражение для поиска мусора/шаблона.
     * Значение: на что заменить найденное (обычно пустая строка или пробел).
     *
     * Правила:
     * - Удаляют мусорные теги (z2.fm, official, lyric, kbps и т.д.)
     * - Убирают спецсимволы (#, &, ;, кавычки, скобки, запятые)
     * - Приводят к красивому виду (убирают двойные пробелы, подчёркивания, дефисы)
     * - Заменяют html- и unicode-апострофы на обычный '
     * - Оставляют только полезную информацию (исполнитель, название)
     */
    private static final Map<Pattern, String> rules = new LinkedHashMap<>();
    static {
        rules.put(Pattern.compile("\\(z2.fm\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("\\(official (video|audio)\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("\\(320 kbps\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("#039;"), "'");
        rules.put(Pattern.compile("&"), " ");
        rules.put(Pattern.compile(";"), "");
        rules.put(Pattern.compile("#"), "");
        rules.put(Pattern.compile("\""), "");
        rules.put(Pattern.compile("_+"), " ");
        rules.put(Pattern.compile("-{2,}"), " ");
        rules.put(Pattern.compile("-"), " ");
        rules.put(Pattern.compile("([\\[].+[\\]])"), "");
        rules.put(Pattern.compile("(\\s')|('\\s)|(\\s'\\s)"), "");
        rules.put(Pattern.compile("(&#39)|(#39)|(39;)|(39)"), "'");
        rules.put(Pattern.compile("\\(Official Music Video\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("\\(lyric video\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("official", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("music", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("video", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("lyrics?", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("clip officiel", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("clip", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("премьера песни", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("dark techno ebm industrial type", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("topic", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("2025", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("\\(", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile("\\)", Pattern.CASE_INSENSITIVE), "");
        rules.put(Pattern.compile(","), "");
        rules.put(Pattern.compile("/"), " ");
        // URL-encoded characters - декодируем основные
        rules.put(Pattern.compile("%20"), " "); // Decode space
        rules.put(Pattern.compile("%[0-9A-Fa-f]{2}"), ""); // Remove other encoded
        // Special characters and symbols that need cleaning
        rules.put(Pattern.compile("\\*"), "");
        rules.put(Pattern.compile("\\?"), " ");
        rules.put(Pattern.compile("\\\\"), " ");
        rules.put(Pattern.compile("\\|"), " ");
        rules.put(Pattern.compile("!+"), "");
        rules.put(Pattern.compile("\\.{2,}"), "");
        rules.put(Pattern.compile("\\?{2,}"), " ");
        rules.put(Pattern.compile("\\p{Cntrl}"), " "); // Заменяем управляющие символы на пробел
    }

    public static String sanitize(String fileName) {
        if (fileName == null)
            return null;
        String result = fileName.trim().toLowerCase();
        for (Map.Entry<Pattern, String> entry : rules.entrySet()) {
            result = entry.getKey().matcher(result).replaceAll(entry.getValue());
        }
        result = result.replaceAll("mp3$", ""); // убираем mp3 без точки в конце
        result = result.replaceAll("\\.mp3$", ""); // убираем .mp3 в конце
        result = result.replaceAll("\\s+", " ").trim();
        result = result.replaceAll("\\s*'\\s*", "'");
        result = result.replaceAll("\\s*\\.+\\s*$", ""); // убираем точки в конце строки
        result = capitalizeWords(result);
        return result;
    }

    /**
     * Делает каждое слово с большой буквы, остальные буквы маленькие
     */
    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty())
            return input;
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1)
                    sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Формирует итоговое имя файла mp3.
     * Если title уже содержит channel (или channel пустой), возвращает только
     * title.
     * Иначе возвращает "channel - title".
     */
    public static String composeFileName(String channel, String title) {
        if (title == null)
            return channel;
        if (channel == null || channel.isBlank())
            return title;
        if (title.toLowerCase().contains(channel.toLowerCase())) {
            return title;
        } else {
            return channel + " - " + title;
        }
    }

    /**
     * Массовое переименование файлов в папке по правилам санитайзера.
     * Переименовывает все файлы (по умолчанию только .mp3) в указанной директории.
     * 
     * @param dirPath   путь к папке
     * @param extension фильтр по расширению (например, ".mp3"), если null — все
     *                  файлы
     * @param dryRun    если true — только выводит, что будет переименовано, без
     *                  изменений
     */
    public static void sanitizeAllInDirectory(String dirPath, String extension, boolean dryRun) {
        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            System.out.println("[SUMMARY] Directory not found: " + dirPath);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println("[SUMMARY] Directory is empty: " + dirPath);
            return;
        }
        int affected = 0;
        int changed = 0;
        int unchanged = 0;
        int total = 0;
        if (dryRun) {
            System.out.println("\n==================== [DRY RUN START] ======================");
        } else {
            System.out.println("\n====================== [RENAME START] ======================");
        }
        for (File file : files) {
            if (file.isFile() && (extension == null || file.getName().toLowerCase().endsWith(extension))) {
                total++;
                String oldName = file.getName();
                String ext = "";
                int dot = oldName.lastIndexOf('.');
                if (dot > 0) {
                    ext = oldName.substring(dot);
                    oldName = oldName.substring(0, dot);
                }
                String newName = sanitize(oldName) + ext;
                if (!newName.equals(file.getName())) {
                    affected++;
                    File newFile = new File(dir, newName);
                    if (dryRun) {
                        System.out.println("[DRY RUN] Would rename: " + file.getName() + " -> " + newName);
                    } else {
                        boolean ok = file.renameTo(newFile);
                        System.out.println((ok ? "Renamed: " : "Failed: ") + file.getName() + " -> " + newName);
                        if (ok)
                            changed++;
                    }
                } else {
                    unchanged++;
                    System.out.println("[OK] Already clean: " + file.getName());
                }
            }
        }
        if (dryRun) {
            System.out.println("===================== [DRY RUN END] =======================");
            System.out.println("\n==================== [DRY RUN SUMMARY] ====================");
            System.out.println("Total files checked: " + total);
            System.out.println("Files to be renamed: " + affected);
            System.out.println("Files already clean: " + unchanged);
            if (affected == 0) {
                System.out.println("✅ No files to rename in " + dirPath);
            }
            System.out.println("==========================================================\n");
        } else {
            System.out.println("====================== [RENAME END] =======================");
            System.out.println("\n======================== [SUMMARY] ========================");
            System.out.println("Total files checked: " + total);
            System.out.println("Files renamed: " + changed + " out of " + affected);
            System.out.println("Files already clean: " + unchanged);
            if (affected == 0) {
                System.out.println("✅ No files to rename in " + dirPath);
            }
            System.out.println("==========================================================\n");
        }
    }
}
