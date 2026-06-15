package com.assistant.chatbot.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * AppUtils — general-purpose application helpers.
 */
@Component
public class AppUtils {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("hh:mm a");

    // ── Session helpers ───────────────────────────────────────────────────────

    /**
     * Generate a new random session ID for a fresh conversation.
     */
    public static String newSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // ── Time helpers ──────────────────────────────────────────────────────────

    /**
     * Format an epoch-millisecond timestamp for the chat UI (e.g. "02:35 PM").
     */
    public static String formatTimestamp(long epochMs) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        return dt.format(DISPLAY_FMT);
    }

    /**
     * Return a greeting appropriate to the current time of day.
     */
    public static String timeOfDayGreeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    // ── String helpers ────────────────────────────────────────────────────────

    /**
     * Truncate a string to maxLen characters, appending "..." if trimmed.
     */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Safely null-check and trim a string; returns empty string if null.
     */
    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
