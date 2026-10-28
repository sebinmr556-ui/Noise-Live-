package com.example.model

enum class MessageType {
    TEXT,
    VOICE_NOTE,
    PHOTO
}

enum class OnlinePresenceState(val label: String, val colorHex: String) {
    ONLINE("Active now", "#10B981"),
    AWAY("Active 15m ago", "#F59E0B"),
    DO_NOT_DISTURB("Do not disturb", "#EF4444"),
    OFFLINE("Offline", "#71717A")
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String = "",
    val timestampText: String = "10:24 AM",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isMine: Boolean,
    val isRead: Boolean = true,
    val messageType: MessageType = MessageType.TEXT,
    val voiceDurationSec: Int = 0,
    val voiceAmplitudes: List<Float> = emptyList(),
    val audioFilePath: String? = null,
    val photoUrl: String? = null,
    val reactionEmoji: String? = null
)

data class Conversation(
    val id: String,
    val participantId: String,
    val participantName: String,
    val participantUsername: String,
    val participantAvatar: String,
    val isOnline: Boolean = true,
    val presenceState: OnlinePresenceState = if (isOnline) OnlinePresenceState.ONLINE else OnlinePresenceState.OFFLINE,
    val lastActiveText: String = if (isOnline) "Active now" else "Active 2h ago",
    val lastMessageText: String = "",
    val lastMessageTimestamp: String = "5m",
    val unreadCount: Int = 0
) {
    val isUserActiveNow: Boolean
        get() = isOnline || presenceState == OnlinePresenceState.ONLINE
}
