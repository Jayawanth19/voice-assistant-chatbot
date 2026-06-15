package com.assistant.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WeatherService — fetches current weather from OpenWeatherMap.
 *
 * Free tier: 1,000 calls/day — more than enough for a personal assistant.
 * Sign up at https://openweathermap.org/api and set:
 *   app.weather.api-key=YOUR_KEY   in application.properties
 *
 * Detects weather queries like:
 *   "weather in London"
 *   "what's the weather in Chennai?"
 *   "temperature in Tokyo"
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    // Matches "weather in <City>" / "temperature in <City>"
    private static final Pattern CITY_PATTERN =
            Pattern.compile("(?:weather|temperature|forecast|temp)\\s+(?:in|for|at)\\s+([a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE);

    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.weather.api-key:REPLACE_WITH_YOUR_OPENWEATHER_KEY}")
    private String apiKey;

    public WeatherService(OkHttpClient http) {
        this.http = http;
    }

    /**
     * Detect if a user message is a weather query.
     */
    public boolean isWeatherQuery(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return (lower.contains("weather") || lower.contains("temperature") || lower.contains("temp"))
                && (lower.contains(" in ") || lower.contains(" for ") || lower.contains(" at "));
    }

    /**
     * Handle a weather query end-to-end.
     *
     * @param input raw user message
     * @return a human-readable weather description
     */
    public String getWeatherResponse(String input) {
        String city = extractCity(input);
        if (city == null || city.isBlank()) {
            return "I couldn't detect a city name. Try 'Weather in Mumbai'.";
        }
        return fetchWeather(city.trim());
    }

    /**
     * Fetch weather data and build a map of key fields.
     * Called by ChatController to attach structured data to the response.
     */
    public Map<String, Object> fetchWeatherData(String city) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            String url = String.format(BASE_URL, city.replace(" ", "+"), apiKey);
            Request req = new Request.Builder().url(url).build();
            try (Response res = http.newCall(req).execute()) {
                if (!res.isSuccessful()) {
                    data.put("error", "City not found or API error.");
                    return data;
                }
                JsonNode root = mapper.readTree(res.body().string());
                data.put("city",        root.path("name").asText());
                data.put("country",     root.path("sys").path("country").asText());
                data.put("temperature", Math.round(root.path("main").path("temp").asDouble()) + "°C");
                data.put("feels_like",  Math.round(root.path("main").path("feels_like").asDouble()) + "°C");
                data.put("humidity",    root.path("main").path("humidity").asInt() + "%");
                data.put("description", root.path("weather").get(0).path("description").asText());
                data.put("wind_speed",  root.path("wind").path("speed").asDouble() + " m/s");
            }
        } catch (Exception e) {
            log.error("Weather fetch error: {}", e.getMessage());
            data.put("error", "Failed to fetch weather data.");
        }
        return data;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String fetchWeather(String city) {
        Map<String, Object> data = fetchWeatherData(city);
        if (data.containsKey("error")) {
            return "Sorry, I couldn't find weather information for " + city + ".";
        }
        return String.format(
            "The weather in %s, %s is currently %s with %s. " +
            "Temperature: %s (feels like %s). Humidity: %s. Wind: %s.",
            data.get("city"), data.get("country"),
            data.get("description"), data.get("description"),
            data.get("temperature"), data.get("feels_like"),
            data.get("humidity"), data.get("wind_speed")
        );
    }

    private String extractCity(String input) {
        Matcher m = CITY_PATTERN.matcher(input);
        if (m.find()) {
            return m.group(1).replaceAll("[^a-zA-Z\\s]", "").trim();
        }
        return null;
    }
}
