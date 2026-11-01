package com.example.model

data class FloatingRoomReaction(
    val id: String = "",
    val emoji: String = "❤️",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
