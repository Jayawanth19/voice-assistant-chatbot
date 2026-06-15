package com.assistant.chatbot.model;

/**
 * Outgoing chat response sent back to the frontend.
 * (No Lombok — plain Java builder pattern for maximum compatibility)
 */
public class ChatResponse {

    private String  message;
    private String  type;
    private Object  data;
    private boolean speakResponse;
    private long    timestamp;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ChatResponse() {}

    public ChatResponse(String message, String type, Object data,
                        boolean speakResponse, long timestamp) {
        this.message       = message;
        this.type          = type;
        this.data          = data;
        this.speakResponse = speakResponse;
        this.timestamp     = timestamp;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getMessage()      { return message; }
    public String  getType()         { return type; }
    public Object  getData()         { return data; }
    public boolean isSpeakResponse() { return speakResponse; }
    public long    getTimestamp()    { return timestamp; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setMessage(String message)          { this.message       = message; }
    public void setType(String type)                { this.type          = type; }
    public void setData(Object data)                { this.data          = data; }
    public void setSpeakResponse(boolean speak)     { this.speakResponse = speak; }
    public void setTimestamp(long timestamp)        { this.timestamp     = timestamp; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String  message;
        private String  type;
        private Object  data;
        private boolean speakResponse = true;
        private long    timestamp     = System.currentTimeMillis();

        public Builder message(String message)          { this.message       = message; return this; }
        public Builder type(String type)                { this.type          = type;    return this; }
        public Builder data(Object data)                { this.data          = data;    return this; }
        public Builder speakResponse(boolean speak)     { this.speakResponse = speak;  return this; }
        public Builder timestamp(long timestamp)        { this.timestamp     = timestamp; return this; }

        public ChatResponse build() {
            return new ChatResponse(message, type, data, speakResponse, timestamp);
        }
    }

    // ── Convenience factories ─────────────────────────────────────────────────

    public static ChatResponse text(String message) {
        return builder()
                .message(message)
                .type("text")
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ChatResponse error(String message) {
        return builder()
                .message(message)
                .type("error")
                .speakResponse(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ChatResponse withData(String message, String type, Object data) {
        return builder()
                .message(message)
                .type(type)
                .data(data)
                .speakResponse(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public String toString() {
        return "ChatResponse{type='" + type + "', message='" + message + "'}";
    }
}
