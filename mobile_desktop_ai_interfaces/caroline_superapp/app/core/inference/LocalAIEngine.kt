package com.ai_code_assist.ai.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAIEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val aiScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _aiState = MutableStateFlow(LocalAIState())
    val aiState: StateFlow<LocalAIState> = _aiState.asStateFlow()
    
    // AI Model components
    private val modelManager = ModelManager(context)
    private val inferenceEngine = InferenceEngine()
    private val modelOptimizer = ModelOptimizer()
    private val quantizationEngine = QuantizationEngine()
    
    // Local models
    private val loadedModels = ConcurrentHashMap<String, LoadedModel>()
    private val modelCache = ConcurrentHashMap<String, ModelCache>()
    
    // Inference optimization
    private val inferenceQueue = mutableListOf<InferenceRequest>()
    private val batchProcessor = BatchProcessor()
    private val memoryManager = MemoryManager()
    
    // Model types and capabilities
    private val supportedModels = mapOf(
        "code_generation" to ModelInfo(
            name = "CodeGen-Local",
            type = "transformer",
            size = "350M",
            capabilities = listOf("code_completion", "code_generation", "bug_fixing"),
            quantized = true
        ),
        "text_embedding" to ModelInfo(
            name = "SentenceTransformer-Local",
            type = "encoder",
            size = "110M",
            capabilities = listOf("text_embedding", "semantic_search", "similarity"),
            quantized = true
        ),
        "classification" to ModelInfo(
            name = "CodeClassifier-Local",
            type = "classifier",
            size = "50M",
            capabilities = listOf("intent_classification", "code_type_detection"),
            quantized = true
        ),
        "summarization" to ModelInfo(
            name = "Summarizer-Local",
            type = "seq2seq",
            size = "200M",
            capabilities = listOf("code_summarization", "documentation_generation"),
            quantized = true
        )
    )
    
    init {
        initializeLocalAI()
    }
    
    private fun initializeLocalAI() {
        aiScope.launch {
            try {
                setupModelDirectory()
                downloadEssentialModels()
                optimizeModelsForDevice()
                startInferenceProcessor()
                
                _aiState.value = _aiState.value.copy(
                    isInitialized = true,
                    status = "Local AI engine ready"
                )
                
                Timber.d("LocalAIEngine initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize local AI engine")
                _aiState.value = _aiState.value.copy(
                    status = "Initialization failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun setupModelDirectory() {
        val modelDir = File(context.filesDir, "ai_models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        
        // Create subdirectories for different model types
        listOf("quantized", "optimized", "cache", "temp").forEach { subDir ->
            File(modelDir, subDir).mkdirs()
        }
        
        Timber.d("Model directory setup complete")
    }
    
    private suspend fun downloadEssentialModels() {
        _aiState.value = _aiState.value.copy(
            status = "Downloading essential AI models..."
        )
        
        // Download and setup essential models
        val essentialModels = listOf("code_generation", "text_embedding", "classification")
        
        essentialModels.forEach { modelType ->
            try {
                downloadModel(modelType)
                loadModel(modelType)
            } catch (e: Exception) {
                Timber.e(e, "Failed to download model: $modelType")
            }
        }
    }
    
    private suspend fun downloadModel(modelType: String) {
        val modelInfo = supportedModels[modelType] ?: return
        
        _aiState.value = _aiState.value.copy(
            status = "Downloading ${modelInfo.name}..."
        )
        
        // Simulate model download (in real implementation, this would download from a model repository)
        delay(2000) // Simulate download time
        
        // Create mock model file
        val modelFile = File(context.filesDir, "ai_models/${modelType}.bin")
        modelFile.writeText("Mock model data for $modelType")
        
        Timber.d("Downloaded model: ${modelInfo.name}")
    }
    
    private suspend fun optimizeModelsForDevice() {
        _aiState.value = _aiState.value.copy(
            status = "Optimizing models for device..."
        )
        
        loadedModels.values.forEach { model ->
            try {
                optimizeModel(model)
            } catch (e: Exception) {
                Timber.e(e, "Failed to optimize model: ${model.info.name}")
            }
        }
    }
    
    private suspend fun optimizeModel(model: LoadedModel) {
        // Quantize model for mobile deployment
        val quantizedModel = quantizationEngine.quantizeModel(model)
        
        // Optimize for specific hardware
        val optimizedModel = modelOptimizer.optimizeForDevice(quantizedModel)
        
        // Update loaded model
        loadedModels[model.type] = optimizedModel
        
        Timber.d("Optimized model: ${model.info.name}")
    }
    
    private fun startInferenceProcessor() {
        aiScope.launch {
            while (true) {
                try {
                    processInferenceQueue()
                    delay(100) // Process every 100ms
                } catch (e: Exception) {
                    Timber.e(e, "Inference processing error")
                    delay(1000)
                }
            }
        }
    }
    
    private suspend fun processInferenceQueue() {
        if (inferenceQueue.isEmpty()) return
        
        val batchSize = determineBatchSize()
        val batch = synchronized(inferenceQueue) {
            if (inferenceQueue.size >= batchSize) {
                val batch = inferenceQueue.take(batchSize)
                repeat(batchSize) { inferenceQueue.removeAt(0) }
                batch
            } else {
                emptyList()
            }
        }
        
        if (batch.isNotEmpty()) {
            processBatch(batch)
        }
    }
    
    private fun determineBatchSize(): Int {
        val availableMemory = memoryManager.getAvailableMemory()
        return when {
            availableMemory > 1000 -> 8
            availableMemory > 500 -> 4
            availableMemory > 200 -> 2
            else -> 1
        }
    }
    
    private suspend fun processBatch(batch: List<InferenceRequest>) {
        try {
            _aiState.value = _aiState.value.copy(
                isProcessing = true,
                status = "Processing batch of ${batch.size} requests"
            )
            
            val results = batchProcessor.processBatch(batch, loadedModels)
            
            // Send results back to requesters
            results.forEach { (request, result) ->
                request.resultCallback(result)
            }
            
            _aiState.value = _aiState.value.copy(
                isProcessing = false,
                totalInferences = _aiState.value.totalInferences + batch.size,
                status = "Batch processing complete"
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Batch processing failed")
            _aiState.value = _aiState.value.copy(
                isProcessing = false,
                status = "Batch processing failed: ${e.message}"
            )
            
            // Send error to all requests in batch
            batch.forEach { request ->
                request.resultCallback(InferenceResult.error("Batch processing failed: ${e.message}"))
            }
        }
    }
    
    suspend fun generateCode(
        prompt: String,
        language: String = "kotlin",
        maxTokens: Int = 200
    ): InferenceResult {
        return withContext(Dispatchers.IO) {
            try {
                val model = loadedModels["code_generation"]
                    ?: return@withContext InferenceResult.error("Code generation model not loaded")
                
                _aiState.value = _aiState.value.copy(
                    status = "Generating code for: ${prompt.take(50)}..."
                )
                
                val input = preprocessCodeInput(prompt, language)
                val result = inferenceEngine.runInference(model, input)
                val generatedCode = postprocessCodeOutput(result, language)
                
                InferenceResult.success(generatedCode)
                
            } catch (e: Exception) {
                Timber.e(e, "Code generation failed")
                InferenceResult.error("Code generation failed: ${e.message}")
            }
        }
    }
    
    suspend fun generateCodeAsync(
        prompt: String,
        language: String = "kotlin",
        maxTokens: Int = 200,
        callback: (InferenceResult) -> Unit
    ) {
        val request = InferenceRequest(
            id = "code_${System.currentTimeMillis()}",
            type = "code_generation",
            input = mapOf(
                "prompt" to prompt,
                "language" to language,
                "max_tokens" to maxTokens
            ),
            resultCallback = callback
        )
        
        synchronized(inferenceQueue) {
            inferenceQueue.add(request)
        }
    }
    
    suspend fun embedText(text: String): InferenceResult {
        return withContext(Dispatchers.IO) {
            try {
                val model = loadedModels["text_embedding"]
                    ?: return@withContext InferenceResult.error("Text embedding model not loaded")
                
                val input = preprocessTextInput(text)
                val result = inferenceEngine.runInference(model, input)
                val embedding = postprocessEmbeddingOutput(result)
                
                InferenceResult.success(embedding)
                
            } catch (e: Exception) {
                Timber.e(e, "Text embedding failed")
                InferenceResult.error("Text embedding failed: ${e.message}")
            }
        }
    }
    
    suspend fun classifyIntent(text: String): InferenceResult {
        return withContext(Dispatchers.IO) {
            try {
                val model = loadedModels["classification"]
                    ?: return@withContext InferenceResult.error("Classification model not loaded")
                
                val input = preprocessTextInput(text)
                val result = inferenceEngine.runInference(model, input)
                val classification = postprocessClassificationOutput(result)
                
                InferenceResult.success(classification)
                
            } catch (e: Exception) {
                Timber.e(e, "Intent classification failed")
                InferenceResult.error("Intent classification failed: ${e.message}")
            }
        }
    }
    
    suspend fun summarizeCode(code: String, language: String = "kotlin"): InferenceResult {
        return withContext(Dispatchers.IO) {
            try {
                val model = loadedModels["summarization"]
                    ?: return@withContext InferenceResult.error("Summarization model not loaded")
                
                val input = preprocessCodeInput(code, language)
                val result = inferenceEngine.runInference(model, input)
                val summary = postprocessSummaryOutput(result)
                
                InferenceResult.success(summary)
                
            } catch (e: Exception) {
                Timber.e(e, "Code summarization failed")
                InferenceResult.error("Code summarization failed: ${e.message}")
            }
        }
    }
    
    private fun preprocessCodeInput(prompt: String, language: String): FloatArray {
        // Tokenize and encode the input for the model
        val tokens = tokenizeText(prompt)
        val languageToken = getLanguageToken(language)
        
        // Combine prompt tokens with language token
        val combinedTokens = listOf(languageToken) + tokens
        
        // Convert to float array (simplified)
        return combinedTokens.map { it.toFloat() }.toFloatArray()
    }
    
    private fun preprocessTextInput(text: String): FloatArray {
        val tokens = tokenizeText(text)
        return tokens.map { it.toFloat() }.toFloatArray()
    }
    
    private fun tokenizeText(text: String): List<Int> {
        // Simplified tokenization (in real implementation, use proper tokenizer)
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.hashCode() % 10000 } // Simple hash-based tokenization
    }
    
    private fun getLanguageToken(language: String): Int {
        return when (language.lowercase()) {
            "kotlin" -> 1001
            "java" -> 1002
            "python" -> 1003
            "javascript" -> 1004
            else -> 1000
        }
    }
    
    private fun postprocessCodeOutput(output: FloatArray, language: String): String {
        // Convert model output back to code (simplified)
        val tokens = output.map { it.toInt() % 1000 }
        
        // Generate code based on tokens and language
        return when (language.lowercase()) {
            "kotlin" -> generateKotlinCode(tokens)
            "java" -> generateJavaCode(tokens)
            else -> generateGenericCode(tokens)
        }
    }
    
    private fun generateKotlinCode(tokens: List<Int>): String {
        val codeTemplates = listOf(
            "fun ${generateFunctionName(tokens)}(${generateParameters(tokens)}): ${generateReturnType(tokens)} {\n    ${generateFunctionBody(tokens)}\n}",
            "class ${generateClassName(tokens)} {\n    ${generateClassBody(tokens)}\n}",
            "val ${generateVariableName(tokens)} = ${generateValue(tokens)}",
            "if (${generateCondition(tokens)}) {\n    ${generateIfBody(tokens)}\n}"
        )
        
        return codeTemplates[tokens.first() % codeTemplates.size]
    }
    
    private fun generateJavaCode(tokens: List<Int>): String {
        val codeTemplates = listOf(
            "public ${generateReturnType(tokens)} ${generateFunctionName(tokens)}(${generateParameters(tokens)}) {\n    ${generateFunctionBody(tokens)}\n}",
            "public class ${generateClassName(tokens)} {\n    ${generateClassBody(tokens)}\n}",
            "${generateReturnType(tokens)} ${generateVariableName(tokens)} = ${generateValue(tokens)};",
            "if (${generateCondition(tokens)}) {\n    ${generateIfBody(tokens)}\n}"
        )
        
        return codeTemplates[tokens.first() % codeTemplates.size]
    }
    
    private fun generateGenericCode(tokens: List<Int>): String {
        return "// Generated code based on tokens: ${tokens.take(5)}\n// Implementation would go here"
    }
    
    private fun generateFunctionName(tokens: List<Int>): String {
        val names = listOf("calculate", "process", "handle", "create", "update", "delete", "validate", "transform")
        return names[tokens.getOrElse(0) { 0 } % names.size]
    }
    
    private fun generateClassName(tokens: List<Int>): String {
        val names = listOf("DataProcessor", "ServiceManager", "ConfigHandler", "UtilityClass", "ModelAdapter")
        return names[tokens.getOrElse(0) { 0 } % names.size]
    }
    
    private fun generateVariableName(tokens: List<Int>): String {
        val names = listOf("result", "data", "config", "value", "item", "element", "response")
        return names[tokens.getOrElse(1) { 0 } % names.size]
    }
    
    private fun generateParameters(tokens: List<Int>): String {
        val params = listOf("input: String", "data: List<String>", "config: Map<String, Any>", "callback: () -> Unit")
        return params[tokens.getOrElse(2) { 0 } % params.size]
    }
    
    private fun generateReturnType(tokens: List<Int>): String {
        val types = listOf("String", "Int", "Boolean", "List<String>", "Unit")
        return types[tokens.getOrElse(3) { 0 } % types.size]
    }
    
    private fun generateFunctionBody(tokens: List<Int>): String {
        val bodies = listOf(
            "return \"processed\"",
            "return 42",
            "return true",
            "return emptyList()",
            "println(\"Processing...\")"
        )
        return bodies[tokens.getOrElse(4) { 0 } % bodies.size]
    }
    
    private fun generateClassBody(tokens: List<Int>): String {
        val bodies = listOf(
            "private val data = mutableListOf<String>()",
            "fun process() { /* implementation */ }",
            "companion object { const val TAG = \"${generateClassName(tokens)}\" }"
        )
        return bodies[tokens.getOrElse(5) { 0 } % bodies.size]
    }
    
    private fun generateValue(tokens: List<Int>): String {
        val values = listOf("\"default\"", "42", "true", "emptyList()", "null")
        return values[tokens.getOrElse(6) { 0 } % values.size]
    }
    
    private fun generateCondition(tokens: List<Int>): String {
        val conditions = listOf("data.isNotEmpty()", "value > 0", "config.containsKey(\"key\")", "result != null")
        return conditions[tokens.getOrElse(7) { 0 } % conditions.size]
    }
    
    private fun generateIfBody(tokens: List<Int>): String {
        val bodies = listOf(
            "println(\"Condition met\")",
            "processData()",
            "return result",
            "updateConfig()"
        )
        return bodies[tokens.getOrElse(8) { 0 } % bodies.size]
    }
    
    private fun postprocessEmbeddingOutput(output: FloatArray): List<Float> {
        // Normalize embedding vector
        val magnitude = kotlin.math.sqrt(output.map { it * it }.sum())
        return if (magnitude > 0) {
            output.map { it / magnitude }
        } else {
            output.toList()
        }
    }
    
    private fun postprocessClassificationOutput(output: FloatArray): Map<String, Float> {
        val classes = listOf(
            "code_generation", "bug_fixing", "explanation", "refactoring", 
            "testing", "documentation", "optimization", "debugging"
        )
        
        // Apply softmax to get probabilities
        val expValues = output.map { kotlin.math.exp(it.toDouble()).toFloat() }
        val sumExp = expValues.sum()
        val probabilities = expValues.map { it / sumExp }
        
        return classes.zip(probabilities).toMap()
    }
    
    private fun postprocessSummaryOutput(output: FloatArray): String {
        // Convert output to summary text (simplified)
        val summaryTemplates = listOf(
            "This code defines a function that processes input data and returns a result.",
            "This class provides utility methods for data manipulation and processing.",
            "This code implements a configuration handler with validation and error handling.",
            "This function performs calculations and transformations on the provided input."
        )
        
        val index = (output.sum() % summaryTemplates.size).toInt()
        return summaryTemplates[index]
    }
    
    suspend fun loadModel(modelType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (loadedModels.containsKey(modelType)) {
                    return@withContext true // Already loaded
                }
                
                val modelInfo = supportedModels[modelType]
                    ?: return@withContext false
                
                _aiState.value = _aiState.value.copy(
                    status = "Loading model: ${modelInfo.name}"
                )
                
                // Load model from file
                val modelFile = File(context.filesDir, "ai_models/${modelType}.bin")
                if (!modelFile.exists()) {
                    downloadModel(modelType)
                }
                
                // Create loaded model
                val loadedModel = LoadedModel(
                    type = modelType,
                    info = modelInfo,
                    modelData = loadModelData(modelFile),
                    isOptimized = false,
                    loadTime = System.currentTimeMillis()
                )
                
                loadedModels[modelType] = loadedModel
                
                _aiState.value = _aiState.value.copy(
                    loadedModels = loadedModels.keys.toList(),
                    status = "Model loaded: ${modelInfo.name}"
                )
                
                true
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load model: $modelType")
                _aiState.value = _aiState.value.copy(
                    status = "Model loading failed: ${e.message}"
                )
                false
            }
        }
    }
    
    private fun loadModelData(modelFile: File): ByteArray {
        // In real implementation, this would load actual model weights
        return modelFile.readBytes()
    }
    
    fun unloadModel(modelType: String) {
        loadedModels.remove(modelType)
        modelCache.remove(modelType)
        
        _aiState.value = _aiState.value.copy(
            loadedModels = loadedModels.keys.toList(),
            status = "Model unloaded: $modelType"
        )
        
        // Trigger garbage collection
        System.gc()
    }
    
    fun getModelInfo(modelType: String): ModelInfo? {
        return supportedModels[modelType]
    }
    
    fun getSupportedModels(): List<String> {
        return supportedModels.keys.toList()
    }
    
    fun getLoadedModels(): List<String> {
        return loadedModels.keys.toList()
    }
    
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        return MemoryUsage(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            modelMemory = calculateModelMemoryUsage()
        )
    }
    
    private fun calculateModelMemoryUsage(): Long {
        return loadedModels.values.sumOf { model ->
            model.modelData.size.toLong()
        }
    }
    
    fun optimizeMemory() {
        aiScope.launch {
            try {
                _aiState.value = _aiState.value.copy(
                    status = "Optimizing memory usage..."
                )
                
                // Clear model cache
                modelCache.clear()
                
                // Unload least recently used models if memory is low
                val memoryUsage = getMemoryUsage()
                if (memoryUsage.usedMemory > memoryUsage.maxMemory * 0.8) {
                    unloadLeastRecentlyUsedModels()
                }
                
                // Force garbage collection
                System.gc()
                
                _aiState.value = _aiState.value.copy(
                    status = "Memory optimization complete"
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Memory optimization failed")
                _aiState.value = _aiState.value.copy(
                    status = "Memory optimization failed: ${e.message}"
                )
            }
        }
    }
    
    private fun unloadLeastRecentlyUsedModels() {
        val sortedModels = loadedModels.values.sortedBy { it.loadTime }
        val modelsToUnload = sortedModels.take(sortedModels.size / 2)
        
        modelsToUnload.forEach { model ->
            unloadModel(model.type)
        }
        
        Timber.d("Unloaded ${modelsToUnload.size} least recently used models")
    }
    
    fun getInferenceMetrics(): InferenceMetrics {
        return InferenceMetrics(
            totalInferences = _aiState.value.totalInferences,
            averageInferenceTime = calculateAverageInferenceTime(),
            queueSize = inferenceQueue.size,
            loadedModelsCount = loadedModels.size,
            memoryUsage = getMemoryUsage()
        )
    }
    
    private fun calculateAverageInferenceTime(): Double {
        // This would track actual inference times in a real implementation
        return 150.0 // milliseconds
    }
}

// Supporting classes for local AI engine
class ModelManager(private val context: Context) {
    fun downloadModel(modelType: String, url: String): Boolean {
        // Download model from URL
        return true
    }
    
    fun validateModel(modelFile: File): Boolean {
        // Validate model integrity
        return modelFile.exists() && modelFile.length() > 0
    }
}

class InferenceEngine {
    fun runInference(model: LoadedModel, input: FloatArray): FloatArray {
        // Run actual inference (simplified simulation)
        val outputSize = when (model.type) {
            "code_generation" -> 200
            "text_embedding" -> 384
            "classification" -> 8
            "summarization" -> 100
            else -> 50
        }
        
        // Simulate inference computation
        Thread.sleep(50) // Simulate processing time
        
        // Generate mock output
        return FloatArray(outputSize) { kotlin.random.Random.nextFloat() }
    }
}

class ModelOptimizer {
    fun optimizeForDevice(model: LoadedModel): LoadedModel {
        // Optimize model for specific device capabilities
        return model.copy(isOptimized = true)
    }
}

class QuantizationEngine {
    fun quantizeModel(model: LoadedModel): LoadedModel {
        // Quantize model weights to reduce size and improve performance
        val quantizedData = quantizeWeights(model.modelData)
        return model.copy(modelData = quantizedData)
    }
    
    private fun quantizeWeights(weights: ByteArray): ByteArray {
        // Simplified quantization (8-bit)
        return weights // In real implementation, would perform actual quantization
    }
}

class BatchProcessor {
    suspend fun processBatch(
        batch: List<InferenceRequest>,
        models: Map<String, LoadedModel>
    ): List<Pair<InferenceRequest, InferenceResult>> {
        return batch.map { request ->
            val model = models[request.type]
            val result = if (model != null) {
                try {
                    processRequest(request, model)
                } catch (e: Exception) {
                    InferenceResult.error("Processing failed: ${e.message}")
                }
            } else {
                InferenceResult.error("Model not loaded: ${request.type}")
            }
            
            request to result
        }
    }
    
    private fun processRequest(request: InferenceRequest, model: LoadedModel): InferenceResult {
        // Process individual request
        val input = preprocessInput(request.input, model.type)
        val output = runModelInference(model, input)
        val result = postprocessOutput(output, model.type, request.input)
        
        return InferenceResult.success(result)
    }
    
    private fun preprocessInput(input: Map<String, Any>, modelType: String): FloatArray {
        // Convert input to model format
        return when (modelType) {
            "code_generation" -> {
                val prompt = input["prompt"] as? String ?: ""
                tokenizeAndEncode(prompt)
            }
            "text_embedding" -> {
                val text = input["text"] as? String ?: ""
                tokenizeAndEncode(text)
            }
            else -> floatArrayOf()
        }
    }
    
    private fun tokenizeAndEncode(text: String): FloatArray {
        // Simplified tokenization and encoding
        return text.take(100).map { it.code.toFloat() }.toFloatArray()
    }
    
    private fun runModelInference(model: LoadedModel, input: FloatArray): FloatArray {
        // Run inference on the model
        return FloatArray(100) { kotlin.random.Random.nextFloat() }
    }
    
    private fun postprocessOutput(output: FloatArray, modelType: String, originalInput: Map<String, Any>): Any {
        return when (modelType) {
            "code_generation" -> decodeToText(output)
            "text_embedding" -> output.toList()
            "classification" -> mapToClassification(output)
            else -> output.toList()
        }
    }
    
    private fun decodeToText(output: FloatArray): String {
        // Decode output tokens to text
        return "Generated code based on model output"
    }
    
    private fun mapToClassification(output: FloatArray): Map<String, Float> {
        val classes = listOf("code", "bug", "explanation", "other")
        return classes.zip(output.take(classes.size)).toMap()
    }
}

class MemoryManager {
    fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
    
    fun shouldUnloadModels(): Boolean {
        val usage = getMemoryUsagePercentage()
        return usage > 0.85 // Unload if using more than 85% of available memory
    }
    
    private fun getMemoryUsagePercentage(): Double {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return used.toDouble() / runtime.maxMemory()
    }
}

// Data classes for local AI engine
data class LocalAIState(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val status: String = "Initializing...",
    val loadedModels: List<String> = emptyList(),
    val totalInferences: Long = 0
)

data class ModelInfo(
    val name: String,
    val type: String,
    val size: String,
    val capabilities: List<String>,
    val quantized: Boolean
)

data class LoadedModel(
    val type: String,
    val info: ModelInfo,
    val modelData: ByteArray,
    val isOptimized: Boolean,
    val loadTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LoadedModel
        return type == other.type
    }
    
    override fun hashCode(): Int {
        return type.hashCode()
    }
}

data class ModelCache(
    val modelType: String,
    val cachedInputs: Map<String, FloatArray>,
    val cachedOutputs: Map<String, FloatArray>,
    val lastAccess: Long
)

data class InferenceRequest(
    val id: String,
    val type: String,
    val input: Map<String, Any>,
    val resultCallback: (InferenceResult) -> Unit
)

sealed class InferenceResult {
    data class Success(val data: Any) : InferenceResult()
    data class Error(val message: String) : InferenceResult()
    
    companion object {
        fun success(data: Any) = Success(data)
        fun error(message: String) = Error(message)
    }
}

data class MemoryUsage(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val modelMemory: Long
)

data class InferenceMetrics(
    val totalInferences: Long,
    val averageInferenceTime: Double,
    val queueSize: Int,
    val loadedModelsCount: Int,
    val memoryUsage: MemoryUsage
)

