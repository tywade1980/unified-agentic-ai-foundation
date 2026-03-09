package com.wade.caroline.ui.construction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────────────

data class WccProject(
    val id: String,
    val clientName: String,
    val projectType: String,
    val status: WccProjectStatus,
    val estimateTotal: Double,
    val progressPct: Int,
    val currentPhase: String
)

enum class WccProjectStatus(val label: String, val color: Color) {
    ACTIVE("Active", Color(0xFF4CAF50)),
    ESTIMATING("Estimating", Color(0xFFFF9800)),
    PUNCH_OUT("Punch-Out", Color(0xFF2196F3)),
    COMPLETE("Complete", Color(0xFF9E9E9E))
}

data class WccQuickAction(
    val label: String,
    val icon: ImageVector,
    val voicePrompt: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Construction Dashboard Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructionDashboardScreen(
    onVoiceCommand: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToEstimate: (String) -> Unit = {}
) {
    // Sample data — in production, this comes from the WCC Pro Agent via ViewModel
    val projects = remember {
        listOf(
            WccProject("1", "John Smith",    "Master Bathroom Remodel", WccProjectStatus.ACTIVE,     23500.0, 45, "Tile"),
            WccProject("2", "Sarah Johnson", "Kitchen Remodel",          WccProjectStatus.ESTIMATING, 41200.0, 0,  "Estimating"),
            WccProject("3", "Mike Davis",    "Trim Carpentry",           WccProjectStatus.PUNCH_OUT,  8400.0,  90, "Punch-Out"),
            WccProject("4", "Lisa Chen",     "Hardwood Flooring",        WccProjectStatus.ACTIVE,     12800.0, 60, "Flooring"),
        )
    }

    val quickActions = listOf(
        WccQuickAction("New Estimate",   Icons.Default.Calculate,    "Create a new project estimate"),
        WccQuickAction("Log Hours",      Icons.Default.AccessTime,   "Log hours for a task"),
        WccQuickAction("Add Client",     Icons.Default.PersonAdd,    "Add a new client"),
        WccQuickAction("Voice Briefing", Icons.Default.RecordVoiceOver, "Give me a project status briefing"),
        WccQuickAction("Find Material",  Icons.Default.Search,       "Search the pricebook for a material"),
        WccQuickAction("Send Proposal",  Icons.Default.Send,         "Generate and send a proposal"),
    )

    // KPI totals
    val activeProjects = projects.count { it.status == WccProjectStatus.ACTIVE }
    val totalRevenue = projects.sumOf { it.estimateTotal }
    val avgProgress = if (projects.isNotEmpty()) projects.map { it.progressPct }.average().toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WCC Pro Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Wade Custom Carpentry", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat with Caroline")
                    }
                    IconButton(onClick = { onVoiceCommand("Give me a project status briefing") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Command")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color(0xFFFF6B35)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── KPI Cards ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Active Jobs",
                        value = "$activeProjects",
                        icon = Icons.Default.Construction,
                        accentColor = Color(0xFF4CAF50)
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Pipeline",
                        value = "$${"%,.0f".format(totalRevenue)}",
                        icon = Icons.Default.AttachMoney,
                        accentColor = Color(0xFFFF6B35)
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Avg Progress",
                        value = "$avgProgress%",
                        icon = Icons.Default.TrendingUp,
                        accentColor = Color(0xFF2196F3)
                    )
                }
            }

            // ── Quick Actions ──
            item {
                Text(
                    "Quick Actions",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                QuickActionsGrid(actions = quickActions, onAction = onVoiceCommand)
            }

            // ── Active Projects ──
            item {
                Text(
                    "Projects",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onNavigateToEstimate(project.id) }
                )
            }

            // ── Voice Prompt Banner ──
            item {
                VoicePromptBanner(onVoiceCommand = onVoiceCommand)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KPI Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E30))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = Color(0xFF9E9E9E), fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Actions Grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickActionsGrid(actions: List<WccQuickAction>, onAction: (String) -> Unit) {
    val rows = actions.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { action ->
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        action = action,
                        onClick = { onAction(action.voicePrompt) }
                    )
                }
                // Fill empty slots if row has < 3 items
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    action: WccQuickAction,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252540))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                action.icon,
                contentDescription = action.label,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(action.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Project Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectCard(project: WccProject, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E30))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.clientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(project.projectType, color = Color(0xFF9E9E9E), fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${"%,.0f".format(project.estimateTotal)}",
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    StatusChip(status = project.status)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Phase: ${project.currentPhase}",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("${project.progressPct}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { project.progressPct / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = project.status.color,
                trackColor = Color(0xFF333355)
            )
        }
    }
}

@Composable
fun StatusChip(status: WccProjectStatus) {
    Box(
        modifier = Modifier
            .background(status.color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.label, color = status.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Voice Prompt Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VoicePromptBanner(onVoiceCommand: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVoiceCommand("Give me a full project status briefing") },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.RecordVoiceOver,
                contentDescription = "Voice",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text("Tap for Voice Briefing", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "\"Caroline, give me a project status update\"",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
        }
    }
}
