package com.assistant.chatbot.utils;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VoiceUtils — static-style helpers for voice and speech processing.
 */
@Component
public class VoiceUtils {

    private static final Pattern SPECIAL_CHARS  = Pattern.compile("[^a-zA-Z0-9\\s.,?!'\"\\-]");
    private static final Pattern MULTI_SPACE     = Pattern.compile("\\s{2,}");
    private static final Pattern SENTENCE_END    = Pattern.compile("[.!?]\\s*$");

    // ── Text sanitisation ─────────────────────────────────────────────────────

    /**
     * Clean raw transcript text from the Web Speech API before sending to AI.
     * Trims, normalises whitespace, and ensures terminal punctuation.
     */
    public static String cleanTranscript(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        // Capitalise first letter
        if (!s.isEmpty()) {
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        // Add period if no terminal punctuation
        if (!s.isEmpty() && !SENTENCE_END.matcher(s).find()) {
            s += ".";
        }
        return s;
    }

    /**
     * Prepare AI response text for TTS reading.
     * Removes markdown, URLs, and other constructs that sound odd when spoken.
     */
    public static String prepareForSpeech(String text) {
        if (text == null) return "";
        return text
            .replaceAll("```[\\s\\S]*?```", "code block omitted")   // code blocks
            .replaceAll("`[^`]+`", "")                               // inline code
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")                  // bold
            .replaceAll("\\*(.*?)\\*", "$1")                         // italic
            .replaceAll("https?://\\S+", "link")                     // URLs
            .replaceAll("#+ ", "")                                   // headings
            .replaceAll("[-•*] ", "")                                // bullet points
            .replaceAll("\\n+", " ")                                 // newlines → space
            .replaceAll("\\s{2,}", " ")
            .trim();
    }

    // ── Intent helpers ────────────────────────────────────────────────────────

    /**
     * Rough intent label for logging/analytics. Not used for routing.
     */
    public static String detectIntent(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.contains("weather") || lower.contains("temperature")) return "weather";
        if (lower.contains("time") || lower.contains("date"))           return "datetime";
        if (lower.contains("joke") || lower.contains("funny"))          return "joke";
        if (lower.contains("hello") || lower.contains("hi"))            return "greeting";
        if (lower.contains("clear") || lower.contains("reset"))         return "clear";
        if (lower.contains("help") || lower.contains("commands"))       return "help";
        return "general";
    }

    /**
     * Estimate spoken duration in seconds (average 150 words per minute).
     */
    public static int estimateSpeakDuration(String text) {
        if (text == null || text.isBlank()) return 0;
        int wordCount = text.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(wordCount / 2.5));
    }
}
