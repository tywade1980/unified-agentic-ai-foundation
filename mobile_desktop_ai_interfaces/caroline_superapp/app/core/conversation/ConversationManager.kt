package com.ai_code_assist.ui.chat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val conversationHistory = mutableListOf<ConversationEntry>()
    private val contextMemory = ConcurrentHashMap<String, Any>()
    private val userPreferences = ConcurrentHashMap<String, String>()
    private val projectContext = ConcurrentHashMap<String, ProjectInfo>()
    
    private val _conversationState = MutableStateFlow(ConversationState())
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()
    
    init {
        initializeConversationManager()
    }
    
    private fun initializeConversationManager() {
        loadUserPreferences()
        loadProjectContext()
        Timber.d("ConversationManager initialized")
    }
    
    suspend fun processMessage(message: String): ConversationResponse {
        return withContext(Dispatchers.IO) {
            try {
                // Add message to history
                val entry = ConversationEntry(
                    id = UUID.randomUUID().toString(),
                    content = message,
                    timestamp = System.currentTimeMillis(),
                    type = ConversationEntryType.USER_INPUT
                )
                
                conversationHistory.add(entry)
                
                // Analyze message intent
                val intent = analyzeMessageIntent(message)
                
                // Build context for AI response
                val context = buildConversationContext(message, intent)
                
                // Update conversation state
                updateConversationState(intent, context)
                
                ConversationResponse(
                    intent = intent,
                    context = context,
                    suggestions = generateSuggestions(intent),
                    requiresAction = determineActionRequired(intent)
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process message")
                ConversationResponse(
                    intent = MessageIntent.UNKNOWN,
                    context = "Error processing message: ${e.message}",
                    suggestions = emptyList(),
                    requiresAction = false
                )
            }
        }
    }
    
    private fun analyzeMessageIntent(message: String): MessageIntent {
        val lowerMessage = message.lowercase()
        
        return when {
            // App creation intents
            lowerMessage.contains("create app") || 
            lowerMessage.contains("build app") ||
            lowerMessage.contains("make app") -> MessageIntent.CREATE_APP
            
            // Code generation intents
            lowerMessage.contains("generate code") ||
            lowerMessage.contains("write code") ||
            lowerMessage.contains("implement") -> MessageIntent.GENERATE_CODE
            
            // Debugging intents
            lowerMessage.contains("debug") ||
            lowerMessage.contains("fix") ||
            lowerMessage.contains("error") ||
            lowerMessage.contains("bug") -> MessageIntent.DEBUG_CODE
            
            // Explanation intents
            lowerMessage.contains("explain") ||
            lowerMessage.contains("how does") ||
            lowerMessage.contains("what is") -> MessageIntent.EXPLAIN_CONCEPT
            
            // Tool creation intents
            lowerMessage.contains("tool") ||
            lowerMessage.contains("utility") ||
            lowerMessage.contains("helper") -> MessageIntent.CREATE_TOOL
            
            // Deployment intents
            lowerMessage.contains("deploy") ||
            lowerMessage.contains("publish") ||
            lowerMessage.contains("release") -> MessageIntent.DEPLOY_APP
            
            // Voice commands
            lowerMessage.contains("voice") ||
            lowerMessage.contains("speak") ||
            lowerMessage.contains("listen") -> MessageIntent.VOICE_COMMAND
            
            // Settings and configuration
            lowerMessage.contains("setting") ||
            lowerMessage.contains("config") ||
            lowerMessage.contains("setup") -> MessageIntent.CONFIGURE_SETTINGS
            
            else -> MessageIntent.GENERAL_QUERY
        }
    }
    
    private fun buildConversationContext(message: String, intent: MessageIntent): String {
        val contextBuilder = StringBuilder()
        
        // Add recent conversation history
        val recentHistory = conversationHistory.takeLast(5)
        if (recentHistory.isNotEmpty()) {
            contextBuilder.append("Recent conversation:\n")
            recentHistory.forEach { entry ->
                contextBuilder.append("${entry.type}: ${entry.content}\n")
            }
            contextBuilder.append("\n")
        }
        
        // Add current project context
        val currentProject = getCurrentProject()
        if (currentProject != null) {
            contextBuilder.append("Current project: ${currentProject.name}\n")
            contextBuilder.append("Type: ${currentProject.type}\n")
            contextBuilder.append("Language: ${currentProject.language}\n")
            contextBuilder.append("Features: ${currentProject.features.joinToString(", ")}\n\n")
        }
        
        // Add user preferences
        if (userPreferences.isNotEmpty()) {
            contextBuilder.append("User preferences:\n")
            userPreferences.forEach { (key, value) ->
                contextBuilder.append("$key: $value\n")
            }
            contextBuilder.append("\n")
        }
        
        // Add intent-specific context
        when (intent) {
            MessageIntent.CREATE_APP -> {
                contextBuilder.append("App creation context:\n")
                contextBuilder.append("- Available templates: Basic Activity, Empty Activity, Navigation Drawer\n")
                contextBuilder.append("- Supported languages: Kotlin, Java\n")
                contextBuilder.append("- Target SDK: 34\n")
                contextBuilder.append("- Min SDK: 26\n")
            }
            
            MessageIntent.GENERATE_CODE -> {
                contextBuilder.append("Code generation context:\n")
                contextBuilder.append("- Current architecture: MVVM with Jetpack Compose\n")
                contextBuilder.append("- Dependencies: Hilt, Room, Retrofit, Coroutines\n")
                contextBuilder.append("- Code style: Google Android Style Guide\n")
            }
            
            MessageIntent.DEBUG_CODE -> {
                contextBuilder.append("Debugging context:\n")
                contextBuilder.append("- Available tools: Logcat, Debugger, Profiler\n")
                contextBuilder.append("- Common issues: Memory leaks, ANRs, Crashes\n")
            }
            
            else -> {
                // Add general context
                contextBuilder.append("General context:\n")
                contextBuilder.append("- Platform: Android\n")
                contextBuilder.append("- IDE: CodeAssist with AI enhancements\n")
                contextBuilder.append("- Capabilities: Voice input, real-time deployment, auto-tooling\n")
            }
        }
        
        return contextBuilder.toString()
    }
    
    private fun generateSuggestions(intent: MessageIntent): List<String> {
        return when (intent) {
            MessageIntent.CREATE_APP -> listOf(
                "Create a simple calculator app",
                "Build a todo list with database",
                "Make a weather app with API integration",
                "Design a social media feed",
                "Create a camera app with filters"
            )
            
            MessageIntent.GENERATE_CODE -> listOf(
                "Generate a RecyclerView adapter",
                "Create a custom View component",
                "Implement network layer with Retrofit",
                "Add database operations with Room",
                "Create navigation between screens"
            )
            
            MessageIntent.DEBUG_CODE -> listOf(
                "Check for memory leaks",
                "Analyze crash logs",
                "Optimize performance",
                "Fix UI layout issues",
                "Resolve dependency conflicts"
            )
            
            MessageIntent.EXPLAIN_CONCEPT -> listOf(
                "Explain Android lifecycle",
                "How does Jetpack Compose work?",
                "What is dependency injection?",
                "Explain MVVM architecture",
                "How to handle background tasks?"
            )
            
            MessageIntent.CREATE_TOOL -> listOf(
                "Create a code formatter tool",
                "Build a dependency analyzer",
                "Make a performance profiler",
                "Create a UI testing helper",
                "Build a deployment automation tool"
            )
            
            else -> listOf(
                "What can you help me with?",
                "Show me recent projects",
                "Open project settings",
                "Start voice input mode",
                "Generate sample code"
            )
        }
    }
    
    private fun determineActionRequired(intent: MessageIntent): Boolean {
        return when (intent) {
            MessageIntent.CREATE_APP,
            MessageIntent.GENERATE_CODE,
            MessageIntent.CREATE_TOOL,
            MessageIntent.DEPLOY_APP -> true
            else -> false
        }
    }
    
    private fun updateConversationState(intent: MessageIntent, context: String) {
        _conversationState.value = _conversationState.value.copy(
            currentIntent = intent,
            contextSummary = context.take(200) + "...",
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    fun addProjectContext(projectInfo: ProjectInfo) {
        projectContext[projectInfo.id] = projectInfo
        saveProjectContext()
    }
    
    fun updateUserPreference(key: String, value: String) {
        userPreferences[key] = value
        saveUserPreferences()
    }
    
    fun getCurrentProject(): ProjectInfo? {
        return projectContext.values.firstOrNull { it.isActive }
    }
    
    fun setActiveProject(projectId: String) {
        projectContext.values.forEach { it.isActive = false }
        projectContext[projectId]?.isActive = true
        saveProjectContext()
    }
    
    fun getConversationHistory(limit: Int = 50): List<ConversationEntry> {
        return conversationHistory.takeLast(limit)
    }
    
    fun clearConversationHistory() {
        conversationHistory.clear()
        _conversationState.value = ConversationState()
    }
    
    fun exportConversationHistory(): String {
        return conversationHistory.joinToString("\n") { entry ->
            "${entry.timestamp}: [${entry.type}] ${entry.content}"
        }
    }
    
    private fun loadUserPreferences() {
        try {
            // Load from SharedPreferences or database
            // Default preferences
            userPreferences["code_style"] = "google"
            userPreferences["language"] = "kotlin"
            userPreferences["theme"] = "dark"
            userPreferences["voice_enabled"] = "true"
            
            Timber.d("User preferences loaded")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user preferences")
        }
    }
    
    private fun saveUserPreferences() {
        try {
            // Save to SharedPreferences or database
            Timber.d("User preferences saved")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save user preferences")
        }
    }
    
    private fun loadProjectContext() {
        try {
            // Load from database or file system
            // Add default project if none exists
            if (projectContext.isEmpty()) {
                val defaultProject = ProjectInfo(
                    id = "default",
                    name = "My CodeAssist Project",
                    type = "Android App",
                    language = "Kotlin",
                    features = listOf("Jetpack Compose", "MVVM", "Hilt"),
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                projectContext[defaultProject.id] = defaultProject
            }
            
            Timber.d("Project context loaded")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load project context")
        }
    }
    
    private fun saveProjectContext() {
        try {
            // Save to database or file system
            Timber.d("Project context saved")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save project context")
        }
    }
    
    fun addConversationEntry(content: String, type: ConversationEntryType) {
        val entry = ConversationEntry(
            id = UUID.randomUUID().toString(),
            content = content,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        conversationHistory.add(entry)
        
        // Keep history manageable
        if (conversationHistory.size > 100) {
            conversationHistory.removeAt(0)
        }
    }
    
    fun getContextMemory(key: String): Any? {
        return contextMemory[key]
    }
    
    fun setContextMemory(key: String, value: Any) {
        contextMemory[key] = value
    }
    
    fun clearContextMemory() {
        contextMemory.clear()
    }
}

// Data classes for conversation management
data class ConversationResponse(
    val intent: MessageIntent,
    val context: String,
    val suggestions: List<String>,
    val requiresAction: Boolean
)

data class ConversationState(
    val currentIntent: MessageIntent = MessageIntent.UNKNOWN,
    val contextSummary: String = "",
    val lastUpdateTime: Long = 0L,
    val isProcessing: Boolean = false
)

data class ConversationEntry(
    val id: String,
    val content: String,
    val timestamp: Long,
    val type: ConversationEntryType
)

data class ProjectInfo(
    val id: String,
    val name: String,
    val type: String,
    val language: String,
    val features: List<String>,
    var isActive: Boolean = false,
    val createdAt: Long
)

enum class MessageIntent {
    CREATE_APP,
    GENERATE_CODE,
    DEBUG_CODE,
    EXPLAIN_CONCEPT,
    CREATE_TOOL,
    DEPLOY_APP,
    VOICE_COMMAND,
    CONFIGURE_SETTINGS,
    GENERAL_QUERY,
    UNKNOWN
}

enum class ConversationEntryType {
    USER_INPUT,
    AI_RESPONSE,
    SYSTEM_MESSAGE,
    CODE_GENERATION,
    TOOL_CREATION,
    ERROR_MESSAGE
}

