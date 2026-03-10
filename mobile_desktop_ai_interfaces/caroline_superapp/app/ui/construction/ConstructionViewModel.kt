package com.wade.caroline.ui.construction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class ConstructionUiState(
    val projects: List<WccProject> = emptyList(),
    val kpiActiveJobs: Int = 0,
    val kpiPipelineRevenue: Double = 0.0,
    val kpiAvgProgress: Int = 0,
    val isLoading: Boolean = false,
    val agentResponse: String = "",
    val lastEstimateJson: String = "",
    val lastProposalMarkdown: String = "",
    val errorMessage: String? = null,
    val isSpeaking: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// ConstructionViewModel
// Bridges ConstructionDashboardScreen ↔ Multi-Agent Orchestrator (Python backend)
// ─────────────────────────────────────────────────────────────────────────────

class ConstructionViewModel : ViewModel() {

    companion object {
        private const val TAG = "ConstructionViewModel"

        // Orchestrator API endpoint — update to your deployed server URL in production
        // For local dev: "http://10.0.2.2:8000" (Android emulator → localhost)
        // For on-device: "http://localhost:8000"
        private const val ORCHESTRATOR_BASE_URL = "http://10.0.2.2:8000"
        private const val AGENT_NAME = "WCC_Pro"
    }

    private val _uiState = MutableStateFlow(ConstructionUiState())
    val uiState: StateFlow<ConstructionUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    init {
        loadProjects()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load Projects from Orchestrator
    // ─────────────────────────────────────────────────────────────────────────

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = sendToOrchestrator(
                    agentName = AGENT_NAME,
                    message = "List all active projects with their status, phase, and estimate total. Return as JSON array."
                )
                val projects = parseProjectsFromResponse(response)
                val activeJobs = projects.count { it.status == WccProjectStatus.ACTIVE }
                val pipeline = projects.sumOf { it.estimateTotal }
                val avgProg = if (projects.isNotEmpty()) projects.map { it.progressPct }.average().toInt() else 0
                _uiState.update {
                    it.copy(
                        projects = projects,
                        kpiActiveJobs = activeJobs,
                        kpiPipelineRevenue = pipeline,
                        kpiAvgProgress = avgProg,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load projects", e)
                // Fall back to sample data so UI is never empty
                val sample = sampleProjects()
                _uiState.update {
                    it.copy(
                        projects = sample,
                        kpiActiveJobs = sample.count { p -> p.status == WccProjectStatus.ACTIVE },
                        kpiPipelineRevenue = sample.sumOf { p -> p.estimateTotal },
                        kpiAvgProgress = sample.map { p -> p.progressPct }.average().toInt(),
                        isLoading = false,
                        errorMessage = "Offline mode — showing cached data"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handle Voice / Quick Action Commands
    // ─────────────────────────────────────────────────────────────────────────

    fun handleVoiceCommand(command: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, agentResponse = "") }
            try {
                // Route to the correct agent based on intent keywords
                val targetAgent = routeCommand(command)
                val response = sendToOrchestrator(agentName = targetAgent, message = command)

                // Post-process: if response looks like an estimate JSON, store it
                if (response.trimStart().startsWith("{") && response.contains("grand_total")) {
                    _uiState.update { it.copy(lastEstimateJson = response) }
                }
                // If response looks like a Markdown proposal, store it
                if (response.contains("# Project Proposal")) {
                    _uiState.update { it.copy(lastProposalMarkdown = response) }
                }

                _uiState.update {
                    it.copy(isLoading = false, agentResponse = response, errorMessage = null)
                }

                // Reload projects after any mutating command
                if (command.contains("log", ignoreCase = true) ||
                    command.contains("start project", ignoreCase = true) ||
                    command.contains("add client", ignoreCase = true)) {
                    loadProjects()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice command failed: $command", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Could not reach Caroline: ${e.message}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Estimate
    // ─────────────────────────────────────────────────────────────────────────

    fun createEstimate(clientName: String, projectType: String, notes: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Create an estimate for $clientName — $projectType. Notes: $notes"
                val response = sendToOrchestrator(AGENT_NAME, prompt)
                _uiState.update {
                    it.copy(isLoading = false, lastEstimateJson = response, agentResponse = response)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generate Proposal from Last Estimate
    // ─────────────────────────────────────────────────────────────────────────

    fun generateProposal() {
        val estimateJson = _uiState.value.lastEstimateJson
        if (estimateJson.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No estimate on file. Create an estimate first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Generate a proposal from this estimate JSON: $estimateJson"
                val response = sendToOrchestrator(AGENT_NAME, prompt)
                _uiState.update {
                    it.copy(isLoading = false, lastProposalMarkdown = response, agentResponse = response)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Log Hours
    // ─────────────────────────────────────────────────────────────────────────

    fun logHours(taskName: String, hours: Double, progressPct: Int, notes: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Log $hours hours on '$taskName' at $progressPct% complete. Notes: $notes"
                val response = sendToOrchestrator(AGENT_NAME, prompt)
                _uiState.update { it.copy(isLoading = false, agentResponse = response) }
                loadProjects()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pricebook Search
    // ─────────────────────────────────────────────────────────────────────────

    fun searchPricebook(searchTerm: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Search the pricebook for: $searchTerm"
                val response = sendToOrchestrator(AGENT_NAME, prompt)
                _uiState.update { it.copy(isLoading = false, agentResponse = response) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clear Error
    // ─────────────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private: HTTP call to the Python Orchestrator REST API
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun sendToOrchestrator(agentName: String, message: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("$ORCHESTRATOR_BASE_URL/chat")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000

            val body = JSONObject().apply {
                put("agent", agentName)
                put("message", message)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val raw = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode in 200..299) {
                try {
                    JSONObject(raw).optString("response", raw)
                } catch (e: Exception) {
                    raw
                }
            } else {
                throw Exception("Orchestrator error $responseCode: $raw")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private: Route command to the right agent by keyword
    // ─────────────────────────────────────────────────────────────────────────

    private fun routeCommand(command: String): String {
        val lower = command.lowercase()
        return when {
            lower.contains("estimate") || lower.contains("proposal") ||
            lower.contains("log hours") || lower.contains("client") ||
            lower.contains("project") || lower.contains("pricebook") ||
            lower.contains("material") || lower.contains("briefing") -> AGENT_NAME
            lower.contains("research") || lower.contains("price") ||
            lower.contains("rsmeans") || lower.contains("code") -> "Researcher"
            else -> "Orchestrator"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private: Parse project list from agent JSON response
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseProjectsFromResponse(response: String): List<WccProject> {
        return try {
            val arr = JSONArray(response.trim())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WccProject(
                    id = obj.optString("id", "$i"),
                    clientName = obj.optString("client", "Unknown"),
                    projectType = obj.optString("project_type", "Remodel"),
                    status = when (obj.optString("status", "active").lowercase()) {
                        "estimating" -> WccProjectStatus.ESTIMATING
                        "punch_out", "punch-out" -> WccProjectStatus.PUNCH_OUT
                        "complete" -> WccProjectStatus.COMPLETE
                        else -> WccProjectStatus.ACTIVE
                    },
                    estimateTotal = obj.optDouble("estimate_total", 0.0),
                    progressPct = obj.optInt("progress_pct", 0),
                    currentPhase = obj.optString("current_phase", "TBD")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse project JSON, using sample data: ${e.message}")
            sampleProjects()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sample / Fallback Data
    // ─────────────────────────────────────────────────────────────────────────

    private fun sampleProjects() = listOf(
        WccProject("1", "John Smith",    "Master Bathroom Remodel", WccProjectStatus.ACTIVE,     23500.0, 45, "Tile"),
        WccProject("2", "Sarah Johnson", "Kitchen Remodel",          WccProjectStatus.ESTIMATING, 41200.0, 0,  "Estimating"),
        WccProject("3", "Mike Davis",    "Trim Carpentry",           WccProjectStatus.PUNCH_OUT,  8400.0,  90, "Punch-Out"),
        WccProject("4", "Lisa Chen",     "Hardwood Flooring",        WccProjectStatus.ACTIVE,     12800.0, 60, "Flooring")
    )
}
