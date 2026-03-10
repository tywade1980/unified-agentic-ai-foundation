package com.wade.caroline.core.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// Voice Pipeline State
// ─────────────────────────────────────────────────────────────────────────────

enum class VoiceState {
    IDLE,           // Not doing anything
    LISTENING,      // Recording mic audio → Deepgram STT
    PROCESSING,     // Grok agent is thinking
    SPEAKING,       // ElevenLabs TTS audio is playing
    ERROR           // Something went wrong
}

data class VoiceResult(
    val transcript: String = "",
    val agentResponse: String = "",
    val voiceText: String = "",
    val agentUsed: String = "",
    val error: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// VoiceEngine
//
// Full hands-free voice loop:
//   1. startListening()  → Records mic audio
//   2. stopListening()   → Sends audio to Deepgram STT → gets transcript
//   3. Sends transcript  → Orchestrator /voice/full_pipeline
//   4. Receives ElevenLabs TTS audio stream → plays on device speaker
//
// Usage from ViewModel:
//   val engine = VoiceEngine(context, sessionId = "wade_voice")
//   engine.startListening()
//   // ... user speaks ...
//   engine.stopListening()
//   engine.voiceState.collect { state -> ... }
//   engine.lastResult.collect { result -> ... }
// ─────────────────────────────────────────────────────────────────────────────

class VoiceEngine(
    private val context: Context,
    private val sessionId: String = "voice_default",
    private val orchestratorBaseUrl: String = "http://10.0.2.2:8000",
    private val deepgramApiKey: String = "",
    private val elevenLabsApiKey: String = "",
    private val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM"  // Rachel — replace with Caroline's cloned voice
) {
    companion object {
        private const val TAG = "VoiceEngine"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val DEEPGRAM_URL = "https://api.deepgram.com/v1/listen?model=nova-3&smart_format=true&language=en-US"
        private const val ELEVENLABS_MODEL = "eleven_turbo_v2_5"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val audioBuffer = ByteArrayOutputStream()
    private var isRecording = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastResult = MutableStateFlow(VoiceResult())
    val lastResult: StateFlow<VoiceResult> = _lastResult.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Start recording from the microphone.
     * Call stopListening() when the user finishes speaking.
     */
    fun startListening() {
        if (_voiceState.value == VoiceState.LISTENING) return
        _voiceState.value = VoiceState.LISTENING
        audioBuffer.reset()

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
        )

        recordingJob = scope.launch {
            audioRecord?.startRecording()
            isRecording = true
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) audioBuffer.write(buffer, 0, read)
            }
        }
        Log.d(TAG, "Listening started.")
    }

    /**
     * Stop recording and run the full pipeline:
     * Deepgram STT → Grok Orchestrator → ElevenLabs TTS
     */
    fun stopListening() {
        if (_voiceState.value != VoiceState.LISTENING) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingJob?.cancel()

        val audioBytes = audioBuffer.toByteArray()
        Log.d(TAG, "Recorded ${audioBytes.size} bytes. Running pipeline...")

        scope.launch {
            runFullPipeline(audioBytes)
        }
    }

    /**
     * Send a text string directly to the voice pipeline, bypassing STT.
     * Useful for typed commands that should still get a spoken response.
     */
    fun sendTextCommand(text: String) {
        scope.launch {
            _voiceState.value = VoiceState.PROCESSING
            runOrchestratorAndSpeak(text)
        }
    }

    fun destroy() {
        audioRecord?.release()
        scope.cancel()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pipeline Steps
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun runFullPipeline(audioBytes: ByteArray) {
        try {
            // Step 1: Deepgram STT
            _voiceState.value = VoiceState.PROCESSING
            val transcript = transcribeWithDeepgram(audioBytes)
            if (transcript.isBlank()) {
                _voiceState.value = VoiceState.ERROR
                _lastResult.value = VoiceResult(error = "Could not understand audio. Please try again.")
                _voiceState.value = VoiceState.IDLE
                return
            }
            _transcript.value = transcript
            Log.d(TAG, "Transcript: $transcript")

            // Step 2 + 3: Orchestrator (Grok) + ElevenLabs TTS
            runOrchestratorAndSpeak(transcript)

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error", e)
            _lastResult.value = VoiceResult(error = e.message)
            _voiceState.value = VoiceState.ERROR
            _voiceState.value = VoiceState.IDLE
        }
    }

    private suspend fun runOrchestratorAndSpeak(transcript: String) {
        try {
            // Step 2: Send to Orchestrator /voice/full_pipeline
            // This returns ElevenLabs TTS audio stream directly
            val (responseText, voiceText, agentUsed, audioBytes) = callFullPipeline(transcript)

            _lastResult.value = VoiceResult(
                transcript = transcript,
                agentResponse = responseText,
                voiceText = voiceText,
                agentUsed = agentUsed
            )

            // Step 3: Play audio
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                _voiceState.value = VoiceState.SPEAKING
                playAudioBytes(audioBytes)
            }
            _voiceState.value = VoiceState.IDLE

        } catch (e: Exception) {
            Log.e(TAG, "Orchestrator/TTS error", e)
            _lastResult.value = VoiceResult(transcript = transcript, error = e.message)
            _voiceState.value = VoiceState.IDLE
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deepgram STT
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun transcribeWithDeepgram(audioBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            val url = URL(DEEPGRAM_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Token $deepgramApiKey")
            conn.setRequestProperty("Content-Type", "audio/wav")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            // Write raw PCM wrapped as WAV
            val wavBytes = pcmToWav(audioBytes, SAMPLE_RATE, 1, 16)
            conn.outputStream.write(wavBytes)

            val responseCode = conn.responseCode
            val raw = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) conn.inputStream else conn.errorStream
            )).use { it.readText() }

            if (responseCode !in 200..299) {
                throw Exception("Deepgram error $responseCode: $raw")
            }

            // Parse Deepgram response
            val json = JSONObject(raw)
            json.getJSONObject("results")
                .getJSONArray("channels")
                .getJSONObject(0)
                .getJSONArray("alternatives")
                .getJSONObject(0)
                .getString("transcript")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Orchestrator Full Pipeline Call
    // Returns: (responseText, voiceText, agentUsed, audioBytes?)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun callFullPipeline(transcript: String): PipelineResult {
        return withContext(Dispatchers.IO) {
            val url = URL("$orchestratorBaseUrl/voice/full_pipeline")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000

            val body = JSONObject().apply {
                put("transcript", transcript)
                put("session_id", sessionId)
                put("agent", "Orchestrator")
                put("return_audio", elevenLabsApiKey.isNotBlank())
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val contentType = conn.contentType ?: ""

            return@withContext if (contentType.contains("audio/mpeg")) {
                // Audio stream response — read headers for text, read body for audio
                val responseText = conn.getHeaderField("X-Response-Text") ?: ""
                val voiceText = conn.getHeaderField("X-Voice-Text") ?: ""
                val agentUsed = conn.getHeaderField("X-Agent-Used") ?: "Orchestrator"
                val audioBytes = conn.inputStream.readBytes()
                PipelineResult(responseText, voiceText, agentUsed, audioBytes)
            } else {
                // JSON response (no audio key configured)
                val raw = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(raw)
                val responseText = json.optString("response_text", "")
                val voiceText = json.optString("voice_text", responseText)
                val agentUsed = json.optString("agent_used", "Orchestrator")
                PipelineResult(responseText, voiceText, agentUsed, null)
            }
        }
    }

    data class PipelineResult(
        val responseText: String,
        val voiceText: String,
        val agentUsed: String,
        val audioBytes: ByteArray?
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Audio Playback
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun playAudioBytes(mp3Bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                // Use MediaPlayer for MP3 playback from byte array
                val tempFile = java.io.File.createTempFile("caroline_tts_", ".mp3", context.cacheDir)
                tempFile.writeBytes(mp3Bytes)

                val player = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepare()
                }

                val latch = java.util.concurrent.CountDownLatch(1)
                player.setOnCompletionListener {
                    it.release()
                    tempFile.delete()
                    latch.countDown()
                }
                player.start()
                latch.await()

            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PCM → WAV Conversion (for Deepgram upload)
    // ─────────────────────────────────────────────────────────────────────────

    private fun pcmToWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val dataSize = pcmData.size
        val headerSize = 44
        val wav = ByteArray(headerSize + dataSize)
        val byteRate = sampleRate * channels * bitsPerSample / 8

        fun writeInt(arr: ByteArray, offset: Int, value: Int) {
            arr[offset] = (value and 0xFF).toByte()
            arr[offset + 1] = ((value shr 8) and 0xFF).toByte()
            arr[offset + 2] = ((value shr 16) and 0xFF).toByte()
            arr[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun writeShort(arr: ByteArray, offset: Int, value: Short) {
            arr[offset] = (value.toInt() and 0xFF).toByte()
            arr[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }

        // RIFF header
        "RIFF".toByteArray().copyInto(wav, 0)
        writeInt(wav, 4, 36 + dataSize)
        "WAVE".toByteArray().copyInto(wav, 8)
        "fmt ".toByteArray().copyInto(wav, 12)
        writeInt(wav, 16, 16)
        writeShort(wav, 20, 1)
        writeShort(wav, 22, channels.toShort())
        writeInt(wav, 24, sampleRate)
        writeInt(wav, 28, byteRate)
        writeShort(wav, 32, (channels * bitsPerSample / 8).toShort())
        writeShort(wav, 34, bitsPerSample.toShort())
        "data".toByteArray().copyInto(wav, 36)
        writeInt(wav, 40, dataSize)
        pcmData.copyInto(wav, 44)

        return wav
    }
}
