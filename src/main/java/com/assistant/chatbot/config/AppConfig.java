package com.assistant.chatbot.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Application-level Spring beans.
 */
@Configuration
public class AppConfig {

    @Value("${app.http.connect-timeout:10}")
    private long connectTimeoutSeconds;

    @Value("${app.http.read-timeout:30}")
    private long readTimeoutSeconds;

    /**
     * Shared OkHttpClient for all outbound API calls (AI + Weather).
     * Configured with generous timeouts so slow AI APIs don't cause errors.
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * RestTemplate bean for any Spring-style HTTP calls.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
