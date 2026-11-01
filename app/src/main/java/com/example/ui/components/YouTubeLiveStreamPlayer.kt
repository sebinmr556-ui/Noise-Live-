package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.RoomYouTubeStream

/**
 * Embedded in-room YouTube Watch Party player for voice chatrooms.
 * Syncs play, pause, seek position, and track queue across all members in real time via Firestore.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLiveStreamPlayer(
    stream: RoomYouTubeStream,
    isHost: Boolean,
    onStopStream: () -> Unit,
    onTogglePlayPause: (Boolean) -> Unit = {},
    onSeekTo: (Int) -> Unit = {},
    onAddToQueue: (String, String) -> Unit = { _, _ -> },
    onPlayNextInQueue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMinimized by remember { mutableStateOf(false) }
    var showAddQueueBar by remember { mutableStateOf(false) }
    var queueUrlInput by remember { mutableStateOf("") }
    var queueTitleInput by remember { mutableStateOf("") }
    var seekSliderVal by remember(stream.currentPositionSec) {
        mutableStateOf(stream.currentPositionSec.coerceIn(0, 600).toFloat())
    }
    var lastLoadedVideoId by remember { mutableStateOf("") }
    var lastSeekApplied by remember { mutableStateOf(-1) }
    var lastPlayingState by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F081C),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFFA855F7), Color(0xFFEF4444)))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("youtube_live_stream_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Stream Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B0E30))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (stream.isPlaying) Color(0xFFDC2626) else Color(0xFF6B7280))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (stream.isPlaying) "🔴 WATCH PARTY" else "⏸ PAUSED",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stream.title.ifBlank { "YouTube Watch Party" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Host: ${stream.startedBy.ifBlank { "Room Host" }} • Queue: ${stream.queue.size}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Add to Watch Party Queue Button (available to all room members)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E164B),
                        border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { showAddQueueBar = !showAddQueueBar }
                            .testTag("youtube_toggle_queue_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to Queue",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Queue (${stream.queue.size})",
                                color = Color(0xFFFDE047),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Minimize/Expand Toggle
                    IconButton(
                        onClick = { isMinimized = !isMinimized },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text(
                            text = if (isMinimized) "▼" else "▲",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    // Host Stop Stream Button
                    if (isHost) {
                        IconButton(
                            onClick = onStopStream,
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("stop_youtube_stream_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Stop Stream",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Embedded Video WebView (Collapsible)
            AnimatedVisibility(visible = !isMinimized) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                    }
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    val startSec = stream.currentPositionSec.coerceAtLeast(0)
                                    val autoplayParam = if (stream.isPlaying) 1 else 0
                                    val embedUrl = "https://www.youtube.com/embed/${stream.videoId}?autoplay=$autoplayParam&playsinline=1&controls=1&enablejsapi=1&rel=0&start=$startSec"
                                    lastLoadedVideoId = stream.videoId
                                    lastSeekApplied = startSec
                                    lastPlayingState = stream.isPlaying
                                    loadUrl(embedUrl)
                                }
                            },
                            update = { webView ->
                                val targetStart = stream.currentPositionSec.coerceAtLeast(0)
                                if (lastLoadedVideoId != stream.videoId) {
                                    lastLoadedVideoId = stream.videoId
                                    lastSeekApplied = targetStart
                                    lastPlayingState = stream.isPlaying
                                    val autoplayParam = if (stream.isPlaying) 1 else 0
                                    val embedUrl = "https://www.youtube.com/embed/${stream.videoId}?autoplay=$autoplayParam&playsinline=1&controls=1&enablejsapi=1&rel=0&start=$targetStart"
                                    webView.loadUrl(embedUrl)
                                } else {
                                    if (lastPlayingState != stream.isPlaying) {
                                        lastPlayingState = stream.isPlaying
                                        val cmd = if (stream.isPlaying) "playVideo" else "pauseVideo"
                                        webView.evaluateJavascript(
                                            """javascript:try{document.querySelector('iframe')?.contentWindow?.postMessage('{"event":"command","func":"$cmd","args":""}','*');}catch(e){}""",
                                            null
                                        )
                                    }
                                    if (kotlin.math.abs(targetStart - lastSeekApplied) >= 3) {
                                        lastSeekApplied = targetStart
                                        val autoplayParam = if (stream.isPlaying) 1 else 0
                                        val embedUrl = "https://www.youtube.com/embed/${stream.videoId}?autoplay=$autoplayParam&playsinline=1&controls=1&enablejsapi=1&rel=0&start=$targetStart"
                                        webView.loadUrl(embedUrl)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Synchronized Watch Party Controls (Play/Pause, Seek Bar, Next Track)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF150A26))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onTogglePlayPause(!stream.isPlaying) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("youtube_sync_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (stream.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (stream.isPlaying) "Pause Sync" else "Play Sync",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = formatSecondsLabel(seekSliderVal.toInt()),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Slider(
                            value = seekSliderVal,
                            onValueChange = { seekSliderVal = it },
                            onValueChangeFinished = {
                                onSeekTo(seekSliderVal.toInt())
                            },
                            valueRange = 0f..600f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEAB308),
                                activeTrackColor = Color(0xFFEF4444),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                        )

                        if (stream.queue.isNotEmpty()) {
                            IconButton(
                                onClick = onPlayNextInQueue,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("youtube_next_track_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next in Queue",
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Expandable Add-to-Queue Bar
                    AnimatedVisibility(visible = showAddQueueBar) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B0E30))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = queueUrlInput,
                                    onValueChange = { queueUrlInput = it },
                                    placeholder = { Text("Paste YouTube link or ID", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = queueTitleInput,
                                    onValueChange = { queueTitleInput = it },
                                    placeholder = { Text("Track title", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Button(
                                    onClick = {
                                        if (queueUrlInput.isNotBlank()) {
                                            onAddToQueue(
                                                queueUrlInput.trim(),
                                                queueTitleInput.trim().ifBlank { "Queued Video" }
                                            )
                                            queueUrlInput = ""
                                            queueTitleInput = ""
                                            showAddQueueBar = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Add", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Queued Tracks Horizontal Chips
                    if (stream.queue.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF120822))
                        ) {
                            itemsIndexed(stream.queue) { idx, item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF24133D),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1} ${item.title}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "• ${item.addedBy}",
                                            color = Color(0xFFFDE047),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSecondsLabel(totalSec: Int): String {
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(mins, secs)
}

/**
 * Modal dialog for launching a YouTube video or Watch Party in the voice chatroom.
 */
@Composable
fun YouTubeStreamLauncherDialog(
    onDismiss: () -> Unit,
    onStartStream: (videoId: String, title: String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    var streamTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF160B29),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "YouTube Watch Party",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Synchronize a YouTube video or live stream across all participants in the room with real-time play, pause, seek, and queue support.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom URL Input
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        errorMessage = ""
                    },
                    label = { Text("YouTube URL or Video ID", fontSize = 12.sp) },
                    placeholder = { Text("e.g., https://youtu.be/... or 11-char ID", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEAB308),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color(0xFFEAB308),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = streamTitle,
                    onValueChange = { streamTitle = it },
                    label = { Text("Watch Party Title (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color(0xFFA855F7),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputUrl.isBlank()) {
                        errorMessage = "Please enter a valid YouTube URL or Video ID."
                        return@Button
                    }
                    onStartStream(inputUrl.trim(), streamTitle.trim().ifBlank { "YouTube Watch Party" })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Watch Party", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
