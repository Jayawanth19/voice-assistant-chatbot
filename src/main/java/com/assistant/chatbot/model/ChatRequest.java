package com.assistant.chatbot.model;

/**
 * Incoming chat request from the frontend.
 * (No Lombok — plain Java getters/setters for maximum compatibility)
 */
public class ChatRequest {

    /** The user's text message (typed or transcribed from voice). */
    private String message;

    /** Session / conversation ID to maintain chat history. */
    private String sessionId;

    /** Whether the request originated from voice input. */
    private boolean voiceInput;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ChatRequest() {}

    public ChatRequest(String message, String sessionId, boolean voiceInput) {
        this.message    = message;
        this.sessionId  = sessionId;
        this.voiceInput = voiceInput;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getMessage()   { return message; }
    public String getSessionId() { return sessionId; }
    public boolean isVoiceInput(){ return voiceInput; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setMessage(String message)      { this.message    = message; }
    public void setSessionId(String sessionId)  { this.sessionId  = sessionId; }
    public void setVoiceInput(boolean v)        { this.voiceInput = v; }

    @Override
    public String toString() {
        return "ChatRequest{message='" + message + "', sessionId='" + sessionId
                + "', voiceInput=" + voiceInput + "}";
    }
}
