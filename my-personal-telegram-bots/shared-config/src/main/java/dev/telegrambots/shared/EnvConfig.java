package dev.telegrambots.shared;

import java.util.Optional;
import java.util.OptionalLong;

public final class EnvConfig {
    private EnvConfig() {
    }

    public static String getRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return value;
    }

    public static Optional<String> getOptional(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static long getRequiredLong(String key) {
        String value = getRequired(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid long for env var " + key + ": " + value, e);
        }
    }

    public static OptionalLong getOptionalLong(String key) {
        Optional<String> value = getOptional(key);
        if (value.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(value.get()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid long for env var " + key + ": " + value.get(), e);
        }
    }
}
