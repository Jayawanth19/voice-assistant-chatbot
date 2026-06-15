// ─────────────────────────────────────────────────────────────
//  Voice Assistant — Frontend Script
//  Handles: chat UI, Web Speech API (STT), browser TTS, REST calls
// ─────────────────────────────────────────────────────────────

const SESSION_ID   = document.getElementById('sessionId')?.value || crypto.randomUUID();
const messagesEl   = document.getElementById('messages');
const userInput    = document.getElementById('userInput');
const btnSend      = document.getElementById('btnSend');
const btnMic       = document.getElementById('btnMic');
const btnClear     = document.getElementById('btnClearChat');
const btnHelp      = document.getElementById('btnHelp');
const btnNewChat   = document.getElementById('btnNewChat');
const btnTts       = document.getElementById('btnTtsToggle');
const voiceStatus  = document.getElementById('voiceStatus');
const statusDot    = document.querySelector('.status-dot');
const statusText   = document.querySelector('.status-text');

let isRecording   = false;
let ttsEnabled    = true;
let recognition   = null;
let typingRow     = null;

// ── Speech Recognition (STT) setup ───────────────────────────
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.continuous    = false;
  recognition.interimResults = true;
  recognition.lang          = 'en-US';

  recognition.onstart = () => {
    isRecording = true;
    btnMic.classList.add('recording');
    btnMic.textContent = '⏹';
    setStatus('listening', 'Listening…');
    voiceStatus.textContent = '🎙️ Listening — speak now…';
  };

  recognition.onresult = (e) => {
    const transcript = Array.from(e.results)
      .map(r => r[0].transcript).join('');
    userInput.value = transcript;
    autoResize(userInput);
    if (e.results[e.results.length - 1].isFinal) {
      voiceStatus.textContent = '✅ Got it!';
    }
  };

  recognition.onerror = (e) => {
    console.error('STT error:', e.error);
    voiceStatus.textContent = e.error === 'not-allowed'
      ? '🚫 Microphone access denied.'
      : '⚠️ Voice recognition error. Try again.';
    stopRecording();
  };

  recognition.onend = () => {
    stopRecording();
    if (userInput.value.trim()) {
      setTimeout(() => sendMessage(), 300);
    }
  };
} else {
  btnMic.title    = 'Voice not supported in this browser';
  btnMic.disabled = true;
  btnMic.style.opacity = '0.4';
}

