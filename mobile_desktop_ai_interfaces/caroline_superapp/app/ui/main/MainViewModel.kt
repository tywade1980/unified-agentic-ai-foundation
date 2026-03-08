package com.enhanced.codeassist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val masterCoderEngine: MasterCoderEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _generatedApps = MutableStateFlow<List<GeneratedAppInfo>>(emptyList())
    val generatedApps: StateFlow<List<GeneratedAppInfo>> = _generatedApps.asStateFlow()
    
    fun initializeAI() {
        viewModelScope.launch {
            try {
                masterCoderEngine.initialize()
                updateStatus("🚀 Master Coder ready for voice commands!")
                Timber.d("AI initialization completed")
            } catch (e: Exception) {
                updateStatus("⚠️ AI initialization failed - retrying...")
                Timber.e(e, "AI initialization failed")
            }
        }
    }
    
    fun startVoiceCapture() {
        viewModelScope.launch {
            try {
                _isListening.value = true
                updateStatus("🎤 Listening for your app idea...")
                masterCoderEngine.startIdeaCapture()
            } catch (e: Exception) {
                _isListening.value = false
                updateStatus("Voice capture failed - please try again")
                Timber.e(e, "Voice capture failed")
            }
        }
    }
    
    fun stopListening() {
        _isListening.value = false
        updateStatus("Ready for next command")
    }
    
    fun addGeneratedApp(appInfo: GeneratedAppInfo) {
        val currentApps = _generatedApps.value.toMutableList()
        currentApps.add(0, appInfo) // Add to beginning
        _generatedApps.value = currentApps
    }
    
    fun removeGeneratedApp(appId: String) {
        val currentApps = _generatedApps.value.toMutableList()
        currentApps.removeAll { it.id == appId }
        _generatedApps.value = currentApps
    }
    
    fun updateAppStatus(appId: String, status: String) {
        val currentApps = _generatedApps.value.toMutableList()
        val index = currentApps.indexOfFirst { it.id == appId }
        if (index != -1) {
            currentApps[index] = currentApps[index].copy(status = status)
            _generatedApps.value = currentApps
        }
    }
    
    private fun updateStatus(status: String) {
        _uiState.value = _uiState.value.copy(
            status = status,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    fun updateProgress(progress: Float, message: String) {
        _uiState.value = _uiState.value.copy(
            progress = progress,
            progressMessage = message,
            isProcessing = progress > 0f && progress < 1f
        )
    }
    
    fun setProcessing(isProcessing: Boolean) {
        _uiState.value = _uiState.value.copy(
            isProcessing = isProcessing,
            progress = if (isProcessing) 0.1f else 0f
        )
    }
    
    fun clearProgress() {
        _uiState.value = _uiState.value.copy(
            progress = 0f,
            progressMessage = "",
            isProcessing = false
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        masterCoderEngine.cleanup()
    }
}

data class MainUiState(
    val status: String = "Initializing Master Coder...",
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val progressMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class GeneratedAppInfo(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val createdAt: Long,
    val appSize: String = "Unknown",
    val features: List<String> = emptyList()
)

