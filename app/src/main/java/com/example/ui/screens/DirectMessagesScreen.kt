package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.model.Conversation
import com.example.model.OnlinePresenceState
import com.example.ui.components.NewMessageBottomSheet
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.WebRtcConnectedGreen
import com.example.util.VoiceNoteManager

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
    val suggestedCreators by repository.suggestedCreators.collectAsState()
    val myOnlinePresence by repository.myOnlinePresence.collectAsState()

    var activeConversationId by remember { mutableStateOf<String?>(null) }
    var showNewMessageSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ONLINE, UNREAD
    var showPresenceMenu by remember { mutableStateOf(false) }

    val voiceNoteManager = remember { VoiceNoteManager() }
    DisposableEffect(Unit) {
        onDispose {
            voiceNoteManager.release()
        }
    }

    val activeConversation = conversations.find { it.id == activeConversationId }

    if (activeConversation != null) {
        // Active Chat Detail Screen
        LaunchedEffect(activeConversation.id) {
            repository.markConversationAsRead(activeConversation.id)
        }

        ChatDetailScreen(
            conversation = activeConversation,
            messages = chatMessagesMap[activeConversation.id] ?: emptyList(),
            currentUser = currentUser,
            voiceNoteManager = voiceNoteManager,
            onBack = { activeConversationId = null },
            onSendMessage = { text ->
                repository.sendTextMessage(activeConversation.id, text)
            },
            onSendVoiceNote = { durationSec, amplitudes, filePath ->
                repository.sendVoiceNote(activeConversation.id, durationSec, amplitudes, filePath)
            },
            onSendPhoto = { photoUrl ->
                repository.sendPhotoMessage(activeConversation.id, photoUrl)
            },
            onReact = { messageId, emoji ->
                repository.reactToMessage(activeConversation.id, messageId, emoji)
            }
        )
    } else {
        // Inbox Conversations Screen
        val onlineConversations = remember(conversations) {
            conversations.filter { it.isUserActiveNow }
        }

        val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
            conversations.filter { conv ->
                val matchesSearch = searchQuery.isBlank() ||
                        conv.participantName.contains(searchQuery, ignoreCase = true) ||
                        conv.participantUsername.contains(searchQuery, ignoreCase = true) ||
                        conv.lastMessageText.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "ONLINE" -> conv.isUserActiveNow
                    "UNREAD" -> conv.unreadCount > 0
                    else -> true
                }
                matchesSearch && matchesFilter
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentUser.username.isNotBlank()) currentUser.username else "Messages",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // My Online Presence Chip (Clickable to switch status)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { showPresenceMenu = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (myOnlinePresence) {
                                                        OnlinePresenceState.ONLINE -> WebRtcConnectedGreen
                                                        OnlinePresenceState.AWAY -> Color(0xFFF59E0B)
                                                        OnlinePresenceState.DO_NOT_DISTURB -> Color(0xFFEF4444)
                                                        OnlinePresenceState.OFFLINE -> Color.Gray
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = myOnlinePresence.label.split(" ").firstOrNull() ?: "Online",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showPresenceMenu,
                                        onDismissRequest = { showPresenceMenu = false }
                                    ) {
                                        OnlinePresenceState.values().forEach { state ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (state) {
                                                                        OnlinePresenceState.ONLINE -> WebRtcConnectedGreen
                                                                        OnlinePresenceState.AWAY -> Color(0xFFF59E0B)
                                                                        OnlinePresenceState.DO_NOT_DISTURB -> Color(0xFFEF4444)
                                                                        OnlinePresenceState.OFFLINE -> Color.Gray
                                                                    }
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(state.label, fontSize = 13.sp)
                                                    }
                                                },
                                                onClick = {
                                                    repository.setMyOnlinePresence(state)
                                                    showPresenceMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNewMessageSheet = true },
                            modifier = Modifier.testTag("compose_message_button")
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "New Message",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
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
                // Search bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search chats or messages...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("inbox_search_input")
                    )
                }

                // Active Now Stories/Avatars Strip
                item {
                    Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(WebRtcConnectedGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Now (${onlineConversations.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(onlineConversations, key = { it.id }) { conv ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { activeConversationId = conv.id }
                                        .testTag("active_friend_${conv.id}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.BottomEnd,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        AsyncImage(
                                            model = conv.participantAvatar,
                                            contentDescription = conv.participantName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                        // Glowing green active indicator
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

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Filter Chips (All, Online, Unread)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "ONLINE",
                            onClick = { selectedFilter = "ONLINE" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(WebRtcConnectedGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active Now (${onlineConversations.size})", fontSize = 12.sp)
                                }
                            }
                        )
                        val unreadCount = conversations.count { it.unreadCount > 0 }
                        FilterChip(
                            selected = selectedFilter == "UNREAD",
                            onClick = { selectedFilter = "UNREAD" },
                            label = { Text("Unread ($unreadCount)", fontSize = 12.sp) }
                        )
                    }
                }

                // Conversations List
                if (filteredConversations.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No messages found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Send voice notes, photos, and messages to your friends and creators.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showNewMessageSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send a Message")
                            }
                        }
                    }
                } else {
                    items(filteredConversations, key = { it.id }) { conv ->
                        ConversationRowItem(
                            conversation = conv,
                            onClick = { activeConversationId = conv.id }
                        )
                    }
                }
            }
        }
    }

    // New Message Bottom Sheet
    if (showNewMessageSheet) {
        NewMessageBottomSheet(
            availableUsers = suggestedCreators,
            onSelectUser = { user ->
                val newConv = repository.startOrGetConversation(user)
                activeConversationId = newConv.id
                showNewMessageSheet = false
            },
            onDismiss = { showNewMessageSheet = false }
        )
    }
}

@Composable
private fun ConversationRowItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("conversation_row_${conversation.id}")
    ) {
        // Participant Avatar with Online Status Indicator
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
            if (conversation.isUserActiveNow) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.participantName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (conversation.isUserActiveNow) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(WebRtcConnectedGreen)
                    )
                }
            }
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
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(DarkAccent)
            )
        }
    }
}