// ── TTS (browser Web Speech API) ─────────────────────────────
function speak(text) {
  if (!ttsEnabled || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const utt  = new SpeechSynthesisUtterance(cleanForSpeech(text));
  utt.rate   = 1.0;
  utt.pitch  = 1.0;
  utt.volume = 1.0;
  // Prefer a natural English voice if available
  const voices = window.speechSynthesis.getVoices();
  const preferred = voices.find(v => v.lang.startsWith('en') && v.localService);
  if (preferred) utt.voice = preferred;
  window.speechSynthesis.speak(utt);
}

function cleanForSpeech(text) {
  return text
    .replace(/```[\s\S]*?```/g, 'code block')
    .replace(/`[^`]+`/g, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/https?:\/\/\S+/g, 'link')
    .replace(/#+\s/g, '')
    .replace(/[-•*]\s/g, '')
    .replace(/\n+/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim();
}

// ── Send message ──────────────────────────────────────────────
async function sendMessage() {
  const text = userInput.value.trim();
  if (!text) return;

  // Clear input
  userInput.value = '';
  autoResize(userInput);
  voiceStatus.textContent = '';

  // Render user bubble
  appendMessage('user', text);

  // Show typing indicator
  showTyping();
  setStatus('thinking', 'Thinking…');
  btnSend.disabled = true;

  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, sessionId: SESSION_ID, voiceInput: isRecording })
    });

    const data = await res.json();
    hideTyping();

    if (data.type === 'clear' && data.message === '__CLEAR__') {
      clearMessages();
    } else if (data.type === 'weather' && data.data && !data.data.error) {
      appendMessage('bot', data.message, data.type, data.data);
    } else {
      appendMessage('bot', data.message, data.type);
    }

    if (data.speakResponse !== false) {
      speak(data.message);
    }

  } catch (err) {
    hideTyping();
    appendMessage('bot', '⚠️ Could not reach the server. Please check your connection.', 'error');
    console.error('Chat error:', err);
  } finally {
    setStatus('ready', 'Ready');
    btnSend.disabled = false;
  }
}

// ── DOM helpers ───────────────────────────────────────────────
function appendMessage(role, text, type = 'text', extra = null) {
  // Remove welcome message on first real message
  const welcome = messagesEl.querySelector('.welcome-message');
  if (welcome) welcome.remove();

  const row    = document.createElement('div');
  row.className = `message-row ${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'avatar';
  avatar.textContent = role === 'user' ? '🧑' : '🤖';

  const col = document.createElement('div');

  const bubble = document.createElement('div');
  bubble.className = `bubble${type === 'error' ? ' error' : ''}`;

  const p = document.createElement('p');
  p.textContent = text;
  bubble.appendChild(p);

  // Weather card
  if (type === 'weather' && extra && !extra.error) {
    bubble.appendChild(buildWeatherCard(extra));
  }

  const meta = document.createElement('div');
  meta.className = 'msg-meta';
  meta.textContent = formatTime(Date.now());

  col.appendChild(bubble);
  col.appendChild(meta);
  row.appendChild(avatar);
  row.appendChild(col);
  messagesEl.appendChild(row);
  scrollToBottom();
}

function buildWeatherCard(d) {
  const card = document.createElement('div');
  card.className = 'weather-card';
  card.innerHTML = `
    <div class="weather-city">📍 ${d.city}, ${d.country}</div>
    <div class="weather-temp">${d.temperature}</div>
    <div style="color:#93c5fd;font-size:13px;margin:4px 0;">${d.description}</div>
    <div class="weather-grid">
      <span>Feels like</span><span>${d.feels_like}</span>
      <span>Humidity</span><span>${d.humidity}</span>
      <span>Wind</span><span>${d.wind_speed}</span>
    </div>`;
  return card;
}

function showTyping() {
  const row = document.createElement('div');
  row.className = 'message-row bot';
  row.id = 'typingRow';
  row.innerHTML = `
    <div class="avatar">🤖</div>
    <div class="typing-indicator">
      <div class="typing-dot"></div>
      <div class="typing-dot"></div>
      <div class="typing-dot"></div>
    </div>`;
  messagesEl.appendChild(row);
  scrollToBottom();
  typingRow = row;
}

function hideTyping() {
  if (typingRow) { typingRow.remove(); typingRow = null; }
}

function clearMessages() {
  messagesEl.innerHTML = `
    <div class="welcome-message">
      <div class="welcome-icon">🎙️</div>
      <p>Chat cleared. Say hello or type a message.</p>
      <p class="welcome-hint">Click 🎤 to use your voice.</p>
    </div>`;
}

function scrollToBottom() {
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

function setStatus(state, text) {
  statusDot.className  = `status-dot ${state === 'ready' ? '' : state}`;
  statusText.textContent = text;
}

function formatTime(ts) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

function autoResize(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

// ── Mic helpers ───────────────────────────────────────────────
function stopRecording() {
  isRecording = false;
  btnMic.classList.remove('recording');
  btnMic.textContent = '🎤';
  setStatus('ready', 'Ready');
}

// ── Event listeners ───────────────────────────────────────────
btnSend.addEventListener('click', sendMessage);

userInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
});

userInput.addEventListener('input', () => autoResize(userInput));

btnMic.addEventListener('click', () => {
  if (!recognition) return;
  if (isRecording) {
    recognition.stop();
  } else {
    try { recognition.start(); } catch (e) { console.warn(e); }
  }
});

btnTts.addEventListener('click', () => {
  ttsEnabled = !ttsEnabled;
  btnTts.classList.toggle('active', ttsEnabled);
  btnTts.textContent = ttsEnabled ? '🔊' : '🔇';
  if (!ttsEnabled) window.speechSynthesis?.cancel();
});

btnClear.addEventListener('click', async () => {
  await fetch('/api/clear', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId: SESSION_ID })
  });
  clearMessages();
});

btnNewChat.addEventListener('click', () => btnClear.click());

btnHelp.addEventListener('click', () => {
  userInput.value = 'help';
  sendMessage();
});

// Load voices for TTS (Chrome needs this async)
if (window.speechSynthesis) {
  window.speechSynthesis.onvoiceschanged = () => window.speechSynthesis.getVoices();
}
