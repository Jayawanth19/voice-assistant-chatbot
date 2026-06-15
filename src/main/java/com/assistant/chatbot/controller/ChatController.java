package com.assistant.chatbot.controller;

import com.assistant.chatbot.model.ChatRequest;
import com.assistant.chatbot.model.ChatResponse;
import com.assistant.chatbot.service.AIService;
import com.assistant.chatbot.service.CommandService;
import com.assistant.chatbot.service.SpeechService;
import com.assistant.chatbot.service.WeatherService;
import com.assistant.chatbot.utils.AppUtils;
import com.assistant.chatbot.utils.VoiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ChatController — handles all HTTP requests.
 *
 * Routes:
 *   GET  /           → renders index.html (Thymeleaf)
 *   POST /api/chat   → main chat endpoint (JSON)
 *   POST /api/clear  → clear conversation history
 *   GET  /api/health → health check
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AIService       aiService;
    private final CommandService  commandService;
    private final WeatherService  weatherService;
    private final SpeechService   speechService;

    public ChatController(AIService ai, CommandService cmd,
                          WeatherService weather, SpeechService speech) {
        this.aiService      = ai;
        this.commandService = cmd;
        this.weatherService = weather;
        this.speechService  = speech;
    }

    // ── Page ──────────────────────────────────────────────────────────────────

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("greeting", AppUtils.timeOfDayGreeting());
        model.addAttribute("sessionId", AppUtils.newSessionId());
        return "index";
    }

    // ── Chat API ──────────────────────────────────────────────────────────────

    /**
     * Main chat endpoint.
     *
     * Decision priority:
     *   1. Built-in commands (time, date, joke, help, greetings)
     *   2. Weather queries
     *   3. AI (Groq / OpenAI)
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        String input     = VoiceUtils.cleanTranscript(AppUtils.safe(req.getMessage()));
        String sessionId = AppUtils.safe(req.getSessionId());

        if (input.isBlank()) {
            return ResponseEntity.badRequest().body(ChatResponse.error("Please say or type something."));
        }

        log.info("[{}] User ({}): {}", sessionId, req.isVoiceInput() ? "voice" : "text", input);

        ChatResponse response;

        // 1 — local commands
        if (commandService.isCommand(input)) {
            response = commandService.handle(input);

        // 2 — weather
        } else if (weatherService.isWeatherQuery(input)) {
            String text = weatherService.getWeatherResponse(input);
            String city = extractCityFromInput(input);
            Map<String, Object> data = city != null ? weatherService.fetchWeatherData(city) : Map.of();
            response = ChatResponse.withData(text, "weather", data);

        // 3 — AI
        } else {
            String aiReply = aiService.chat(sessionId, input);
            response = ChatResponse.text(aiReply);
        }

        log.info("[{}] Assistant: {}", sessionId, AppUtils.truncate(response.getMessage(), 120));

        // Optional server-side TTS
        if (response.isSpeakResponse()) {
            speechService.speak(VoiceUtils.prepareForSpeech(response.getMessage()));
        }

        return ResponseEntity.ok(response);
    }

    // ── Utility endpoints ─────────────────────────────────────────────────────

    @PostMapping("/api/clear")
    @ResponseBody
    public ResponseEntity<Map<String, String>> clearHistory(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "");
        aiService.clearHistory(sessionId);
        log.info("History cleared for session: {}", sessionId);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Chat history cleared."));
    }

    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "service", "Voice Assistant Chatbot",
            "tts",     speechService.isEnabled() ? "server-side" : "browser"
        ));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String extractCityFromInput(String input) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:weather|temperature|temp)\\s+(?:in|for|at)\\s+([a-zA-Z\\s]+)",
                         java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(input);
        return m.find() ? m.group(1).trim() : null;
    }
}
