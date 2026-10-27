package com.example.model

enum class GiftType(val label: String, val icon: String, val coins: Int) {
    COFFEE("Cold Brew", "☕", 10),
    HEART_BURST("Super Heart", "💖", 25),
    FIRE("Fire Hype", "🔥", 50),
    ROCKET("Wave Rocket", "🚀", 100),
    DIAMOND("Crown Gem", "💎", 500)
}

data class StreamGift(
    val id: String,
    val type: GiftType,
    val senderName: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class LiveComment(
    val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String,
    val text: String,
    val isHost: Boolean = false,
    val isPinned: Boolean = false,
    val timestampText: String = "Now"
)

data class LiveStream(
    val id: String,
    val hostId: String,
    val hostName: String,
    val hostUsername: String,
    val hostAvatar: String,
    val title: String,
    val category: String,
    val viewerCount: Int = 1842,
    val isLive: Boolean = true,
    val startedAtText: String = "Live for 12m",
    val streamPreviewUrl: String,
    val comments: List<LiveComment> = emptyList(),
    val totalLikes: Int = 5420
)
