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

enum class RoomBackground(
    val id: String,
    val displayName: String,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val backgroundAssetUrl: String = "",
    val coinCost: Int = 0,
    val isPremium: Boolean = true
) {
    DEFAULT_DARK(
        id = "velvet_night",
        displayName = "Velvet Night",
        primaryColorHex = "#121218",
        secondaryColorHex = "#0A0A0F",
        coinCost = 0,
        isPremium = false
    ),
    ROYAL_CAR_GOLD(
        id = "royal_car_gold",
        displayName = "Golden Monarch Car",
        primaryColorHex = "#332408",
        secondaryColorHex = "#140E02",
        backgroundAssetUrl = "https://example.com/themes/golden_car_anim.gif",
        coinCost = 9999
    ),
    LUXURY_PALACE(
        id = "mystic_palace",
        displayName = "Royal Palace Glow",
        primaryColorHex = "#2C1226",
        secondaryColorHex = "#140712",
        backgroundAssetUrl = "https://example.com/themes/palace_anim.gif",
        coinCost = 5000
    ),
    CYBER_NEON(
        id = "cyber_neon",
        displayName = "Cyberpunk Neon 3D",
        primaryColorHex = "#0B1D3A",
        secondaryColorHex = "#060E1D",
        backgroundAssetUrl = "https://example.com/themes/cyber_neon_anim.gif",
        coinCost = 3000
    )
}

enum class WebRtcConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting LiveKit WebRTC..."),
    CONNECTED("Connected (LiveKit WebRTC Low-Latency)"),
    RECONNECTING("Reconnecting WebRTC Session...")
}

// Ultra Luxury Audio Visualizer & Animation Config
data class AudioVisualizerState(
    val isAnimating: Boolean = true,
    val waveIntensity: Float = 0.85f,
    val primaryGlowColor: String = "#FF2D55",
    val secondaryGlowColor: String = "#FFD700",
    val particleEffectName: String = "Gold_Sparkles_And_Neon_Waves"
)

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
    val avatarFrame: String = "",
    val visualizerState: AudioVisualizerState = AudioVisualizerState()
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
    val seatCount: Int = 8,
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
    val background: RoomBackground = RoomBackground.ROYAL_CAR_GOLD,
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
    val isVip: Boolean = true,
    val isPasswordProtected: Boolean = false,
    val createdAtText: String = "Live",
    val speakers: List<Participant> = emptyList(),
    val listeners: List<Participant> = emptyList(),
    val totalSlots: Int = 8,
    val lockedSlotIndices: Set<Int> = emptySet(),
    val isJoined: Boolean = false,
    val isMuted: Boolean = false,
    val isHandRaised: Boolean = false,
    val connectionState: WebRtcConnectionState = WebRtcConnectionState.CONNECTED,
    val stats: RoomStats = RoomStats(),
    val settings: RoomSettings = RoomSettings(name = title, seatCount = totalSlots),
    val charmPoints: Int = 4850,
    val isFavorite: Boolean = true
)

enum class FallaGiftCategory(val title: String) {
    POPULAR("Popular"),
    NORMAL("Normal"),
    PREMIUM("Premium"),
    LUXURY("Luxury"),
    VIP("VIP"),
    ANIMATED("Animated Super Car")
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
    val vipTier: VipTier = VipTier.CROWN_SUPREME,
    val userLevel: Int = 99,
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
    val actionType: String,
    val timestampText: String = "Just now"
)
