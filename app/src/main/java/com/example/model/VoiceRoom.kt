package com.example.model

enum class ParticipantRole {
    OWNER,
    CO_HOST,
    MODERATOR,
    SPEAKER,
    LISTENER
}

enum class VipTier(val title: String, val badgeColorHex: String) {
    NONE("Member", "#71717A"),
    BRONZE("Bronze VIP", "#CD7F32"),
    SILVER("Silver VIP", "#C0C0C0"),
    GOLD("Gold VIP", "#FFD700"),
    DIAMOND("Diamond VIP", "#00B4D8"),
    CROWN_SUPREME("Crown Supreme", "#FF2D55")
}

enum class RoomCategory(val displayName: String, val icon: String) {
    TRENDING("Trending", "🔥"),
    POPULAR("Popular", "⭐"),
    NEW("New", "✨"),
    VIP("VIP Lounge", "👑"),
    MUSIC("Music & Singing", "🎵"),
    FRIENDSHIP("Friendship", "🤝"),
    ENTERTAINMENT("Entertainment", "🎭"),
    GAMING("Gaming", "🎮"),
    REGIONAL("Global & Regional", "🌍")
}

enum class RoomBackground(val displayName: String, val primaryColorHex: String, val secondaryColorHex: String) {
    DEFAULT_DARK("Velvet Night", "#121218", "#0A0A0F"),
    LUXURY_PALACE("Royal Palace", "#2C1226", "#140712"),
    CYBER_NEON("Cyberpunk Neon", "#0B1D3A", "#060E1D"),
    SUNSET_LOUNGE("Sunset Oasis", "#38171E", "#18080C"),
    GOLDEN_VIP("Golden Monarch", "#332408", "#140E02"),
    RETRO_DISCO("Disco Mirage", "#200E33", "#0E0518")
}

enum class WebRtcConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting LiveKit WebRTC..."),
    CONNECTED("Connected (LiveKit WebRTC Low-Latency)"),
    RECONNECTING("Reconnecting WebRTC Session...")
}

data class RoomStats(
    val bitrateKbps: Int = 96,
    val latencyMs: Int = 24,
    val packetLossPercent: Float = 0.05f,
    val audioCodec: String = "Opus 48kHz Stereo HD",
    val region: String = "Global Low-Latency WebRTC Edge"
)

data class Participant(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val role: ParticipantRole = ParticipantRole.LISTENER,
    val vipTier: VipTier = VipTier.NONE,
    val userLevel: Int = 5,
    val wealthLevel: Int = 3,
    val charmLevel: Int = 4,
    val isMuted: Boolean = false,
    val isTalking: Boolean = false,
    val audioLevel: Float = 0f,
    val handRaised: Boolean = false,
    val slotIndex: Int = -1,
    val avatarFrame: String = ""
)

data class SpeakerSlot(
    val slotIndex: Int,
    val participant: Participant? = null,
    val isLocked: Boolean = false,
    val isMutedByMod: Boolean = false,
    val label: String = "Slot ${slotIndex + 1}"
)

data class RoomSettings(
    val name: String,
    val coverUrl: String = "",
    val description: String = "",
    val announcement: String = "Welcome to our voice room! Be respectful and have fun.",
    val seatCount: Int = 8, // 4, 6, 8, 9, 12
    val category: RoomCategory = RoomCategory.TRENDING,
    val language: String = "English",
    val tags: List<String> = listOf("Chat", "Music", "Vibes"),
    val isPrivate: Boolean = false,
    val password: String = "",
    val requireApprovalToSpeak: Boolean = false,
    val minLevelToSpeak: Int = 1,
    val prohibitedWords: List<String> = listOf("spam", "abuse", "scam"),
    val blockedUserIds: List<String> = emptyList(),
    val moderatorIds: List<String> = emptyList(),
    val coHostIds: List<String> = emptyList(),
    val background: RoomBackground = RoomBackground.DEFAULT_DARK,
    val soundEffectsEnabled: Boolean = true,
    val giftsEnabled: Boolean = true
)

data class VoiceRoom(
    val id: String,
    val numericRoomId: String = "889420",
    val title: String,
    val topicTag: String,
    val category: RoomCategory = RoomCategory.TRENDING,
    val hostId: String,
    val hostName: String,
    val hostAvatar: String,
    val coverUrl: String = "",
    val isLive: Boolean = true,
    val isVip: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val createdAtText: String = "Live",
    val speakers: List<Participant> = emptyList(),
    val listeners: List<Participant> = emptyList(),
    val totalSlots: Int = 8,
    val lockedSlotIndices: Set<Int> = emptySet(),
    val isJoined: Boolean = false,
    val isMuted: Boolean = false,
    val isHandRaised: Boolean = false,
    val connectionState: WebRtcConnectionState = WebRtcConnectionState.DISCONNECTED,
    val stats: RoomStats = RoomStats(),
    val settings: RoomSettings = RoomSettings(name = title, seatCount = totalSlots),
    val charmPoints: Int = 1420,
    val isFavorite: Boolean = false
)

enum class FallaGiftCategory(val title: String) {
    POPULAR("Popular"),
    NORMAL("Normal"),
    PREMIUM("Premium"),
    LUXURY("Luxury"),
    VIP("VIP"),
    ANIMATED("Animated")
}

data class FallaGift(
    val id: String,
    val name: String,
    val icon: String,
    val coinCost: Int,
    val category: FallaGiftCategory,
    val animationEffect: String
)

data class GiftTransaction(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val receiverId: String,
    val receiverName: String,
    val gift: FallaGift,
    val quantity: Int = 1,
    val totalCoins: Int,
    val timestampText: String = "Just now",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class InRoomChatMessage(
    val id: String,
    val roomId: String,
    val senderName: String,
    val senderAvatar: String,
    val vipTier: VipTier = VipTier.NONE,
    val userLevel: Int = 1,
    val text: String,
    val isSystem: Boolean = false,
    val isGiftAnnouncement: Boolean = false
)

enum class RoomRankingPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class RoomRankingEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val vipTier: VipTier,
    val score: Long,
    val scoreLabel: String
)

enum class InRoomGameType(val title: String, val icon: String, val desc: String) {
    LUCKY_DICE("Lucky Dice", "🎲", "Roll dice with room members for coin jackpots"),
    TIC_TAC_TOE("Tic-Tac-Toe", "❌", "Multiplayer 3x3 battle on the voice stage"),
    TRIVIA_QUIZ("Trivia Royale", "💡", "Compete in live quiz questions & win coins")
}

data class ModerationAction(
    val id: String,
    val moderatorName: String,
    val targetUserName: String,
    val actionType: String, // Mute, Kick, Ban, Seat Lock
    val timestampText: String = "Just now"
)
