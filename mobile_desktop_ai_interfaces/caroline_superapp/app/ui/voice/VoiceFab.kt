package com.wade.caroline.ui.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wade.caroline.core.voice.VoiceState

/**
 * VoiceFab — Floating Action Button for hands-free voice interaction.
 *
 * Drop this into any Scaffold's floatingActionButton slot:
 *
 *   Scaffold(
 *       floatingActionButton = {
 *           VoiceFab(
 *               voiceState = voiceState,
 *               lastTranscript = transcript,
 *               lastResponse = result.voiceText,
 *               onToggle = { voiceVm.toggleListening() }
 *           )
 *       }
 *   )
 */
@Composable
fun VoiceFab(
    voiceState: VoiceState,
    lastTranscript: String = "",
    lastResponse: String = "",
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Response bubble ──
        if (lastResponse.isNotBlank() && voiceState == VoiceState.IDLE) {
            ResponseBubble(text = lastResponse)
        }

        // ── Transcript bubble ──
        if (lastTranscript.isNotBlank() && voiceState != VoiceState.IDLE) {
            TranscriptBubble(text = lastTranscript)
        }

        // ── Main FAB ──
        val pulseAnim = rememberInfiniteTransition(label = "pulse")
        val scale by pulseAnim.animateFloat(
            initialValue = 1f,
            targetValue = if (voiceState == VoiceState.LISTENING) 1.15f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.scale(scale),
            shape = CircleShape,
            containerColor = when (voiceState) {
                VoiceState.LISTENING  -> Color(0xFFE53935)   // Red — recording
                VoiceState.PROCESSING -> Color(0xFFFF9800)   // Orange — thinking
                VoiceState.SPEAKING   -> Color(0xFF4CAF50)   // Green — speaking
                VoiceState.ERROR      -> Color(0xFF9E9E9E)   // Grey — error
                VoiceState.IDLE       -> Color(0xFFFF6B35)   // WCC Orange — ready
            },
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            when (voiceState) {
                VoiceState.LISTENING  -> Icon(Icons.Default.Stop,   contentDescription = "Stop recording", modifier = Modifier.size(28.dp))
                VoiceState.PROCESSING -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                VoiceState.SPEAKING   -> Icon(Icons.Default.MicOff, contentDescription = "Speaking",       modifier = Modifier.size(28.dp))
                VoiceState.ERROR      -> Icon(Icons.Default.MicOff, contentDescription = "Error",          modifier = Modifier.size(28.dp))
                VoiceState.IDLE       -> Icon(Icons.Default.Mic,    contentDescription = "Talk to Caroline", modifier = Modifier.size(28.dp))
            }
        }

        // ── State label ──
        Text(
            text = when (voiceState) {
                VoiceState.LISTENING  -> "Listening..."
                VoiceState.PROCESSING -> "Thinking..."
                VoiceState.SPEAKING   -> "Caroline speaking"
                VoiceState.ERROR      -> "Try again"
                VoiceState.IDLE       -> "Talk to Caroline"
            },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun ResponseBubble(text: String) {
    Card(
        modifier = Modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E30))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Caroline",
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TranscriptBubble(text: String) {
    Card(
        modifier = Modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252540))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "You",
                color = Color(0xFFFF6B35),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color.White, fontSize = 13.sp)
        }
    }
}
