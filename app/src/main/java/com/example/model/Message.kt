package com.example.model

enum class MessageType {
    TEXT,
    VOICE_NOTE,
    PHOTO
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
    val lastActiveText: String = "Active now",
    val lastMessageText: String = "",
    val lastMessageTimestamp: String = "5m",
    val unreadCount: Int = 0
)
