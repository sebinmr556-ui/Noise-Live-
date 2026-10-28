package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.ui.theme.AudioWavePurple
import com.example.ui.theme.DarkAccent
import com.example.util.VoiceNoteManager

@Composable
fun VoiceNoteBubble(
    message: ChatMessage,
    voiceNoteManager: VoiceNoteManager,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var speedMultiplier by remember { mutableStateOf(1f) }

    // Synchronize with voiceNoteManager
    DisposableEffect(message.id) {
        onDispose {
            if (voiceNoteManager.currentPlayingPath == message.audioFilePath && isPlaying) {
                voiceNoteManager.stopAudio()
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Play / Pause Circle Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (message.isMine) Color.White else DarkAccent)
                .clickable {
                    if (isPlaying) {
                        voiceNoteManager.stopAudio()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        voiceNoteManager.playVoiceNote(
                            filePath = message.audioFilePath,
                            durationSec = message.voiceDurationSec,
                            onProgress = { p -> progress = p },
                            onComplete = {
                                isPlaying = false
                                progress = 0f
                            }
                        )
                    }
                }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                tint = if (message.isMine) DarkAccent else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Waveform & Timer Column
        Column(modifier = Modifier.weight(1f)) {
            // Interactive Waveform Bars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val amplitudes = if (message.voiceAmplitudes.isNotEmpty()) {
                    message.voiceAmplitudes
                } else {
                    listOf(0.35f, 0.65f, 0.85f, 0.45f, 0.95f, 0.75f, 0.40f, 0.80f, 0.60f, 0.90f, 0.50f, 0.70f)
                }

                amplitudes.forEachIndexed { index, amp ->
                    val barFraction = index.toFloat() / amplitudes.size.toFloat()
                    val isPlayed = barFraction <= progress

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((amp * 26).dp.coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isPlayed && message.isMine -> Color.White
                                    isPlayed -> AudioWavePurple
                                    message.isMine -> Color.White.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Time & Mic indicator row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (message.isMine) Color.White.copy(alpha = 0.8f) else AudioWavePurple,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val currentSeconds = (progress * message.voiceDurationSec).toInt()
                val displaySec = if (isPlaying) currentSeconds else message.voiceDurationSec
                Text(
                    text = "0:${displaySec.toString().padStart(2, '0')} / 0:${message.voiceDurationSec.toString().padStart(2, '0')}",
                    fontSize = 11.sp,
                    color = if (message.isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // Playback speed pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (message.isMine) Color.White.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        .clickable {
                            speedMultiplier = when (speedMultiplier) {
                                1f -> 1.5f
                                1.5f -> 2f
                                else -> 1f
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${speedMultiplier}x",
                        fontSize = 10.sp,
                        color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
