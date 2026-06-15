package com.assistant.chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.ai.api-key=test-key",
    "app.weather.api-key=test-key",
    "app.speech.enabled=false"
})
class VoiceAssistantApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts without errors.
    }
}
