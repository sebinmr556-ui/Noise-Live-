package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageType
import com.example.ui.theme.AudioWavePurple
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.WebRtcConnectedGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by repository.conversations.collectAsState()
    val chatMessagesMap by repository.chatMessages.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var activeConversationId by remember { mutableStateOf<String?>(null) }

    val activeConversation = conversations.find { it.id == activeConversationId }

    if (activeConversation != null) {
        // Active Chat View
        ChatDetailScreen(
            conversation = activeConversation,
            messages = chatMessagesMap[activeConversation.id] ?: emptyList(),
            currentUser = currentUser,
            onBack = { activeConversationId = null },
            onSendMessage = { text ->
                repository.sendTextMessage(activeConversation.id, text)
            },
            onSendVoiceNote = { duration, amplitudes ->
                repository.sendVoiceNote(activeConversation.id, duration, amplitudes)
            },
            onReact = { msgId, emoji ->
                repository.reactToMessage(activeConversation.id, msgId, emoji)
            }
        )
    } else {
        // Conversations List
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentUser.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* New message */ }) {
                            Icon(Icons.Default.EditNote, contentDescription = "New Message", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                // Active friends row
                item {
                    Text(
                        text = "Active Friends",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(conversations) { conv ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { activeConversationId = conv.id }
                            ) {
                                Box(
                                    contentAlignment = Alignment.BottomEnd,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    AsyncImage(
                                        model = conv.participantAvatar,
                                        contentDescription = conv.participantName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                    if (conv.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(WebRtcConnectedGreen)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = conv.participantName.split(" ").firstOrNull() ?: conv.participantUsername,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }

                item {
                    Text(
                        text = "Messages",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                items(conversations, key = { it.id }) { conv ->
                    ConversationItem(
                        conversation = conv,
                        onClick = { activeConversationId = conv.id }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("conversation_item_${conversation.id}")
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(52.dp)
        ) {
            AsyncImage(
                model = conversation.participantAvatar,
                contentDescription = conversation.participantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(WebRtcConnectedGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.participantName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastMessageText,
                    fontSize = 12.sp,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • ${conversation.lastMessageTimestamp}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (conversation.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DarkAccent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<ChatMessage>,
    currentUser: com.example.model.UserProfile,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceNote: (Int, List<Float>) -> Unit,
    onReact: (String, String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Voice recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = conversation.participantAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = conversation.participantName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (conversation.isOnline) "Active now" else "Offline",
                                fontSize = 11.sp,
                                color = if (conversation.isOnline) WebRtcConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Voice call */ }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                    }
                    IconButton(onClick = { /* Video call */ }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onReact = { emoji -> onReact(message.id, emoji) }
                    )
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
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
                        // Live Recording UI
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording 0:${recordingSeconds.toString().padStart(2, '0')}",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { isRecordingVoice = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                isRecordingVoice = false
                                val duration = recordingSeconds.coerceAtLeast(3)
                                val amps = (0 until 12).map { Random.nextFloat() * 0.8f + 0.2f }
                                onSendVoiceNote(duration, amps)
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Send Voice Note", tint = WebRtcConnectedGreen)
                        }
                    } else {
                        // Standard Message Input Bar
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
                                modifier = Modifier.testTag("chat_send_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = DarkAccent)
                            }
                        } else {
                            // Mic for voice notes
                            IconButton(
                                onClick = { isRecordingVoice = true },
                                modifier = Modifier.testTag("record_voice_note_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record Voice Note", tint = DarkAccent)
                            }

                            // Photo attachment
                            IconButton(
                                onClick = {
                                    onSendMessage("📸 Shared a photo!")
                                }
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = "Send Image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
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
                .widthIn(max = 280.dp)
                .clickable { showReactionMenu = !showReactionMenu }
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (message.messageType) {
                    MessageType.VOICE_NOTE -> {
                        VoiceNotePlayerBubble(message = message)
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

                Text(
                    text = message.timestampText,
                    fontSize = 10.sp,
                    color = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // Reaction Badge on Bubble
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

        // Reaction Emoji Quick Bar (toggleable)
        if (showReactionMenu) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("❤️", "👍", "😂", "🔥", "😮").forEach { emoji ->
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

@Composable
private fun VoiceNotePlayerBubble(message: ChatMessage) {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playbackProgress = 0f
            val totalSteps = 40
            val stepMs = (message.voiceDurationSec * 1000L) / totalSteps
            while (playbackProgress < 1f && isPlaying) {
                delay(stepMs)
                playbackProgress += (1f / totalSteps)
            }
            isPlaying = false
            playbackProgress = 0f
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(min = 180.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (message.isMine) Color.White else DarkAccent
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Audio Waveform Visualization
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            val amplitudes = message.voiceAmplitudes.ifEmpty {
                listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.9f, 0.7f, 0.5f, 0.8f, 0.3f, 0.5f)
            }
            amplitudes.forEachIndexed { index, amp ->
                val barProgress = index.toFloat() / amplitudes.size
                val isPassed = barProgress <= playbackProgress
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((amp * 26).dp.coerceAtLeast(6.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isPassed) {
                                if (message.isMine) Color.White else AudioWavePurple
                            } else {
                                if (message.isMine) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "0:${message.voiceDurationSec.toString().padStart(2, '0')}",
            fontSize = 11.sp,
            color = if (message.isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
