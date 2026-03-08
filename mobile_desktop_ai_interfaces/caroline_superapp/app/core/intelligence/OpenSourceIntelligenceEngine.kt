package com.ai_code_assist.intelligence

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenSourceIntelligenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val intelligenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _intelligenceState = MutableStateFlow(IntelligenceState())
    val intelligenceState: StateFlow<IntelligenceState> = _intelligenceState.asStateFlow()
    
    // Intelligence gathering components
    private val githubIntelligence = GitHubIntelligence()
    private val stackOverflowIntelligence = StackOverflowIntelligence()
    private val documentationIntelligence = DocumentationIntelligence()
    private val codeAnalysisIntelligence = CodeAnalysisIntelligence()
    private val trendAnalyzer = TrendAnalyzer()
    
    // HTTP client for API calls
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // Intelligence data storage
    private val intelligenceCache = ConcurrentHashMap<String, IntelligenceData>()
    private val trendData = ConcurrentHashMap<String, TrendData>()
    private val knowledgeBase = ConcurrentHashMap<String, KnowledgeEntry>()
    
    // Intelligence sources
    private val intelligenceSources = mapOf(
        "github" to IntelligenceSource(
            id = "github",
            name = "GitHub",
            baseUrl = "https://api.github.com",
            type = SourceType.CODE_REPOSITORY,
            capabilities = listOf("code_search", "repository_analysis", "trend_detection"),
            rateLimitPerHour = 5000
        ),
        "stackoverflow" to IntelligenceSource(
            id = "stackoverflow",
            name = "Stack Overflow",
            baseUrl = "https://api.stackexchange.com/2.3",
            type = SourceType.Q_AND_A,
            capabilities = listOf("question_search", "answer_analysis", "tag_trends"),
            rateLimitPerHour = 10000
        ),
        "android_docs" to IntelligenceSource(
            id = "android_docs",
            name = "Android Documentation",
            baseUrl = "https://developer.android.com",
            type = SourceType.DOCUMENTATION,
            capabilities = listOf("api_reference", "guide_search", "sample_code"),
            rateLimitPerHour = 1000
        ),
        "kotlin_docs" to IntelligenceSource(
            id = "kotlin_docs",
            name = "Kotlin Documentation",
            baseUrl = "https://kotlinlang.org",
            type = SourceType.DOCUMENTATION,
            capabilities = listOf("language_reference", "stdlib_docs", "examples"),
            rateLimitPerHour = 1000
        ),
        "maven_central" to IntelligenceSource(
            id = "maven_central",
            name = "Maven Central",
            baseUrl = "https://search.maven.org",
            type = SourceType.PACKAGE_REGISTRY,
            capabilities = listOf("library_search", "dependency_analysis", "version_tracking"),
            rateLimitPerHour = 2000
        )
    )
    
    init {
        initializeIntelligenceEngine()
    }
    
    private fun initializeIntelligenceEngine() {
        intelligenceScope.launch {
            try {
                setupIntelligenceSources()
                loadKnowledgeBase()
                startIntelligenceGathering()
                startTrendAnalysis()
                
                _intelligenceState.value = _intelligenceState.value.copy(
                    isInitialized = true,
                    status = "Open source intelligence engine ready"
                )
                
                Timber.d("OpenSourceIntelligenceEngine initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize intelligence engine")
                _intelligenceState.value = _intelligenceState.value.copy(
                    status = "Initialization failed: ${e.message}"
                )
            }
        }
    }
    
    private fun setupIntelligenceSources() {
        intelligenceSources.values.forEach { source ->
            when (source.type) {
                SourceType.CODE_REPOSITORY -> githubIntelligence.initialize(source)
                SourceType.Q_AND_A -> stackOverflowIntelligence.initialize(source)
                SourceType.DOCUMENTATION -> documentationIntelligence.initialize(source)
                SourceType.PACKAGE_REGISTRY -> {} // Initialize package registry intelligence
            }
        }
        
        Timber.d("Setup ${intelligenceSources.size} intelligence sources")
    }
    
    private suspend fun loadKnowledgeBase() {
        // Load existing knowledge base from local storage
        loadAndroidKnowledge()
        loadKotlinKnowledge()
        loadCommonPatterns()
        loadBestPractices()
    }
    
    private fun loadAndroidKnowledge() {
        knowledgeBase["android_architecture"] = KnowledgeEntry(
            id = "android_architecture",
            title = "Android Architecture Patterns",
            content = "MVVM, MVP, MVI patterns for Android development",
            category = "architecture",
            tags = listOf("android", "architecture", "mvvm", "mvp", "mvi"),
            confidence = 0.9,
            lastUpdated = System.currentTimeMillis()
        )
        
        knowledgeBase["jetpack_compose"] = KnowledgeEntry(
            id = "jetpack_compose",
            title = "Jetpack Compose UI Framework",
            content = "Modern UI toolkit for Android development",
            category = "ui",
            tags = listOf("android", "compose", "ui", "declarative"),
            confidence = 0.95,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun loadKotlinKnowledge() {
        knowledgeBase["kotlin_coroutines"] = KnowledgeEntry(
            id = "kotlin_coroutines",
            title = "Kotlin Coroutines",
            content = "Asynchronous programming with coroutines",
            category = "concurrency",
            tags = listOf("kotlin", "coroutines", "async", "concurrency"),
            confidence = 0.9,
            lastUpdated = System.currentTimeMillis()
        )
        
        knowledgeBase["kotlin_dsl"] = KnowledgeEntry(
            id = "kotlin_dsl",
            title = "Kotlin DSL",
            content = "Domain-specific languages in Kotlin",
            category = "language",
            tags = listOf("kotlin", "dsl", "builder", "syntax"),
            confidence = 0.85,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun loadCommonPatterns() {
        knowledgeBase["singleton_pattern"] = KnowledgeEntry(
            id = "singleton_pattern",
            title = "Singleton Pattern",
            content = "Ensure a class has only one instance",
            category = "design_pattern",
            tags = listOf("pattern", "singleton", "creational"),
            confidence = 0.95,
            lastUpdated = System.currentTimeMillis()
        )
        
        knowledgeBase["observer_pattern"] = KnowledgeEntry(
            id = "observer_pattern",
            title = "Observer Pattern",
            content = "Define a one-to-many dependency between objects",
            category = "design_pattern",
            tags = listOf("pattern", "observer", "behavioral"),
            confidence = 0.9,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun loadBestPractices() {
        knowledgeBase["android_security"] = KnowledgeEntry(
            id = "android_security",
            title = "Android Security Best Practices",
            content = "Security guidelines for Android applications",
            category = "security",
            tags = listOf("android", "security", "best_practices"),
            confidence = 0.9,
            lastUpdated = System.currentTimeMillis()
        )
        
        knowledgeBase["performance_optimization"] = KnowledgeEntry(
            id = "performance_optimization",
            title = "Performance Optimization",
            content = "Techniques for optimizing Android app performance",
            category = "performance",
            tags = listOf("android", "performance", "optimization"),
            confidence = 0.85,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun startIntelligenceGathering() {
        intelligenceScope.launch {
            while (true) {
                try {
                    gatherIntelligence()
                    delay(3600000) // Gather intelligence every hour
                } catch (e: Exception) {
                    Timber.e(e, "Intelligence gathering error")
                    delay(1800000) // Wait 30 minutes on error
                }
            }
        }
    }
    
    private fun startTrendAnalysis() {
        intelligenceScope.launch {
            while (true) {
                try {
                    analyzeTrends()
                    delay(7200000) // Analyze trends every 2 hours
                } catch (e: Exception) {
                    Timber.e(e, "Trend analysis error")
                    delay(3600000) // Wait 1 hour on error
                }
            }
        }
    }
    
    private suspend fun gatherIntelligence() {
        _intelligenceState.value = _intelligenceState.value.copy(
            isGathering = true,
            status = "Gathering intelligence from sources..."
        )
        
        try {
            // Gather from GitHub
            gatherGitHubIntelligence()
            
            // Gather from Stack Overflow
            gatherStackOverflowIntelligence()
            
            // Gather from documentation sources
            gatherDocumentationIntelligence()
            
            // Analyze code patterns
            analyzeCodePatterns()
            
            _intelligenceState.value = _intelligenceState.value.copy(
                isGathering = false,
                lastGatheringTime = System.currentTimeMillis(),
                totalIntelligenceGathered = _intelligenceState.value.totalIntelligenceGathered + 1,
                status = "Intelligence gathering completed"
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Intelligence gathering failed")
            _intelligenceState.value = _intelligenceState.value.copy(
                isGathering = false,
                status = "Intelligence gathering failed: ${e.message}"
            )
        }
    }
    
    private suspend fun gatherGitHubIntelligence() {
        try {
            // Search for trending Android repositories
            val trendingRepos = githubIntelligence.getTrendingRepositories("android", "kotlin")
            
            trendingRepos.forEach { repo ->
                val intelligence = IntelligenceData(
                    id = "github_${repo.id}",
                    source = "github",
                    type = IntelligenceType.REPOSITORY_ANALYSIS,
                    title = repo.name,
                    content = repo.description,
                    metadata = mapOf(
                        "stars" to repo.stars,
                        "language" to repo.language,
                        "topics" to repo.topics,
                        "url" to repo.url
                    ),
                    confidence = calculateRepoConfidence(repo),
                    timestamp = System.currentTimeMillis()
                )
                
                intelligenceCache[intelligence.id] = intelligence
            }
            
            // Analyze popular code patterns
            val codePatterns = githubIntelligence.analyzeCodePatterns("android")
            codePatterns.forEach { pattern ->
                updateKnowledgeBase(pattern)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "GitHub intelligence gathering failed")
        }
    }
    
    private suspend fun gatherStackOverflowIntelligence() {
        try {
            // Get popular Android questions
            val popularQuestions = stackOverflowIntelligence.getPopularQuestions("android", "kotlin")
            
            popularQuestions.forEach { question ->
                val intelligence = IntelligenceData(
                    id = "so_${question.id}",
                    source = "stackoverflow",
                    type = IntelligenceType.Q_AND_A,
                    title = question.title,
                    content = question.body,
                    metadata = mapOf(
                        "score" to question.score,
                        "views" to question.views,
                        "tags" to question.tags,
                        "answers" to question.answerCount,
                        "url" to question.url
                    ),
                    confidence = calculateQuestionConfidence(question),
                    timestamp = System.currentTimeMillis()
                )
                
                intelligenceCache[intelligence.id] = intelligence
            }
            
            // Analyze common issues and solutions
            val commonIssues = stackOverflowIntelligence.analyzeCommonIssues("android")
            commonIssues.forEach { issue ->
                updateKnowledgeBase(issue)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Stack Overflow intelligence gathering failed")
        }
    }
    
    private suspend fun gatherDocumentationIntelligence() {
        try {
            // Gather Android documentation updates
            val androidUpdates = documentationIntelligence.getDocumentationUpdates("android")
            
            androidUpdates.forEach { update ->
                val intelligence = IntelligenceData(
                    id = "docs_android_${update.id}",
                    source = "android_docs",
                    type = IntelligenceType.DOCUMENTATION,
                    title = update.title,
                    content = update.content,
                    metadata = mapOf(
                        "category" to update.category,
                        "api_level" to update.apiLevel,
                        "url" to update.url
                    ),
                    confidence = 0.95,
                    timestamp = System.currentTimeMillis()
                )
                
                intelligenceCache[intelligence.id] = intelligence
            }
            
            // Gather Kotlin documentation updates
            val kotlinUpdates = documentationIntelligence.getDocumentationUpdates("kotlin")
            
            kotlinUpdates.forEach { update ->
                val intelligence = IntelligenceData(
                    id = "docs_kotlin_${update.id}",
                    source = "kotlin_docs",
                    type = IntelligenceType.DOCUMENTATION,
                    title = update.title,
                    content = update.content,
                    metadata = mapOf(
                        "category" to update.category,
                        "version" to update.version,
                        "url" to update.url
                    ),
                    confidence = 0.95,
                    timestamp = System.currentTimeMillis()
                )
                
                intelligenceCache[intelligence.id] = intelligence
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Documentation intelligence gathering failed")
        }
    }
    
    private suspend fun analyzeCodePatterns() {
        try {
            val codeIntelligence = codeAnalysisIntelligence.analyzePatterns(intelligenceCache.values.toList())
            
            codeIntelligence.forEach { pattern ->
                val intelligence = IntelligenceData(
                    id = "pattern_${pattern.id}",
                    source = "code_analysis",
                    type = IntelligenceType.CODE_PATTERN,
                    title = pattern.name,
                    content = pattern.description,
                    metadata = mapOf(
                        "frequency" to pattern.frequency,
                        "complexity" to pattern.complexity,
                        "examples" to pattern.examples
                    ),
                    confidence = pattern.confidence,
                    timestamp = System.currentTimeMillis()
                )
                
                intelligenceCache[intelligence.id] = intelligence
                updateKnowledgeBase(pattern)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Code pattern analysis failed")
        }
    }
    
    private suspend fun analyzeTrends() {
        _intelligenceState.value = _intelligenceState.value.copy(
            status = "Analyzing trends..."
        )
        
        try {
            // Analyze technology trends
            val techTrends = trendAnalyzer.analyzeTechnologyTrends(intelligenceCache.values.toList())
            
            techTrends.forEach { trend ->
                trendData[trend.id] = trend
            }
            
            // Analyze library popularity trends
            val libraryTrends = trendAnalyzer.analyzeLibraryTrends(intelligenceCache.values.toList())
            
            libraryTrends.forEach { trend ->
                trendData[trend.id] = trend
            }
            
            // Analyze problem/solution trends
            val problemTrends = trendAnalyzer.analyzeProblemTrends(intelligenceCache.values.toList())
            
            problemTrends.forEach { trend ->
                trendData[trend.id] = trend
            }
            
            _intelligenceState.value = _intelligenceState.value.copy(
                lastTrendAnalysisTime = System.currentTimeMillis(),
                totalTrendsAnalyzed = trendData.size,
                status = "Trend analysis completed"
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Trend analysis failed")
            _intelligenceState.value = _intelligenceState.value.copy(
                status = "Trend analysis failed: ${e.message}"
            )
        }
    }
    
    private fun calculateRepoConfidence(repo: GitHubRepository): Double {
        var confidence = 0.5
        
        // Factor in stars
        confidence += minOf(repo.stars / 10000.0, 0.3)
        
        // Factor in recent activity
        if (repo.lastUpdated > System.currentTimeMillis() - 86400000 * 30) { // 30 days
            confidence += 0.1
        }
        
        // Factor in language
        if (repo.language == "Kotlin") {
            confidence += 0.1
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun calculateQuestionConfidence(question: StackOverflowQuestion): Double {
        var confidence = 0.5
        
        // Factor in score
        confidence += minOf(question.score / 100.0, 0.2)
        
        // Factor in views
        confidence += minOf(question.views / 10000.0, 0.1)
        
        // Factor in answers
        if (question.answerCount > 0) {
            confidence += 0.1
        }
        
        // Factor in accepted answer
        if (question.hasAcceptedAnswer) {
            confidence += 0.1
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    private fun updateKnowledgeBase(pattern: Any) {
        // Update knowledge base with new patterns and insights
        when (pattern) {
            is CodePattern -> {
                knowledgeBase[pattern.id] = KnowledgeEntry(
                    id = pattern.id,
                    title = pattern.name,
                    content = pattern.description,
                    category = "code_pattern",
                    tags = pattern.tags,
                    confidence = pattern.confidence,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            is CommonIssue -> {
                knowledgeBase[pattern.id] = KnowledgeEntry(
                    id = pattern.id,
                    title = pattern.title,
                    content = pattern.solution,
                    category = "common_issue",
                    tags = pattern.tags,
                    confidence = pattern.confidence,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }
    
    suspend fun searchIntelligence(
        query: String,
        filters: IntelligenceFilters = IntelligenceFilters()
    ): List<IntelligenceData> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<IntelligenceData>()
            
            // Search in intelligence cache
            intelligenceCache.values.forEach { intelligence ->
                if (matchesQuery(intelligence, query, filters)) {
                    results.add(intelligence)
                }
            }
            
            // Search in knowledge base
            knowledgeBase.values.forEach { knowledge ->
                if (matchesKnowledgeQuery(knowledge, query, filters)) {
                    val intelligence = IntelligenceData(
                        id = knowledge.id,
                        source = "knowledge_base",
                        type = IntelligenceType.KNOWLEDGE,
                        title = knowledge.title,
                        content = knowledge.content,
                        metadata = mapOf(
                            "category" to knowledge.category,
                            "tags" to knowledge.tags
                        ),
                        confidence = knowledge.confidence,
                        timestamp = knowledge.lastUpdated
                    )
                    results.add(intelligence)
                }
            }
            
            // Sort by relevance and confidence
            results.sortedWith(compareByDescending<IntelligenceData> { it.confidence }
                .thenByDescending { it.timestamp })
                .take(filters.limit)
        }
    }
    
    private fun matchesQuery(intelligence: IntelligenceData, query: String, filters: IntelligenceFilters): Boolean {
        val queryLower = query.lowercase()
        
        // Check title and content
        val titleMatch = intelligence.title.lowercase().contains(queryLower)
        val contentMatch = intelligence.content.lowercase().contains(queryLower)
        
        // Check metadata
        val metadataMatch = intelligence.metadata.values.any { value ->
            value.toString().lowercase().contains(queryLower)
        }
        
        val textMatch = titleMatch || contentMatch || metadataMatch
        
        // Apply filters
        val sourceMatch = filters.sources.isEmpty() || filters.sources.contains(intelligence.source)
        val typeMatch = filters.types.isEmpty() || filters.types.contains(intelligence.type)
        val confidenceMatch = intelligence.confidence >= filters.minConfidence
        val timeMatch = intelligence.timestamp >= filters.since
        
        return textMatch && sourceMatch && typeMatch && confidenceMatch && timeMatch
    }
    
    private fun matchesKnowledgeQuery(knowledge: KnowledgeEntry, query: String, filters: IntelligenceFilters): Boolean {
        val queryLower = query.lowercase()
        
        val titleMatch = knowledge.title.lowercase().contains(queryLower)
        val contentMatch = knowledge.content.lowercase().contains(queryLower)
        val tagMatch = knowledge.tags.any { it.lowercase().contains(queryLower) }
        
        val textMatch = titleMatch || contentMatch || tagMatch
        
        val confidenceMatch = knowledge.confidence >= filters.minConfidence
        val timeMatch = knowledge.lastUpdated >= filters.since
        
        return textMatch && confidenceMatch && timeMatch
    }
    
    suspend fun getRecommendations(
        context: String,
        type: RecommendationType
    ): List<Recommendation> {
        return withContext(Dispatchers.IO) {
            val recommendations = mutableListOf<Recommendation>()
            
            when (type) {
                RecommendationType.LIBRARIES -> {
                    recommendations.addAll(getLibraryRecommendations(context))
                }
                RecommendationType.PATTERNS -> {
                    recommendations.addAll(getPatternRecommendations(context))
                }
                RecommendationType.SOLUTIONS -> {
                    recommendations.addAll(getSolutionRecommendations(context))
                }
                RecommendationType.BEST_PRACTICES -> {
                    recommendations.addAll(getBestPracticeRecommendations(context))
                }
            }
            
            recommendations.sortedByDescending { it.confidence }
        }
    }
    
    private fun getLibraryRecommendations(context: String): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Analyze context to recommend relevant libraries
        when {
            context.contains("networking", ignoreCase = true) -> {
                recommendations.add(Recommendation(
                    id = "retrofit",
                    title = "Retrofit",
                    description = "Type-safe HTTP client for Android and Java",
                    type = RecommendationType.LIBRARIES,
                    confidence = 0.9,
                    source = "intelligence_engine"
                ))
                
                recommendations.add(Recommendation(
                    id = "okhttp",
                    title = "OkHttp",
                    description = "HTTP client for Android and Java applications",
                    type = RecommendationType.LIBRARIES,
                    confidence = 0.85,
                    source = "intelligence_engine"
                ))
            }
            
            context.contains("database", ignoreCase = true) -> {
                recommendations.add(Recommendation(
                    id = "room",
                    title = "Room",
                    description = "SQLite object mapping library for Android",
                    type = RecommendationType.LIBRARIES,
                    confidence = 0.95,
                    source = "intelligence_engine"
                ))
            }
            
            context.contains("image", ignoreCase = true) -> {
                recommendations.add(Recommendation(
                    id = "glide",
                    title = "Glide",
                    description = "Image loading and caching library for Android",
                    type = RecommendationType.LIBRARIES,
                    confidence = 0.9,
                    source = "intelligence_engine"
                ))
            }
        }
        
        return recommendations
    }
    
    private fun getPatternRecommendations(context: String): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Recommend architectural patterns based on context
        when {
            context.contains("architecture", ignoreCase = true) -> {
                recommendations.add(Recommendation(
                    id = "mvvm_pattern",
                    title = "MVVM Pattern",
                    description = "Model-View-ViewModel architectural pattern",
                    type = RecommendationType.PATTERNS,
                    confidence = 0.9,
                    source = "knowledge_base"
                ))
            }
            
            context.contains("async", ignoreCase = true) || context.contains("coroutine", ignoreCase = true) -> {
                recommendations.add(Recommendation(
                    id = "coroutine_pattern",
                    title = "Coroutine Patterns",
                    description = "Best practices for using Kotlin coroutines",
                    type = RecommendationType.PATTERNS,
                    confidence = 0.85,
                    source = "knowledge_base"
                ))
            }
        }
        
        return recommendations
    }
    
    private fun getSolutionRecommendations(context: String): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Search for relevant solutions in intelligence cache
        intelligenceCache.values.filter { it.type == IntelligenceType.Q_AND_A }
            .filter { it.title.lowercase().contains(context.lowercase()) || 
                     it.content.lowercase().contains(context.lowercase()) }
            .take(5)
            .forEach { intelligence ->
                recommendations.add(Recommendation(
                    id = intelligence.id,
                    title = intelligence.title,
                    description = intelligence.content.take(200) + "...",
                    type = RecommendationType.SOLUTIONS,
                    confidence = intelligence.confidence,
                    source = intelligence.source
                ))
            }
        
        return recommendations
    }
    
    private fun getBestPracticeRecommendations(context: String): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Get best practices from knowledge base
        knowledgeBase.values.filter { it.category == "best_practices" || it.category == "security" }
            .filter { it.tags.any { tag -> context.lowercase().contains(tag.lowercase()) } }
            .forEach { knowledge ->
                recommendations.add(Recommendation(
                    id = knowledge.id,
                    title = knowledge.title,
                    description = knowledge.content,
                    type = RecommendationType.BEST_PRACTICES,
                    confidence = knowledge.confidence,
                    source = "knowledge_base"
                ))
            }
        
        return recommendations
    }
    
    suspend fun getTrends(category: String = ""): List<TrendData> {
        return withContext(Dispatchers.IO) {
            if (category.isEmpty()) {
                trendData.values.toList()
            } else {
                trendData.values.filter { it.category == category }
            }.sortedByDescending { it.confidence }
        }
    }
    
    fun getIntelligenceMetrics(): IntelligenceMetrics {
        return IntelligenceMetrics(
            totalIntelligenceGathered = _intelligenceState.value.totalIntelligenceGathered,
            totalTrendsAnalyzed = _intelligenceState.value.totalTrendsAnalyzed,
            intelligenceCacheSize = intelligenceCache.size,
            knowledgeBaseSize = knowledgeBase.size,
            trendDataSize = trendData.size,
            lastGatheringTime = _intelligenceState.value.lastGatheringTime,
            lastTrendAnalysisTime = _intelligenceState.value.lastTrendAnalysisTime,
            sourcesActive = intelligenceSources.size
        )
    }
    
    fun clearIntelligenceCache() {
        intelligenceCache.clear()
        _intelligenceState.value = _intelligenceState.value.copy(
            status = "Intelligence cache cleared"
        )
    }
    
    fun exportIntelligence(): String {
        val export = JSONObject()
        
        // Export intelligence data
        val intelligenceArray = JSONArray()
        intelligenceCache.values.forEach { intelligence ->
            val intelligenceObj = JSONObject().apply {
                put("id", intelligence.id)
                put("source", intelligence.source)
                put("type", intelligence.type.name)
                put("title", intelligence.title)
                put("content", intelligence.content)
                put("confidence", intelligence.confidence)
                put("timestamp", intelligence.timestamp)
            }
            intelligenceArray.put(intelligenceObj)
        }
        export.put("intelligence", intelligenceArray)
        
        // Export knowledge base
        val knowledgeArray = JSONArray()
        knowledgeBase.values.forEach { knowledge ->
            val knowledgeObj = JSONObject().apply {
                put("id", knowledge.id)
                put("title", knowledge.title)
                put("content", knowledge.content)
                put("category", knowledge.category)
                put("tags", JSONArray(knowledge.tags))
                put("confidence", knowledge.confidence)
                put("lastUpdated", knowledge.lastUpdated)
            }
            knowledgeArray.put(knowledgeObj)
        }
        export.put("knowledge", knowledgeArray)
        
        // Export trends
        val trendsArray = JSONArray()
        trendData.values.forEach { trend ->
            val trendObj = JSONObject().apply {
                put("id", trend.id)
                put("name", trend.name)
                put("category", trend.category)
                put("direction", trend.direction.name)
                put("strength", trend.strength)
                put("confidence", trend.confidence)
                put("timestamp", trend.timestamp)
            }
            trendsArray.put(trendObj)
        }
        export.put("trends", trendsArray)
        
        return export.toString(2)
    }
}

// Supporting intelligence classes
class GitHubIntelligence {
    fun initialize(source: IntelligenceSource) {
        Timber.d("Initialized GitHub intelligence")
    }
    
    suspend fun getTrendingRepositories(vararg topics: String): List<GitHubRepository> {
        // Mock trending repositories
        return listOf(
            GitHubRepository(
                id = "1",
                name = "awesome-android-kotlin",
                description = "A curated list of awesome Android Kotlin libraries",
                stars = 5000,
                language = "Kotlin",
                topics = listOf("android", "kotlin", "awesome"),
                url = "https://github.com/example/awesome-android-kotlin",
                lastUpdated = System.currentTimeMillis()
            ),
            GitHubRepository(
                id = "2",
                name = "compose-samples",
                description = "Official Jetpack Compose samples",
                stars = 8000,
                language = "Kotlin",
                topics = listOf("android", "compose", "samples"),
                url = "https://github.com/example/compose-samples",
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun analyzeCodePatterns(topic: String): List<CodePattern> {
        return listOf(
            CodePattern(
                id = "viewmodel_pattern",
                name = "ViewModel Pattern",
                description = "Using ViewModel for UI-related data",
                frequency = 0.8,
                complexity = 0.6,
                confidence = 0.9,
                examples = listOf("class MyViewModel : ViewModel()"),
                tags = listOf("android", "viewmodel", "architecture")
            )
        )
    }
}

class StackOverflowIntelligence {
    fun initialize(source: IntelligenceSource) {
        Timber.d("Initialized Stack Overflow intelligence")
    }
    
    suspend fun getPopularQuestions(vararg tags: String): List<StackOverflowQuestion> {
        return listOf(
            StackOverflowQuestion(
                id = "1",
                title = "How to use Jetpack Compose with ViewModel?",
                body = "I'm trying to integrate Jetpack Compose with ViewModel...",
                score = 150,
                views = 10000,
                tags = listOf("android", "jetpack-compose", "viewmodel"),
                answerCount = 5,
                hasAcceptedAnswer = true,
                url = "https://stackoverflow.com/questions/example"
            )
        )
    }
    
    suspend fun analyzeCommonIssues(topic: String): List<CommonIssue> {
        return listOf(
            CommonIssue(
                id = "memory_leak",
                title = "Memory Leaks in Android",
                description = "Common causes and solutions for memory leaks",
                solution = "Use weak references and proper lifecycle management",
                frequency = 0.7,
                confidence = 0.85,
                tags = listOf("android", "memory", "leak", "performance")
            )
        )
    }
}

class DocumentationIntelligence {
    fun initialize(source: IntelligenceSource) {
        Timber.d("Initialized documentation intelligence")
    }
    
    suspend fun getDocumentationUpdates(platform: String): List<DocumentationUpdate> {
        return when (platform) {
            "android" -> listOf(
                DocumentationUpdate(
                    id = "1",
                    title = "New Compose Navigation Features",
                    content = "Latest navigation features in Jetpack Compose",
                    category = "navigation",
                    apiLevel = 33,
                    version = "",
                    url = "https://developer.android.com/guide/navigation/compose"
                )
            )
            "kotlin" -> listOf(
                DocumentationUpdate(
                    id = "1",
                    title = "Kotlin Multiplatform Updates",
                    content = "Latest updates in Kotlin Multiplatform",
                    category = "multiplatform",
                    apiLevel = 0,
                    version = "1.8.0",
                    url = "https://kotlinlang.org/docs/multiplatform.html"
                )
            )
            else -> emptyList()
        }
    }
}

class CodeAnalysisIntelligence {
    suspend fun analyzePatterns(intelligenceData: List<IntelligenceData>): List<CodePattern> {
        return listOf(
            CodePattern(
                id = "dependency_injection",
                name = "Dependency Injection",
                description = "Using Hilt for dependency injection",
                frequency = 0.75,
                complexity = 0.7,
                confidence = 0.9,
                examples = listOf("@HiltAndroidApp", "@Inject"),
                tags = listOf("android", "hilt", "dependency-injection")
            )
        )
    }
}

class TrendAnalyzer {
    fun analyzeTechnologyTrends(intelligenceData: List<IntelligenceData>): List<TrendData> {
        return listOf(
            TrendData(
                id = "compose_trend",
                name = "Jetpack Compose Adoption",
                category = "ui_framework",
                direction = TrendDirection.RISING,
                strength = 0.8,
                confidence = 0.9,
                timestamp = System.currentTimeMillis(),
                description = "Jetpack Compose is rapidly gaining adoption"
            )
        )
    }
    
    fun analyzeLibraryTrends(intelligenceData: List<IntelligenceData>): List<TrendData> {
        return listOf(
            TrendData(
                id = "hilt_trend",
                name = "Hilt Dependency Injection",
                category = "dependency_injection",
                direction = TrendDirection.RISING,
                strength = 0.7,
                confidence = 0.85,
                timestamp = System.currentTimeMillis(),
                description = "Hilt is becoming the preferred DI solution"
            )
        )
    }
    
    fun analyzeProblemTrends(intelligenceData: List<IntelligenceData>): List<TrendData> {
        return listOf(
            TrendData(
                id = "performance_trend",
                name = "Performance Optimization Focus",
                category = "performance",
                direction = TrendDirection.STABLE,
                strength = 0.9,
                confidence = 0.8,
                timestamp = System.currentTimeMillis(),
                description = "Continued focus on app performance optimization"
            )
        )
    }
}

// Data classes for intelligence engine
data class IntelligenceState(
    val isInitialized: Boolean = false,
    val isGathering: Boolean = false,
    val status: String = "Initializing...",
    val totalIntelligenceGathered: Long = 0,
    val totalTrendsAnalyzed: Int = 0,
    val lastGatheringTime: Long = 0,
    val lastTrendAnalysisTime: Long = 0
)

data class IntelligenceSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val type: SourceType,
    val capabilities: List<String>,
    val rateLimitPerHour: Int
)

data class IntelligenceData(
    val id: String,
    val source: String,
    val type: IntelligenceType,
    val title: String,
    val content: String,
    val metadata: Map<String, Any>,
    val confidence: Double,
    val timestamp: Long
)

data class KnowledgeEntry(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>,
    val confidence: Double,
    val lastUpdated: Long
)

data class TrendData(
    val id: String,
    val name: String,
    val category: String,
    val direction: TrendDirection,
    val strength: Double,
    val confidence: Double,
    val timestamp: Long,
    val description: String
)

data class GitHubRepository(
    val id: String,
    val name: String,
    val description: String,
    val stars: Int,
    val language: String,
    val topics: List<String>,
    val url: String,
    val lastUpdated: Long
)

data class StackOverflowQuestion(
    val id: String,
    val title: String,
    val body: String,
    val score: Int,
    val views: Int,
    val tags: List<String>,
    val answerCount: Int,
    val hasAcceptedAnswer: Boolean,
    val url: String
)

data class DocumentationUpdate(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val apiLevel: Int,
    val version: String,
    val url: String
)

data class CodePattern(
    val id: String,
    val name: String,
    val description: String,
    val frequency: Double,
    val complexity: Double,
    val confidence: Double,
    val examples: List<String>,
    val tags: List<String>
)

data class CommonIssue(
    val id: String,
    val title: String,
    val description: String,
    val solution: String,
    val frequency: Double,
    val confidence: Double,
    val tags: List<String>
)

data class IntelligenceFilters(
    val sources: List<String> = emptyList(),
    val types: List<IntelligenceType> = emptyList(),
    val minConfidence: Double = 0.0,
    val since: Long = 0,
    val limit: Int = 50
)

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val type: RecommendationType,
    val confidence: Double,
    val source: String
)

data class IntelligenceMetrics(
    val totalIntelligenceGathered: Long,
    val totalTrendsAnalyzed: Int,
    val intelligenceCacheSize: Int,
    val knowledgeBaseSize: Int,
    val trendDataSize: Int,
    val lastGatheringTime: Long,
    val lastTrendAnalysisTime: Long,
    val sourcesActive: Int
)

enum class SourceType {
    CODE_REPOSITORY, Q_AND_A, DOCUMENTATION, PACKAGE_REGISTRY
}

enum class IntelligenceType {
    REPOSITORY_ANALYSIS, Q_AND_A, DOCUMENTATION, CODE_PATTERN, KNOWLEDGE
}

enum class TrendDirection {
    RISING, FALLING, STABLE
}

enum class RecommendationType {
    LIBRARIES, PATTERNS, SOLUTIONS, BEST_PRACTICES
}

