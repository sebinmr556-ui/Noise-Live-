package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageType
import com.example.model.UserProfile
import com.example.ui.components.VoiceNoteBubble
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.WebRtcConnectedGreen
import com.example.util.VoiceNoteManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<ChatMessage>,
    currentUser: UserProfile,
    voiceNoteManager: VoiceNoteManager,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceNote: (durationSec: Int, amplitudes: List<Float>, filePath: String?) -> Unit,
    onSendPhoto: (String) -> Unit,
    onReact: (messageId: String, emoji: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var liveAmplitudes by remember { mutableStateOf<List<Float>>(emptyList()) }
    var callDialogState by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Blinking animation for recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Permission launcher for audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecordingVoice = true
            recordingSeconds = 0
            liveAmplitudes = emptyList()
            voiceNoteManager.startRecording(context) { amp ->
                liveAmplitudes = (liveAmplitudes + amp).takeLast(20)
            }
        }
    }

    // Timer while recording
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Participant Avatar with Live Online Status Ring
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.size(38.dp)
                        ) {
                            AsyncImage(
                                model = conversation.participantAvatar,
                                contentDescription = conversation.participantName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            if (conversation.isUserActiveNow) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(1.5.dp)
                                        .clip(CircleShape)
                                        .background(WebRtcConnectedGreen)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversation.participantName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (conversation.isUserActiveNow) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(WebRtcConnectedGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Active now",
                                        fontSize = 11.sp,
                                        color = WebRtcConnectedGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = conversation.lastActiveText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { callDialogState = "Audio call with ${conversation.participantName}" }) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { callDialogState = "Video call with ${conversation.participantName}" }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Intro header at top of chat
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = conversation.participantAvatar,
                                contentDescription = conversation.participantName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            if (conversation.isUserActiveNow) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(2.5.dp)
                                        .clip(CircleShape)
                                        .background(WebRtcConnectedGreen)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = conversation.participantName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "@${conversation.participantUsername}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (conversation.isUserActiveNow) "🟢 Online on NOICE LIVE" else conversation.lastActiveText,
                            fontSize = 12.sp,
                            color = if (conversation.isUserActiveNow) WebRtcConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubbleItem(
                        message = message,
                        voiceNoteManager = voiceNoteManager,
                        onReact = { emoji -> onReact(message.id, emoji) }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (isRecordingVoice) {
                        // Voice Recording Active Mode
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "0:${recordingSeconds.toString().padStart(2, '0')}",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Dynamic live waveform bars while speaking
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        ) {
                            val displayBars = liveAmplitudes.takeLast(16)
                            displayBars.forEach { amp ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((amp * 22).dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.Red.copy(alpha = 0.85f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cancel button
                        IconButton(
                            onClick = {
                                isRecordingVoice = false
                                voiceNoteManager.cancelRecording()
                            },
                            modifier = Modifier.testTag("cancel_voice_record_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Cancel Recording",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Send Voice Note button
                        IconButton(
                            onClick = {
                                isRecordingVoice = false
                                val result = voiceNoteManager.stopRecording()
                                val duration = (result?.durationSec ?: recordingSeconds).coerceAtLeast(1)
                                val amps = result?.amplitudes ?: (0 until 14).map { 0.5f }
                                val path = result?.filePath
                                onSendVoiceNote(duration, amps, path)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WebRtcConnectedGreen)
                                .testTag("send_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Note",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Standard Text & Voice Input Bar
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Message...", fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input"),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendMessage(textInput)
                                    textInput = ""
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DarkAccent)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Mic Button for voice recording
                            IconButton(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        isRecordingVoice = true
                                        recordingSeconds = 0
                                        liveAmplitudes = emptyList()
                                        voiceNoteManager.startRecording(context) { amp ->
                                            liveAmplitudes = (liveAmplitudes + amp).takeLast(20)
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.testTag("record_voice_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = DarkAccent
                                )
                            }

                            // Photo attachment
                            IconButton(
                                onClick = {
                                    onSendPhoto("https://images.unsplash.com/photo-1544829099-b9a0c07fad1a?w=600&q=80")
                                },
                                modifier = Modifier.testTag("send_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "Send Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Call dialog
    callDialogState?.let { callText ->
        AlertDialog(
            onDismissRequest = { callDialogState = null },
            title = { Text("Connecting...") },
            text = { Text(callText) },
            confirmButton = {
                TextButton(onClick = { callDialogState = null }) {
                    Text("End Call", color = Color.Red)
                }
            }
        )
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    voiceNoteManager: VoiceNoteManager,
    onReact: (String) -> Unit
) {
    var showReactionMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp
            ),
            color = if (message.isMine) DarkAccent else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clickable { showReactionMenu = !showReactionMenu }
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (message.messageType) {
                    MessageType.VOICE_NOTE -> {
                        VoiceNoteBubble(
                            message = message,
                            voiceNoteManager = voiceNoteManager
                        )
                    }
                    MessageType.PHOTO -> {
                        message.photoUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Sent photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.timestampText,
                        fontSize = 10.sp,
                        color = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Seen",
                            tint = if (message.isRead) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Reaction badge
        message.reactionEmoji?.let { emoji ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = emoji,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // Emoji reaction quick selector
        AnimatedVisibility(visible = showReactionMenu) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("❤️", "👍", "😂", "🔥", "😮", "👏").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable {
                                onReact(emoji)
                                showReactionMenu = false
                            }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}
