package com.example.mkconsumer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static final Properties props = new Properties();

    static {
        // Load config.properties from working directory if present, otherwise from classpath
        Path p = Path.of("config.properties");
        try (InputStream is = Files.exists(p) ? Files.newInputStream(p) : Config.class.getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {
        }
    }

    private Config() {}

    public static String getDefaultQueueUrl() {
        String v = props.getProperty("QUEUE_URL");
        if (v != null && !v.isBlank()) return v.trim();
        // fall back to env var
        String env = System.getenv("QUEUE_URL");
        return env != null && !env.isBlank() ? env : null;
    }

    public static String getDefaultRegion() {
        String v = props.getProperty("REGION");
        if (v != null && !v.isBlank()) return v.trim();
        String env = System.getenv("AWS_REGION");
        return env != null && !env.isBlank() ? env : "us-east-1";
    }

    public static int getPollIntervalSeconds() {
        String v = props.getProperty("POLL_INTERVAL_SECONDS");
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 20;
    }

    public static int getMaxMessages() {
        String v = props.getProperty("MAX_MESSAGES");
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 10;
    }

    public static int getWaitTimeSeconds() {
        String v = props.getProperty("WAIT_TIME_SECONDS");
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 20;
    }
}
