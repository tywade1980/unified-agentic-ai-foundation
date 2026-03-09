package com.wade.caroline.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wade.caroline.ui.construction.ConstructionDashboardScreen
// Import other screens as they are built:
// import com.wade.caroline.ui.chat.GeminiStyleChatInterface
// import com.wade.caroline.ui.ide.IDEScreen
// import com.wade.caroline.ui.settings.SettingsScreen

// ─────────────────────────────────────────────────────────────────────────────
// Navigation Routes
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat         : Screen("chat",         "Caroline",    Icons.Default.Chat)
    object Construction : Screen("construction", "WCC Pro",     Icons.Default.Construction)
    object IDE          : Screen("ide",          "IDE",         Icons.Default.Code)
    object Settings     : Screen("settings",     "Settings",    Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Chat,
    Screen.Construction,
    Screen.IDE,
    Screen.Settings
)

// ─────────────────────────────────────────────────────────────────────────────
// Main App Navigation Host
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationHost(
    onVoiceCommand: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1A2E),
                contentColor = Color.White
            ) {
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint = if (currentRoute == screen.route) Color(0xFFFF6B35) else Color(0xFF9E9E9E)
                            )
                        },
                        label = {
                            Text(
                                screen.label,
                                color = if (currentRoute == screen.route) Color(0xFFFF6B35) else Color(0xFF9E9E9E)
                            )
                        },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF252540)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF0F0F1A)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Chat / Caroline Screen ──
            composable(Screen.Chat.route) {
                // Placeholder until GeminiStyleChatInterface is fully wired
                ChatPlaceholderScreen(onVoiceCommand = onVoiceCommand)
            }

            // ── WCC Pro Construction Dashboard ──
            composable(Screen.Construction.route) {
                ConstructionDashboardScreen(
                    onVoiceCommand = onVoiceCommand,
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToEstimate = { projectId ->
                        // Navigate to estimate detail screen when built
                        navController.navigate("estimate/$projectId")
                    }
                )
            }

            // ── IDE Screen ──
            composable(Screen.IDE.route) {
                IDEPlaceholderScreen()
            }

            // ── Settings Screen ──
            composable(Screen.Settings.route) {
                SettingsPlaceholderScreen()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Placeholder Screens (to be replaced with full implementations)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatPlaceholderScreen(onVoiceCommand: (String) -> Unit) {
    // This will be replaced by the full GeminiStyleChatInterface
    // For now, shows a simple prompt to indicate the screen is wired
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = androidx.compose.ui.Modifier.size(64.dp)
            )
            Text(
                "Caroline Chat",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "GeminiStyleChatInterface will be rendered here.\nWire in ChatViewModel to activate.",
                color = Color(0xFF9E9E9E),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { onVoiceCommand("Hello Caroline, what can you help me with today?") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(androidx.compose.ui.Modifier.width(8.dp))
                Text("Activate Voice")
            }
        }
    }
}

@Composable
fun IDEPlaceholderScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF2196F3), modifier = androidx.compose.ui.Modifier.size(64.dp))
            Spacer(androidx.compose.ui.Modifier.height(16.dp))
            Text("IDE Screen", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("IDEScreen.kt will be rendered here.", color = Color(0xFF9E9E9E))
        }
    }
}

@Composable
fun SettingsPlaceholderScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = androidx.compose.ui.Modifier.size(64.dp))
            Spacer(androidx.compose.ui.Modifier.height(16.dp))
            Text("Settings", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Agent config, API keys, voice settings.", color = Color(0xFF9E9E9E))
        }
    }
}
