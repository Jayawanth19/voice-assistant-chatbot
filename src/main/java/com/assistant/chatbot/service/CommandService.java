package com.assistant.chatbot.service;

import com.assistant.chatbot.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * CommandService — detects and handles built-in local commands so they don't
 * need to hit the AI API at all, giving instant responses.
 *
 * Commands are matched case-insensitively by prefix keywords.
 *
 * Add new commands to the COMMAND_KEYWORDS list and the handle() switch below.
 */
@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    // Keywords that signal a local command (lowercase)
    private static final List<String> TIME_WORDS    = List.of("time", "what time", "current time");
    private static final List<String> DATE_WORDS    = List.of("date", "today's date", "what day", "what's the date");
    private static final List<String> JOKE_WORDS    = List.of("tell me a joke", "say a joke", "joke");
    private static final List<String> CLEAR_WORDS   = List.of("clear chat", "clear history", "reset chat", "new chat");
    private static final List<String> HELP_WORDS    = List.of("help", "what can you do", "commands");
    private static final List<String> GREET_WORDS   = List.of("hello", "hi", "hey", "good morning", "good afternoon", "good evening");

    private final List<String> jokes = List.of(
        "Why don't scientists trust atoms? Because they make up everything!",
        "I told my computer I needed a break. Now it won't stop sending me Kit-Kat ads.",
        "Why do programmers prefer dark mode? Because light attracts bugs!",
        "How many programmers does it take to change a light bulb? None — that's a hardware problem.",
        "Why did the Java developer wear glasses? Because he couldn't C#!"
    );

    private int jokeIndex = 0;

    /**
     * Check whether the input is a built-in command.
     *
     * @param input user's raw message
     * @return true if this service can handle it
     */
    public boolean isCommand(String input) {
        String lower = input.toLowerCase(Locale.ROOT).trim();
        return matchesAny(lower, TIME_WORDS)
            || matchesAny(lower, DATE_WORDS)
            || matchesAny(lower, JOKE_WORDS)
            || matchesAny(lower, CLEAR_WORDS)
            || matchesAny(lower, HELP_WORDS)
            || matchesAny(lower, GREET_WORDS);
    }

    /**
     * Execute the matched command and return a ChatResponse.
     *
     * @param input user's raw message
     * @return ChatResponse with the result
     */
    public ChatResponse handle(String input) {
        String lower = input.toLowerCase(Locale.ROOT).trim();

        if (matchesAny(lower, TIME_WORDS)) {
            return handleTime();
        } else if (matchesAny(lower, DATE_WORDS)) {
            return handleDate();
        } else if (matchesAny(lower, JOKE_WORDS)) {
            return handleJoke();
        } else if (matchesAny(lower, CLEAR_WORDS)) {
            return handleClear();
        } else if (matchesAny(lower, HELP_WORDS)) {
            return handleHelp();
        } else if (matchesAny(lower, GREET_WORDS)) {
            return handleGreeting();
        }

        return ChatResponse.error("Command not recognised.");
    }

    // ── command handlers ──────────────────────────────────────────────────────

    private ChatResponse handleTime() {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("hh:mm a"));
        return ChatResponse.builder()
                .message("The current time is " + time + ".")
                .type("command")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private ChatResponse handleDate() {
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        return ChatResponse.builder()
                .message("Today is " + date + ".")
                .type("command")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private ChatResponse handleJoke() {
        String joke = jokes.get(jokeIndex % jokes.size());
        jokeIndex++;
        return ChatResponse.builder()
                .message(joke)
                .type("command")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private ChatResponse handleClear() {
        return ChatResponse.builder()
                .message("__CLEAR__") // frontend watches for this sentinel
                .type("clear")
                .speakResponse(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private ChatResponse handleHelp() {
        String help = "Here's what I can do:\n" +
                "• Ask me anything — I'll use AI to answer\n" +
                "• Say 'What time is it?' or 'What's the date?'\n" +
                "• Say 'Tell me a joke'\n" +
                "• Ask for the weather: 'Weather in Chennai'\n" +
                "• Say 'Clear chat' to start fresh\n" +
                "• Use the microphone button to speak to me";
        return ChatResponse.builder()
                .message(help)
                .type("command")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private ChatResponse handleGreeting() {
        int hour = LocalDateTime.now().getHour();
        String greeting = hour < 12 ? "Good morning!" : hour < 17 ? "Good afternoon!" : "Good evening!";
        return ChatResponse.builder()
                .message(greeting + " How can I help you today?")
                .type("command")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean matchesAny(String input, List<String> keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) return true;
        }
        return false;
    }
}
