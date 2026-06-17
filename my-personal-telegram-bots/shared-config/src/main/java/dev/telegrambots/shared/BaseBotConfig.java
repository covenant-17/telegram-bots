package dev.telegrambots.shared;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Base configuration class for Telegram bots.
 * Provides common configuration properties and loading logic.
 *
 * @author Telegram Bots Team
 * @version 1.0
 * @since 2025-08-29
 */
public abstract class BaseBotConfig {

    /** Maximum file size in bytes */
    public final long maxFileSize;

    /** Maximum duration in minutes */
    public final double maxDurationMinutes;

    /** Configuration resource bundle */
    protected final ResourceBundle config;

    /**
     * Constructor that loads configuration from config.properties.
     * Subclasses should call super() and then load their specific properties.
     */
    protected BaseBotConfig() {
        this.config = loadConfig();
        
        // Common file limits
        this.maxFileSize = getLongProperty("max.filesize", 50 * 1024 * 1024L);
        this.maxDurationMinutes = getDoubleProperty("max.duration", 10.0);
        
        // NOTE: Validation is NOT called here - subclasses should call validateConfiguration()
        // after initializing their fields
    }

    public static ResourceBundle loadConfig() {
        for (Path externalConfig : configCandidates()) {
            if (Files.isRegularFile(externalConfig)) {
                try (FileInputStream input = new FileInputStream(externalConfig.toFile())) {
                    return new PropertyResourceBundle(input);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to load " + externalConfig, e);
                }
            }
        }

        return ResourceBundle.getBundle("config");
    }

    private static java.util.List<Path> configCandidates() {
        java.util.List<Path> candidates = new java.util.ArrayList<>();
        addIfPresent(candidates, System.getProperty("bot.config.path"));
        addIfPresent(candidates, System.getenv("BOT_CONFIG_PATH"));
        candidates.add(Path.of("config.properties"));

        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!entry.isBlank()) {
                Path classPathEntry = Path.of(entry);
                Path parent = Files.isDirectory(classPathEntry) ? classPathEntry : classPathEntry.getParent();
                if (parent != null) {
                    candidates.add(parent.resolve("config.properties"));
                }
            }
        }

        return candidates;
    }

    private static void addIfPresent(java.util.List<Path> candidates, String path) {
        if (path != null && !path.isBlank()) {
            candidates.add(Path.of(path));
        }
    }

    /**
     * Validates that required configuration properties are present.
     * Subclasses can override this method to add their own validation.
     */
    protected void validateConfiguration() {
        // Base validation - subclasses should call super.validateConfiguration()
        // and add their specific validations
    }

    /**
     * Gets a string property from configuration with optional default value.
     *
     * @param key Configuration property key
     * @param defaultValue Default value if property not found
     * @return Property value or default
     */
    protected String getStringProperty(String key, String defaultValue) {
        return config.containsKey(key) ? config.getString(key) : defaultValue;
    }

    /**
     * Gets a long property from configuration with default value.
     *
     * @param key Configuration property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value or default
     */
    protected long getLongProperty(String key, long defaultValue) {
        if (config.containsKey(key)) {
            try {
                return Long.parseLong(config.getString(key));
            } catch (NumberFormatException e) {
                System.err.println("Invalid long value for property '" + key + "', using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Gets a double property from configuration with default value.
     *
     * @param key Configuration property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value or default
     */
    protected double getDoubleProperty(String key, double defaultValue) {
        if (config.containsKey(key)) {
            try {
                return Double.parseDouble(config.getString(key));
            } catch (NumberFormatException e) {
                System.err.println("Invalid double value for property '" + key + "', using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Gets an integer property from configuration with default value.
     *
     * @param key Configuration property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value or default
     */
    protected int getIntProperty(String key, int defaultValue) {
        if (config.containsKey(key)) {
            try {
                return Integer.parseInt(config.getString(key));
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for property '" + key + "', using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Resolves tool path with multiple fallback strategies.
     * Priority: config property -> environment variable -> default command
     *
     * @param configKey Configuration property key
     * @param envVar Environment variable name
     * @param defaultCmd Default command name
     * @return Resolved path or command
     */
    protected String resolvePath(String configKey, String envVar, String defaultCmd) {
        // First priority: explicit config property
        String configValue = getStringProperty(configKey, "");
        if (!configValue.isEmpty()) {
            return configValue;
        }

        // Second priority: environment variable
        String envPath = System.getenv(envVar);
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // Third priority: default command (assumes it's in PATH)
        return defaultCmd;
    }
}
