# 🎙️ Voice Assistant Chatbot

A Spring Boot AI-powered voice assistant that runs in your browser.  
Type or speak — it answers using Groq AI, handles built-in commands locally, and reads responses aloud via the Web Speech API.

---

## ✨ Features

| Feature | Details |
|---------|---------|
| 💬 AI Chat | Groq LLaMA-3 (free, fast) or OpenAI GPT |
| 🎤 Voice Input | Web Speech API (Chrome / Edge) |
| 🔊 Voice Output | Browser TTS — no extra setup |
| ⚡ Local Commands | Time, date, jokes, greetings — instant, no API call |
| 🌤️ Weather | OpenWeatherMap free tier |
| 🧹 Chat History | Per-session memory with clear option |

---

## 🚀 Quick Start

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- A modern browser (Chrome recommended for voice)

### 2. Get API keys (free)

| Service | URL | Required |
|---------|-----|----------|
| Groq | https://console.groq.com | ✅ Yes |
| OpenWeatherMap | https://openweathermap.org/api | Optional |

### 3. Configure

Edit `src/main/resources/application.properties`:

```properties
app.ai.api-key=YOUR_GROQ_KEY
app.weather.api-key=YOUR_OPENWEATHER_KEY   # optional
```

### 4. Run

```bash
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

---

## 🗣️ Using the Assistant

### Voice Input
1. Click the **🎤 microphone button**
2. Speak your query
3. The assistant replies in text and reads it aloud

### Text Input
Type in the input box and press **Enter** or click **➤**

### Toggle voice output
Click **🔊** in the header to mute/unmute TTS

---

## 📋 Built-in Commands (no AI needed)

| Say… | Response |
|------|----------|
| `"What time is it?"` | Current time |
| `"What's the date?"` | Today's date |
| `"Tell me a joke"` | Programmer joke |
| `"Weather in Mumbai"` | Live weather card |
| `"Clear chat"` | Reset conversation |
| `"Help"` | Show all commands |
| `"Hello"` | Context-aware greeting |

Everything else is sent to the AI.

---

## 🔄 Switching AI Providers

### OpenAI
```properties
app.ai.base-url=https://api.openai.com/v1
app.ai.model=gpt-4o-mini
app.ai.api-key=sk-...
```

### Ollama (local, offline)
```properties
app.ai.base-url=http://localhost:11434/v1
app.ai.model=llama3
app.ai.api-key=ollama
```

---

## 🏗️ Project Structure

```
src/main/java/com/assistant/chatbot/
├── VoiceAssistantApplication.java   ← Spring Boot entry point
├── controller/
│   └── ChatController.java          ← REST endpoints + page route
├── service/
│   ├── AIService.java               ← Groq/OpenAI API integration
│   ├── SpeechService.java           ← Server-side TTS (optional)
│   ├── CommandService.java          ← Local command handler
│   └── WeatherService.java          ← OpenWeatherMap integration
├── model/
│   ├── ChatRequest.java
│   └── ChatResponse.java
├── utils/
│   ├── VoiceUtils.java              ← Text cleaning for speech
│   └── AppUtils.java                ← General helpers
└── config/
    └── AppConfig.java               ← OkHttpClient + RestTemplate beans
```

---

## 🛠️ Build for production

```bash
mvn clean package -DskipTests
java -jar target/voice-assistant-chatbot-1.0.0.jar
```

---

## 📝 Notes

- Voice input requires **HTTPS** or **localhost** (browser security requirement)
- Server-side TTS via FreeTTS is disabled by default; the browser TTS works out of the box
- Conversation history is in-memory and resets on server restart
