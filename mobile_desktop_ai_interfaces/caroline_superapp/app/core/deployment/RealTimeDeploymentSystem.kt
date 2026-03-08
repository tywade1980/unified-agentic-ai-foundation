package com.ai_code_assist.deployment

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTimeDeploymentSystem @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val deploymentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _deploymentState = MutableStateFlow(DeploymentState())
    val deploymentState: StateFlow<DeploymentState> = _deploymentState.asStateFlow()
    
    private val connectedDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val deploymentQueue = mutableListOf<DeploymentTask>()
    private val hotReloadSessions = ConcurrentHashMap<String, HotReloadSession>()
    
    // ADB and deployment tools
    private var adbPath: String? = null
    private var isAdbAvailable = false
    
    init {
        initializeDeploymentSystem()
    }
    
    private fun initializeDeploymentSystem() {
        deploymentScope.launch {
            try {
                setupAdbConnection()
                startDeviceDiscovery()
                startDeploymentProcessor()
                
                _deploymentState.value = _deploymentState.value.copy(
                    isInitialized = true,
                    status = "Deployment system ready"
                )
                
                Timber.d("RealTimeDeploymentSystem initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize deployment system")
                _deploymentState.value = _deploymentState.value.copy(
                    status = "Initialization failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun setupAdbConnection() {
        try {
            // Try to find ADB in common locations
            val possiblePaths = listOf(
                "/system/bin/adb",
                "/system/xbin/adb",
                "${context.filesDir}/adb",
                "${context.cacheDir}/adb"
            )
            
            for (path in possiblePaths) {
                if (File(path).exists()) {
                    adbPath = path
                    isAdbAvailable = true
                    break
                }
            }
            
            if (!isAdbAvailable) {
                // Try to extract ADB from assets or download it
                extractAdbFromAssets()
            }
            
            if (isAdbAvailable) {
                // Test ADB connection
                val result = executeAdbCommand("devices")
                Timber.d("ADB connection test: $result")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to setup ADB connection")
            // Fall back to alternative deployment methods
            setupAlternativeDeployment()
        }
    }
    
    private suspend fun extractAdbFromAssets() {
        try {
            val adbFile = File(context.filesDir, "adb")
            
            // Copy ADB binary from assets (if available)
            context.assets.open("adb").use { input ->
                adbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Make executable
            adbFile.setExecutable(true)
            adbPath = adbFile.absolutePath
            isAdbAvailable = true
            
            Timber.d("ADB extracted from assets")
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract ADB from assets")
        }
    }
    
    private fun setupAlternativeDeployment() {
        // Setup alternative deployment methods
        // - Network deployment
        // - USB deployment via intent
        // - Wireless debugging
        Timber.d("Setting up alternative deployment methods")
    }
    
    private fun startDeviceDiscovery() {
        deploymentScope.launch {
            while (true) {
                try {
                    discoverConnectedDevices()
                    delay(5000) // Check every 5 seconds
                } catch (e: Exception) {
                    Timber.e(e, "Device discovery error")
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    private suspend fun discoverConnectedDevices() {
        if (!isAdbAvailable) return
        
        try {
            val result = executeAdbCommand("devices -l")
            parseDeviceList(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to discover devices")
        }
    }
    
    private fun parseDeviceList(deviceOutput: String) {
        val currentDevices = mutableSetOf<String>()
        
        deviceOutput.lines().forEach { line ->
            if (line.contains("\tdevice")) {
                val parts = line.split("\t")
                if (parts.size >= 2) {
                    val deviceId = parts[0]
                    currentDevices.add(deviceId)
                    
                    if (!connectedDevices.containsKey(deviceId)) {
                        val deviceInfo = DeviceInfo(
                            id = deviceId,
                            name = extractDeviceName(line),
                            model = extractDeviceModel(line),
                            isConnected = true,
                            connectionType = if (deviceId.contains(":")) "WiFi" else "USB",
                            lastSeen = System.currentTimeMillis()
                        )
                        
                        connectedDevices[deviceId] = deviceInfo
                        onDeviceConnected(deviceInfo)
                    }
                }
            }
        }
        
        // Remove disconnected devices
        val disconnectedDevices = connectedDevices.keys - currentDevices
        disconnectedDevices.forEach { deviceId ->
            val device = connectedDevices.remove(deviceId)
            if (device != null) {
                onDeviceDisconnected(device)
            }
        }
        
        updateDeploymentState()
    }
    
    private fun extractDeviceName(deviceLine: String): String {
        return deviceLine.substringAfter("device:", "Unknown Device")
    }
    
    private fun extractDeviceModel(deviceLine: String): String {
        return deviceLine.substringAfter("model:", "Unknown Model")
    }
    
    private fun onDeviceConnected(device: DeviceInfo) {
        Timber.d("Device connected: ${device.name} (${device.id})")
        
        deploymentScope.launch {
            // Setup hot reload for this device
            setupHotReload(device)
            
            // Notify UI
            _deploymentState.value = _deploymentState.value.copy(
                status = "Device connected: ${device.name}"
            )
        }
    }
    
    private fun onDeviceDisconnected(device: DeviceInfo) {
        Timber.d("Device disconnected: ${device.name} (${device.id})")
        
        // Clean up hot reload session
        hotReloadSessions.remove(device.id)
        
        _deploymentState.value = _deploymentState.value.copy(
            status = "Device disconnected: ${device.name}"
        )
    }
    
    private suspend fun setupHotReload(device: DeviceInfo) {
        try {
            val session = HotReloadSession(
                deviceId = device.id,
                isActive = true,
                lastSync = System.currentTimeMillis()
            )
            
            hotReloadSessions[device.id] = session
            
            // Setup file watching for hot reload
            startFileWatcher(device.id)
            
            Timber.d("Hot reload setup for device: ${device.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to setup hot reload for device: ${device.id}")
        }
    }
    
    private fun startFileWatcher(deviceId: String) {
        deploymentScope.launch {
            // Watch for file changes in the project
            // This would integrate with the IDE's file system watcher
            while (hotReloadSessions.containsKey(deviceId)) {
                try {
                    // Check for file changes
                    // If changes detected, trigger hot reload
                    delay(1000) // Check every second
                } catch (e: Exception) {
                    Timber.e(e, "File watcher error for device: $deviceId")
                    break
                }
            }
        }
    }
    
    fun deployApp(appPath: String, targetDevices: List<String> = emptyList()) {
        deploymentScope.launch {
            try {
                val devices = if (targetDevices.isEmpty()) {
                    connectedDevices.keys.toList()
                } else {
                    targetDevices.filter { connectedDevices.containsKey(it) }
                }
                
                if (devices.isEmpty()) {
                    _deploymentState.value = _deploymentState.value.copy(
                        status = "No devices available for deployment"
                    )
                    return@launch
                }
                
                _deploymentState.value = _deploymentState.value.copy(
                    isDeploying = true,
                    status = "Starting deployment to ${devices.size} device(s)"
                )
                
                val deploymentResults = mutableListOf<DeploymentResult>()
                
                devices.forEach { deviceId ->
                    try {
                        val result = deployToDevice(appPath, deviceId)
                        deploymentResults.add(result)
                    } catch (e: Exception) {
                        Timber.e(e, "Deployment failed for device: $deviceId")
                        deploymentResults.add(
                            DeploymentResult(
                                deviceId = deviceId,
                                success = false,
                                error = e.message ?: "Unknown error",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                val successCount = deploymentResults.count { it.success }
                val totalCount = deploymentResults.size
                
                _deploymentState.value = _deploymentState.value.copy(
                    isDeploying = false,
                    lastDeploymentResults = deploymentResults,
                    status = "Deployment complete: $successCount/$totalCount successful"
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Deployment process failed")
                _deploymentState.value = _deploymentState.value.copy(
                    isDeploying = false,
                    status = "Deployment failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun deployToDevice(appPath: String, deviceId: String): DeploymentResult {
        return try {
            _deploymentState.value = _deploymentState.value.copy(
                status = "Installing on device: $deviceId"
            )
            
            // Install APK using ADB
            val installResult = executeAdbCommand("-s $deviceId install -r \"$appPath\"")
            
            if (installResult.contains("Success")) {
                // Launch the app
                val packageName = extractPackageName(appPath)
                if (packageName != null) {
                    launchApp(deviceId, packageName)
                }
                
                DeploymentResult(
                    deviceId = deviceId,
                    success = true,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                throw Exception("Installation failed: $installResult")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to deploy to device: $deviceId")
            DeploymentResult(
                deviceId = deviceId,
                success = false,
                error = e.message ?: "Unknown error",
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun launchApp(deviceId: String, packageName: String) {
        try {
            val launchIntent = "$packageName/.MainActivity"
            executeAdbCommand("-s $deviceId shell am start -n $launchIntent")
            Timber.d("App launched on device: $deviceId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch app on device: $deviceId")
        }
    }
    
    private fun extractPackageName(apkPath: String): String? {
        // Extract package name from APK
        // This would use aapt or similar tool
        return "com.ai_code_assist" // Default for now
    }
    
    fun enableHotReload(deviceId: String) {
        hotReloadSessions[deviceId]?.let { session ->
            hotReloadSessions[deviceId] = session.copy(isActive = true)
            startFileWatcher(deviceId)
            Timber.d("Hot reload enabled for device: $deviceId")
        }
    }
    
    fun disableHotReload(deviceId: String) {
        hotReloadSessions[deviceId]?.let { session ->
            hotReloadSessions[deviceId] = session.copy(isActive = false)
            Timber.d("Hot reload disabled for device: $deviceId")
        }
    }
    
    fun triggerHotReload(deviceId: String, changedFiles: List<String>) {
        deploymentScope.launch {
            try {
                val session = hotReloadSessions[deviceId]
                if (session?.isActive != true) return@launch
                
                _deploymentState.value = _deploymentState.value.copy(
                    status = "Hot reloading changes to device: $deviceId"
                )
                
                // Create incremental update package
                val updatePackage = createIncrementalUpdate(changedFiles)
                
                // Send update to device
                sendHotReloadUpdate(deviceId, updatePackage)
                
                hotReloadSessions[deviceId] = session.copy(lastSync = System.currentTimeMillis())
                
                _deploymentState.value = _deploymentState.value.copy(
                    status = "Hot reload complete for device: $deviceId"
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Hot reload failed for device: $deviceId")
                _deploymentState.value = _deploymentState.value.copy(
                    status = "Hot reload failed: ${e.message}"
                )
            }
        }
    }
    
    private fun createIncrementalUpdate(changedFiles: List<String>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            changedFiles.forEach { filePath ->
                try {
                    val file = File(filePath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to add file to update package: $filePath")
                }
            }
        }
        return outputStream.toByteArray()
    }
    
    private suspend fun sendHotReloadUpdate(deviceId: String, updatePackage: ByteArray) {
        try {
            // Send update package to device via ADB or network
            val tempFile = File(context.cacheDir, "hot_reload_${System.currentTimeMillis()}.zip")
            tempFile.writeBytes(updatePackage)
            
            // Push to device
            executeAdbCommand("-s $deviceId push \"${tempFile.absolutePath}\" /sdcard/hot_reload.zip")
            
            // Trigger reload in the app
            executeAdbCommand("-s $deviceId shell am broadcast -a com.ai_code_assist.HOT_RELOAD")
            
            tempFile.delete()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to send hot reload update")
            throw e
        }
    }
    
    private fun startDeploymentProcessor() {
        deploymentScope.launch {
            while (true) {
                try {
                    processDeploymentQueue()
                    delay(1000)
                } catch (e: Exception) {
                    Timber.e(e, "Deployment processor error")
                    delay(5000)
                }
            }
        }
    }
    
    private fun processDeploymentQueue() {
        synchronized(deploymentQueue) {
            if (deploymentQueue.isNotEmpty()) {
                val task = deploymentQueue.removeAt(0)
                deploymentScope.launch {
                    executeDeploymentTask(task)
                }
            }
        }
    }
    
    private suspend fun executeDeploymentTask(task: DeploymentTask) {
        try {
            when (task.type) {
                DeploymentTaskType.INSTALL_APK -> {
                    deployApp(task.appPath, task.targetDevices)
                }
                DeploymentTaskType.HOT_RELOAD -> {
                    task.targetDevices.forEach { deviceId ->
                        triggerHotReload(deviceId, task.changedFiles)
                    }
                }
                DeploymentTaskType.UNINSTALL_APP -> {
                    uninstallApp(task.packageName, task.targetDevices)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute deployment task: ${task.type}")
        }
    }
    
    private suspend fun uninstallApp(packageName: String, targetDevices: List<String>) {
        targetDevices.forEach { deviceId ->
            try {
                executeAdbCommand("-s $deviceId uninstall $packageName")
                Timber.d("App uninstalled from device: $deviceId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to uninstall app from device: $deviceId")
            }
        }
    }
    
    private suspend fun executeAdbCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val fullCommand = "$adbPath $command"
                val process = Runtime.getRuntime().exec(fullCommand)
                
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                
                process.waitFor()
                
                if (process.exitValue() == 0) {
                    output
                } else {
                    throw Exception("ADB command failed: $error")
                }
            } catch (e: Exception) {
                Timber.e(e, "ADB command execution failed: $command")
                throw e
            }
        }
    }
    
    private fun updateDeploymentState() {
        _deploymentState.value = _deploymentState.value.copy(
            connectedDevices = connectedDevices.values.toList(),
            activeHotReloadSessions = hotReloadSessions.size
        )
    }
    
    fun getConnectedDevices(): List<DeviceInfo> {
        return connectedDevices.values.toList()
    }
    
    fun getDeploymentHistory(): List<DeploymentResult> {
        return _deploymentState.value.lastDeploymentResults
    }
    
    fun clearDeploymentHistory() {
        _deploymentState.value = _deploymentState.value.copy(
            lastDeploymentResults = emptyList()
        )
    }
}

// Data classes for deployment system
data class DeploymentState(
    val isInitialized: Boolean = false,
    val isDeploying: Boolean = false,
    val status: String = "Initializing...",
    val connectedDevices: List<DeviceInfo> = emptyList(),
    val activeHotReloadSessions: Int = 0,
    val lastDeploymentResults: List<DeploymentResult> = emptyList()
)

data class DeviceInfo(
    val id: String,
    val name: String,
    val model: String,
    val isConnected: Boolean,
    val connectionType: String, // USB, WiFi
    val lastSeen: Long
)

data class DeploymentResult(
    val deviceId: String,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Long
)

data class HotReloadSession(
    val deviceId: String,
    val isActive: Boolean,
    val lastSync: Long
)

data class DeploymentTask(
    val type: DeploymentTaskType,
    val appPath: String = "",
    val packageName: String = "",
    val targetDevices: List<String> = emptyList(),
    val changedFiles: List<String> = emptyList()
)

enum class DeploymentTaskType {
    INSTALL_APK,
    HOT_RELOAD,
    UNINSTALL_APP
}

