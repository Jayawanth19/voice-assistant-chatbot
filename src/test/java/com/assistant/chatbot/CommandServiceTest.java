package com.assistant.chatbot;

import com.assistant.chatbot.service.CommandService;
import com.assistant.chatbot.model.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandServiceTest {

    private CommandService service;

    @BeforeEach
    void setUp() { service = new CommandService(); }

    @Test void recognisesTimeQuery()  { assertThat(service.isCommand("what time is it")).isTrue(); }
    @Test void recognisesDateQuery()  { assertThat(service.isCommand("what's the date")).isTrue(); }
    @Test void recognisesJoke()       { assertThat(service.isCommand("tell me a joke")).isTrue(); }
    @Test void recognisesGreeting()   { assertThat(service.isCommand("hello")).isTrue(); }
    @Test void ignoresAIQuestion()    { assertThat(service.isCommand("explain quantum computing")).isFalse(); }

    @Test void jokeResponseNotBlank() {
        ChatResponse r = service.handle("tell me a joke");
        assertThat(r.getMessage()).isNotBlank();
        assertThat(r.getType()).isEqualTo("command");
    }
}
