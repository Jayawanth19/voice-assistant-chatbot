package com.assistant.chatbot.service;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * SpeechService — server-side Text-to-Speech using FreeTTS.
 *
 * NOTE: For a browser-based app the Web Speech API (browser TTS) is much
 * simpler and is handled entirely in script.js. This service exists for
 * desktop/server-side TTS use-cases (e.g. a Raspberry Pi voice assistant
 * that speaks out loud from the server).
 *
 * Set app.speech.enabled=true in application.properties to activate it.
 */
@Service
public class SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);
    private static final String VOICE_NAME = "kevin16"; // FreeTTS built-in voice

    @Value("${app.speech.enabled:false}")
    private boolean enabled;

    @Value("${app.speech.rate:150}")
    private float speechRate;

    @Value("${app.speech.volume:1.0}")
    private float volume;

    private Voice voice;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Server-side TTS disabled. Browser Web Speech API will be used instead.");
            return;
        }
        try {
            System.setProperty("freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager vm = VoiceManager.getInstance();
            voice = vm.getVoice(VOICE_NAME);
            if (voice != null) {
                voice.allocate();
                voice.setRate(speechRate);
                voice.setVolume(volume);
                log.info("FreeTTS voice '{}' initialised successfully.", VOICE_NAME);
            } else {
                log.warn("FreeTTS voice '{}' not found.", VOICE_NAME);
            }
        } catch (Exception e) {
            log.error("Failed to initialise FreeTTS: {}", e.getMessage());
        }
    }

    /**
     * Speak the given text aloud (server-side audio output).
     *
     * @param text text to speak
     */
    public void speak(String text) {
        if (!enabled || voice == null) return;
        try {
            // Strip markdown-style formatting before speaking
            String clean = text.replaceAll("[*_`#]", "").trim();
            voice.speak(clean);
        } catch (Exception e) {
            log.error("TTS error: {}", e.getMessage());
        }
    }

    /**
     * Check whether server-side speech is currently active.
     */
    public boolean isEnabled() {
        return enabled && voice != null;
    }

    /** Change speech rate at runtime (words per minute). */
    public void setRate(float rate) {
        if (voice != null) voice.setRate(rate);
    }
}
