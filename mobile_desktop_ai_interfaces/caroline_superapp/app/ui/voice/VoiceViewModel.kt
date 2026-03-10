package com.wade.caroline.ui.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.caroline.core.voice.VoiceEngine
import com.wade.caroline.core.voice.VoiceResult
import com.wade.caroline.core.voice.VoiceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * VoiceViewModel
 *
 * Exposes VoiceEngine state to any Compose screen that needs voice capability.
 * Inject this into ConstructionDashboardScreen, ChatScreen, or any other screen
 * that needs hands-free voice interaction.
 *
 * Usage in Compose:
 *   val voiceVm: VoiceViewModel = viewModel()
 *   val state by voiceVm.voiceState.collectAsState()
 *   val result by voiceVm.lastResult.collectAsState()
 *
 *   Button(onClick = { voiceVm.toggleListening() }) {
 *       Icon(if (state == VoiceState.LISTENING) Icons.Filled.MicOff else Icons.Filled.Mic, ...)
 *   }
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    // ── Configuration — override via BuildConfig or remote config in production ──
    private val orchestratorUrl = "http://10.0.2.2:8000"   // Android emulator → localhost
    private val deepgramKey     = ""  // Set via BuildConfig.DEEPGRAM_API_KEY
    private val elevenLabsKey   = ""  // Set via BuildConfig.ELEVENLABS_API_KEY
    private val elevenLabsVoice = "21m00Tcm4TlvDq8ikWAM"  // Rachel — replace with Caroline's cloned voice

    val engine = VoiceEngine(
        context = application.applicationContext,
        sessionId = "caroline_voice",
        orchestratorBaseUrl = orchestratorUrl,
        deepgramApiKey = deepgramKey,
        elevenLabsApiKey = elevenLabsKey,
        elevenLabsVoiceId = elevenLabsVoice
    )

    val voiceState: StateFlow<VoiceState> = engine.voiceState
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceState.IDLE)

    val lastResult: StateFlow<VoiceResult> = engine.lastResult
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceResult())

    val transcript: StateFlow<String> = engine.transcript
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // ─────────────────────────────────────────────────────────────────────────
    // Public Actions
    // ─────────────────────────────────────────────────────────────────────────

    /** Toggle mic on/off. Tap once to start, tap again to stop and process. */
    fun toggleListening() {
        when (voiceState.value) {
            VoiceState.IDLE  -> engine.startListening()
            VoiceState.LISTENING -> engine.stopListening()
            else -> { /* ignore taps while processing or speaking */ }
        }
    }

    /** Send a typed text command through the voice pipeline (gets spoken response). */
    fun sendTextCommand(text: String) {
        engine.sendTextCommand(text)
    }

    override fun onCleared() {
        super.onCleared()
        engine.destroy()
    }
}
