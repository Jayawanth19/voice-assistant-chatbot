package com.assistant.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIService — sends user messages to the Groq (or OpenAI-compatible) Chat
 * Completions API and returns the assistant's reply.
 *
 * Groq is free-tier friendly and extremely fast. Swap the base URL and model
 * to use OpenAI, Ollama, or any OpenAI-compatible endpoint.
 *
 * Config keys (application.properties):
 *   app.ai.api-key     — your Groq API key (https://console.groq.com)
 *   app.ai.base-url    — defaults to Groq; change to https://api.openai.com/v1 for OpenAI
 *   app.ai.model       — e.g. llama3-8b-8192 (Groq) or gpt-4o-mini (OpenAI)
 *   app.ai.system-prompt — optional persona for the assistant
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_HISTORY = 20; // messages kept per session

    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    // Simple in-memory conversation history keyed by sessionId
    private final Map<String, List<Map<String, String>>> history = new ConcurrentHashMap<>();

    @Value("${app.ai.api-key:REPLACE_WITH_YOUR_GROQ_KEY}")
    private String apiKey;

    @Value("${app.ai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${app.ai.model:llama3-8b-8192}")
    private String model;

    @Value("${app.ai.system-prompt:You are a helpful, friendly voice assistant. Keep answers concise and clear.}")
    private String systemPrompt;

    public AIService(OkHttpClient http) {
        this.http = http;
    }

    /**
     * Send a user message and return the AI's reply.
     *
     * @param sessionId unique conversation identifier
     * @param userMessage the user's input text
     * @return assistant reply string
     */
    public String chat(String sessionId, String userMessage) {
        // Build/extend conversation history
        List<Map<String, String>> messages = history.computeIfAbsent(sessionId, k -> new ArrayList<>());
        messages.add(Map.of("role", "user", "content", userMessage));

        // Keep history bounded
        if (messages.size() > MAX_HISTORY) {
            messages.subList(0, messages.size() - MAX_HISTORY).clear();
        }

        try {
            String requestBody = buildRequestBody(messages);
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI API error: {} {}", response.code(), response.message());
                    return "Sorry, I couldn't reach the AI service right now. (HTTP " + response.code() + ")";
                }
                String body = response.body().string();
                String reply = parseReply(body);

                // Store assistant reply in history
                messages.add(Map.of("role", "assistant", "content", reply));
                return reply;
            }
        } catch (Exception e) {
            log.error("AIService error: {}", e.getMessage(), e);
            return "I encountered an error processing your request. Please try again.";
        }
    }

    /** Clear conversation history for a session (e.g. on "clear chat"). */
    public void clearHistory(String sessionId) {
        history.remove(sessionId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String buildRequestBody(List<Map<String, String>> conversationHistory) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 512);
        root.put("temperature", 0.7);

        ArrayNode msgs = root.putArray("messages");

        // System message first
        ObjectNode sys = msgs.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);

        // Then conversation history
        for (Map<String, String> msg : conversationHistory) {
            ObjectNode m = msgs.addObject();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
        }

        return mapper.writeValueAsString(root);
    }

    private String parseReply(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        return root.path("choices").get(0)
                   .path("message")
                   .path("content")
                   .asText("No response received.");
    }
}
