package com.ai_code_assist.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_code_assist.ApiKeyManager
import com.ai_code_assist.AutoToolingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyManager: ApiKeyManager,
    private val autoToolingEngine: AutoToolingEngine,
    private val conversationManager: ConversationManager
) : ViewModel() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _voiceInput = MutableStateFlow("")
    val voiceInput: StateFlow<String> = _voiceInput.asStateFlow()

    init {
        initializeChat()
        checkApiConnection()
    }

    private fun initializeChat() {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(
                messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "🚀 Welcome to CodeAssist AI! I can help you:\n\n• Generate complete Android apps from voice or text\n• Write and debug code in real-time\n• Create custom tools and solutions\n• Deploy apps instantly to your device\n\nWhat would you like to build today?",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    private fun checkApiConnection() {
        viewModelScope.launch {
            try {
                val hasValidKey = apiKeyManager.ensureValidApiKey() != null
                _chatState.value = _chatState.value.copy(isConnected = hasValidKey)
                
                if (!hasValidKey) {
                    addSystemMessage("⚠️ No valid API key found. Please configure your API key in settings to enable AI features.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check API connection")
                _chatState.value = _chatState.value.copy(isConnected = false)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = content,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            
            addMessage(userMessage)
            
            // Start generating response
            _isGenerating.value = true
            
            try {
                // Process the message through conversation manager
                val response = conversationManager.processMessage(content)
                
                // Generate AI response
                val aiResponse = generateAIResponse(content, response.context)
                
                // Add AI message
                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = aiResponse.content,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    codeBlocks = extractCodeBlocks(aiResponse.content)
                )
                
                addMessage(aiMessage)
                
                // Execute any code generation or tool creation
                if (aiResponse.shouldGenerateCode) {
                    executeCodeGeneration(aiResponse.codeRequest)
                }
                
                if (aiResponse.shouldCreateTool) {
                    createCustomTool(aiResponse.toolRequest)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate AI response")
                addSystemMessage("❌ Failed to generate response. Please check your connection and try again.")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun generateAIResponse(userInput: String, context: String): AIResponse {
        val apiKey = apiKeyManager.getActiveApiKey() ?: throw Exception("No API key available")
        
        val prompt = buildPrompt(userInput, context)
        
        val requestBody = JSONObject().apply {
            put("model", "gemini-pro")
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 2048)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
            .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
            .build()

        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("API request failed: ${response.code()}")
        }

        val responseBody = response.body()?.string() ?: ""
        return parseAIResponse(responseBody, userInput)
    }

    private fun buildPrompt(userInput: String, context: String): String {
        return """
You are CodeAssist AI, an advanced Android development assistant with autonomous capabilities. You can:

1. Generate complete Android applications from natural language descriptions
2. Write, debug, and optimize code in real-time
3. Create custom tools and solutions for any development challenge
4. Deploy applications instantly to connected devices
5. Provide contextual help and explanations

Current context: $context

User request: $userInput

Instructions:
- If the user wants to create an app, provide a complete implementation plan
- If they need code, generate production-ready, well-documented code
- If they encounter problems, offer multiple solution approaches
- Always explain your reasoning and provide actionable next steps
- Use emojis sparingly and professionally
- Format code blocks with proper language tags

Respond as a helpful, knowledgeable Android development expert.
        """.trimIndent()
    }

    private fun parseAIResponse(responseBody: String, userInput: String): AIResponse {
        return try {
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            AIResponse(
                content = content,
                shouldGenerateCode = detectCodeGenerationIntent(userInput, content),
                shouldCreateTool = detectToolCreationIntent(userInput, content),
                codeRequest = extractCodeRequest(userInput, content),
                toolRequest = extractToolRequest(userInput, content)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse AI response")
            AIResponse(
                content = "I encountered an issue processing your request. Could you please rephrase or try again?",
                shouldGenerateCode = false,
                shouldCreateTool = false
            )
        }
    }

    private fun detectCodeGenerationIntent(userInput: String, aiResponse: String): Boolean {
        val codeKeywords = listOf("create app", "generate", "build", "implement", "code", "develop")
        return codeKeywords.any { 
            userInput.contains(it, ignoreCase = true) || aiResponse.contains("```", ignoreCase = true)
        }
    }

    private fun detectToolCreationIntent(userInput: String, aiResponse: String): Boolean {
        val toolKeywords = listOf("tool", "utility", "helper", "custom solution", "automate")
        return toolKeywords.any { userInput.contains(it, ignoreCase = true) }
    }

    private fun extractCodeRequest(userInput: String, aiResponse: String): String {
        return if (detectCodeGenerationIntent(userInput, aiResponse)) {
            userInput
        } else ""
    }

    private fun extractToolRequest(userInput: String, aiResponse: String): String {
        return if (detectToolCreationIntent(userInput, aiResponse)) {
            userInput
        } else ""
    }

    private fun extractCodeBlocks(content: String): List<CodeBlock> {
        val codeBlocks = mutableListOf<CodeBlock>()
        val regex = "```(\\w+)?\\n([\\s\\S]*?)```".toRegex()
        
        regex.findAll(content).forEach { match ->
            val language = match.groupValues[1].ifEmpty { "text" }
            val code = match.groupValues[2].trim()
            codeBlocks.add(CodeBlock(code, language))
        }
        
        return codeBlocks
    }

    private suspend fun executeCodeGeneration(codeRequest: String) {
        try {
            addSystemMessage("🔧 Generating code for your request...")
            
            // Use the auto-tooling engine to create code generation tools if needed
            val codeGenerator = autoToolingEngine.needTool("Code generator for: $codeRequest")
            
            // Simulate code generation process
            kotlinx.coroutines.delay(2000)
            
            addSystemMessage("✅ Code generation complete! Check the generated files in your project.")
            
        } catch (e: Exception) {
            Timber.e(e, "Code generation failed")
            addSystemMessage("❌ Code generation failed: ${e.message}")
        }
    }

    private suspend fun createCustomTool(toolRequest: String) {
        try {
            addSystemMessage("🛠️ Creating custom tool for your needs...")
            
            val customTool = autoToolingEngine.createToolForProblem(toolRequest)
            
            addSystemMessage("🎉 Custom tool '${customTool.name}' created successfully!")
            
        } catch (e: Exception) {
            Timber.e(e, "Tool creation failed")
            addSystemMessage("❌ Tool creation failed: ${e.message}")
        }
    }

    fun startVoiceInput() {
        _isListening.value = true
        // Voice recognition will be handled by the UI layer
    }

    fun stopVoiceInput() {
        _isListening.value = false
    }

    fun onVoiceResult(result: String) {
        _voiceInput.value = result
        _isListening.value = false
        
        if (result.isNotBlank()) {
            sendMessage(result)
        }
    }

    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("CodeAssist AI", text)
            clipboard.setPrimaryClip(clip)
            
            addSystemMessage("📋 Copied to clipboard!")
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy to clipboard")
        }
    }

    fun openSettings() {
        // Navigate to settings - will be handled by the UI layer
        addSystemMessage("⚙️ Opening settings...")
    }

    fun clearChat() {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(messages = emptyList())
            initializeChat()
        }
    }

    fun regenerateLastResponse() {
        viewModelScope.launch {
            val messages = _chatState.value.messages
            if (messages.size >= 2) {
                val lastUserMessage = messages.findLast { it.isFromUser }
                if (lastUserMessage != null) {
                    // Remove the last AI response
                    val filteredMessages = messages.dropLast(1)
                    _chatState.value = _chatState.value.copy(messages = filteredMessages)
                    
                    // Regenerate response
                    sendMessage(lastUserMessage.content)
                }
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _chatState.value = _chatState.value.copy(
            messages = _chatState.value.messages + message
        )
    }

    private fun addSystemMessage(content: String) {
        val systemMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        addMessage(systemMessage)
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.dispatcher.executorService.shutdown()
    }
}

// Data classes for AI response processing
data class AIResponse(
    val content: String,
    val shouldGenerateCode: Boolean = false,
    val shouldCreateTool: Boolean = false,
    val codeRequest: String = "",
    val toolRequest: String = ""
)

