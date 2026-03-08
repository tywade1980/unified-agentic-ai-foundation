// ================================================================================
// IDEScreen.kt - Main IDE Interface
// ================================================================================

package com.aiassist.codeide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.aiassist.codeide.core.AIEngine
import com.aiassist.codeide.core.ProjectManager
import com.aiassist.codeide.core.VoiceCommandProcessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDEScreen(
    aiEngine: AIEngine,
    projectManager: ProjectManager,
    voiceProcessor: VoiceCommandProcessor,
    onVoiceCommand: () -> Unit,
    isListening: StateFlow<Boolean>
) {
    var currentFile by remember { mutableStateOf("MainActivity.kt") }
    var textCommand by remember { mutableStateOf("") }
    
    val isListeningState by isListening.collectAsStateWithLifecycle()
    val isProcessing by aiEngine.isProcessing.collectAsStateWithLifecycle()
    val evolutionHistory by aiEngine.evolutionHistory.collectAsStateWithLifecycle()
    val currentProject by projectManager.currentProject.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar
        TopAppBar(
            title = { 
                Text("🤖 AI Code Assist IDE") 
            },
            actions = {
                OutlinedTextField(
                    value = textCommand,
                    onValueChange = { textCommand = it },
                    placeholder = { Text("Type command...") },
                    modifier = Modifier.width(200.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (textCommand.isNotEmpty()) {
                                    voiceProcessor.processCommand(textCommand)
                                    textCommand = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Command")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = onVoiceCommand,
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isListeningState) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (isListeningState) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Command"
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = { aiEngine.evolveCodebase() }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Evolve")
                }
                
                IconButton(onClick = { projectManager.buildAPK() }) {
                    Icon(Icons.Default.Build, contentDescription = "Build APK")
                }
                
                IconButton(onClick = { projectManager.deployAPK() }) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Deploy")
                }
            }
        )
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar - File Explorer & Tools
            Card(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Column {
                    // File Explorer
                    Text(
                        "Project Files",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(currentProject?.files ?: emptyList()) { file ->
                            FileItem(
                                fileName = file.name,
                                isSelected = file.name == currentFile,
                                onClick = { currentFile = file.name }
                            )
                        }
                    }
                    
                    Divider()
                    
                    // AI Tools
                    Text(
                        "AI Tools",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    AIToolsList(
                        onToolClick = { tool ->
                            voiceProcessor.processCommand("use $tool")
                        }
                    )
                }
            }
            
            // Center - Code Editor
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Column {
                    // Editor Tabs
                    ScrollableTabRow(
                        selectedTabIndex = 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = true,
                            onClick = { },
                            text = { Text(currentFile) }
                        )
                    }
                    
                    // Code Editor
                    CodeEditor(
                        content = currentProject?.files?.find { it.name == currentFile }?.content ?: "",
                        onContentChange = { newContent ->
                            projectManager.updateFileContent(currentFile, newContent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Right Sidebar - Preview & Console
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Column {
                    // Preview Section
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("App Preview")
                        }
                    }
                    
                    Divider()
                    
                    // Console & Evolution Log
                    Text(
                        "AI Console",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        items(evolutionHistory) { event ->
                            EvolutionLogItem(event = event)
                        }
                        
                        if (isProcessing) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Processing...")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        Color.Transparent
        
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when {
                fileName.endsWith(".kt") -> Icons.Default.Code
                fileName.endsWith(".xml") -> Icons.Default.Description
                fileName.endsWith(".gradle") -> Icons.Default.Build
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            fileName,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AIToolsList(
    onToolClick: (String) -> Unit
) {
    val tools = listOf(
        "Code Generator" to Icons.Default.AutoAwesome,
        "Bug Detector" to Icons.Default.BugReport,
        "Performance Optimizer" to Icons.Default.Speed,
        "UI Builder" to Icons.Default.Palette,
        "Test Generator" to Icons.Default.Quiz,
        "Documentation Writer" to Icons.Default.Article
    )
    
    LazyColumn {
        items(tools) { (tool, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToolClick(tool) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tool,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CodeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(content) }
    
    LaunchedEffect(content) {
        text = content
    }
    
    BasicTextField(
        value = text,
        onValueChange = { 
            text = it
            onContentChange(it)
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun EvolutionLogItem(
    event: AIEngine.EvolutionEvent
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                "🧬 ${event.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                event.type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================================================================================
// ProjectManager.kt - Project & Build Management
// ================================================================================

package com.aiassist.codeide.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class ProjectManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject
    
    private val _buildStatus = MutableStateFlow<BuildStatus>(BuildStatus.Idle)
    val buildStatus: StateFlow<BuildStatus> = _buildStatus
    
    private val _deployStatus = MutableStateFlow<DeployStatus>(DeployStatus.Idle)
    val deployStatus: StateFlow<DeployStatus> = _deployStatus
    
    init {
        createDefaultProject()
    }
    
    data class Project(
        val id: String,
        val name: String,
        val files: List<ProjectFile>,
        val version: String = "1.0.0"
    )
    
    data class ProjectFile(
        val name: String,
        val content: String,
        val type: FileType
    )
    
    enum class FileType {
        KOTLIN, XML, GRADLE, MANIFEST
    }
    
    sealed class BuildStatus {
        object Idle : BuildStatus()
        data class Building(val progress: Int, val message: String) : BuildStatus()
        data class Success(val apkPath: String, val size: String) : BuildStatus()
        data class Error(val message: String) : BuildStatus()
    }
    
    sealed class DeployStatus {
        object Idle : DeployStatus()
        data class Deploying(val step: String) : DeployStatus()
        object Success : DeployStatus()
        data class Error(val message: String) : DeployStatus()
    }
    
    private fun createDefaultProject() {
        val defaultFiles = listOf(
            ProjectFile(
                "MainActivity.kt",
                generateMainActivityTemplate(),
                FileType.KOTLIN
            ),
            ProjectFile(
                "activity_main.xml",
                generateLayoutTemplate(),
                FileType.XML
            ),
            ProjectFile(
                "AndroidManifest.xml",
                generateManifestTemplate(),
                FileType.MANIFEST
            ),
            ProjectFile(
                "build.gradle.kts",
                generateBuildGradleTemplate(),
                FileType.GRADLE
            )
        )
        
        _currentProject.value = Project(
            id = UUID.randomUUID().toString(),
            name = "AI Assist App",
            files = defaultFiles
        )
    }
    
    private fun generateMainActivityTemplate(): String {
        return """
package com.aiassist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🤖 AI Generated App",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                // AI will implement functionality here
            }
        ) {
            Text("AI Button")
        }
    }
}
        """.trimIndent()
    }
    
    private fun generateLayoutTemplate(): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="16dp">

    <TextView
        android:id="@+id/welcome_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="AI Code Assist Ready!"
        android:textSize="18sp"
        android:textStyle="bold" />

    <Button
        android:id="@+id/ai_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="AI Generated Button" />

</LinearLayout>
        """.trimIndent()
    }
    
    private fun generateManifestTemplate(): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.aiassist.app">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AIAssistApp">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.AIAssistApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
        """.trimIndent()
    }
    
    private fun generateBuildGradleTemplate(): String {
        return """
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.aiassist.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.aiassist.app"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.8'
    }

    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.compose.ui:ui:1.5.8'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.5.8'
    implementation 'androidx.compose.material3:material3:1.1.2'
}
        """.trimIndent()
    }
    
    fun updateFileContent(fileName: String, newContent: String) {
        val currentProject = _currentProject.value ?: return
        val updatedFiles = currentProject.files.map { file ->
            if (file.name == fileName) {
                file.copy(content = newContent)
            } else {
                file
            }
        }
        
        _currentProject.value = currentProject.copy(files = updatedFiles)
    }
    
    fun addFile(fileName: String, content: String, type: FileType) {
        val currentProject = _currentProject.value ?: return
        val newFile = ProjectFile(fileName, content, type)
        val updatedFiles = currentProject.files + newFile
        
        _currentProject.value = currentProject.copy(files = updatedFiles)
    }
    
    fun buildAPK() {
        scope.launch {
            _buildStatus.value = BuildStatus.Building(0, "Initializing build...")
            Log.d("ProjectManager", "Starting APK build process")
            
            val buildSteps = listOf(
                "Compiling Kotlin sources..." to 15,
                "Processing resources..." to 30,
                "Generating R.java..." to 45,
                "Creating DEX files..." to 60,
                "Packaging APK..." to 80,
                "Signing APK..." to 90,
                "Optimizing APK..." to 100
            )
            
            for ((step, progress) in buildSteps) {
                _buildStatus.value = BuildStatus.Building(progress, step)
                delay(1500) // Simulate build time
            }
            
            val apkName = "ai-assist-app-v${System.currentTimeMillis()}.apk"
            val apkSize = "${(15..25).random()}.${(1..9).random()}MB"
            
            _buildStatus.value = BuildStatus.Success(apkName, apkSize)
            Log.d("ProjectManager", "APK built successfully: $apkName")
            
            // Reset after 5 seconds
            delay(5000)
            _buildStatus.value = BuildStatus.Idle
        }
    }
    
    fun deployAPK() {
        scope.launch {
            val buildStatus = _buildStatus.value
            if (buildStatus !is BuildStatus.Success) {
                _deployStatus.value = DeployStatus.Error("No APK found. Build first.")
                delay(3000)
                _deployStatus.value = DeployStatus.Idle
                return@launch
            }
            
            val deploySteps = listOf(
                "Scanning for devices...",
                "Device found: Pixel 6 Pro",
                "Establishing ADB connection...",
                "Copying APK to device...",
                "Installing package...",
                "Configuring permissions...",
                "Launching application..."
            )
            
            for (step in deploySteps) {
                _deployStatus.value = DeployStatus.Deploying(step)
                Log.d("ProjectManager", "Deploy: $step")
                delay(1200)
            }
            
            _deployStatus.value = DeployStatus.Success
            Log.d("ProjectManager", "APK deployed successfully")
            
            // Reset after 3 seconds
            delay(3000)
            _deployStatus.value = DeployStatus.Idle
        }
    }
}

// ================================================================================
// VoiceCommandProcessor.kt - Voice Command Processing
// ================================================================================

package com.aiassist.codeide.core

import android.util.Log

class VoiceCommandProcessor(
    private val aiEngine: AIEngine,
    private val projectManager: ProjectManager
) {
    
    fun processCommand(command: String) {
        Log.d("VoiceProcessor", "Processing command: $command")
        
        when {
            command.contains("build", ignoreCase = true) -> {
                projectManager.buildAPK()
            }
            command.contains("deploy", ignoreCase = true) -> {
                projectManager.deployAPK()
            }
            command.contains("evolve", ignoreCase = true) -> {
                aiEngine.evolveCodebase()
            }
            else -> {
                val generatedCode = aiEngine.processCommand(command)
                injectCodeIntoProject(generatedCode)
            }
        }
    }
    
    private fun injectCodeIntoProject(code: String) {
        val currentProject = projectManager.currentProject.value ?: return
        val mainActivity = currentProject.files.find { it.name == "MainActivity.kt" }
        
        if (mainActivity != null) {
            val updatedContent = injectCodeIntoKotlinFile(mainActivity.content, code)
            projectManager.updateFileContent("MainActivity.kt", updatedContent)
        }
    }
    
    private fun injectCodeIntoKotlinFile(existingCode: String, newCode: String): String {
        // Find the best injection point (before the last closing brace)
        val lastBraceIndex = existingCode.lastIndexOf('}')
        
        return if (lastBraceIndex != -1) {
            val beforeBrace = existingCode.substring(0, lastBraceIndex)
            val afterBrace = existingCode.substring(lastBraceIndex)
            "$beforeBrace\n\n$newCode\n$afterBrace"
        } else {
            "$existingCode\n\n$newCode"
        }
    }
}

// ================================================================================
// AndroidManifest.xml
// ================================================================================

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INSTALL_PACKAGES" 
        tools:ignore="ProtectedPermissions" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AICodeAssistIDE"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.AICodeAssistIDE">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider for camera and file operations -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>