package com.example.model

enum class ParticipantRole {
    OWNER,
    HOST,
    CO_HOST,
    SPEAKER,
    LISTENER
}

data class Participant(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val role: ParticipantRole = ParticipantRole.LISTENER,
    val slotIndex: Int = -1,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val isHandRaised: Boolean = false,
    val handRaised: Boolean = isHandRaised,
    val audioLevel: Float = 0f,
    val vipLevel: Int = 1,
    val vipTier: VipTier = VipTier.MEMBER,
    val isNewUser: Boolean = false,
    val showNewUserBadge: Boolean = isNewUser,
    val joinedAtMillis: Long = System.currentTimeMillis(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val userLevel: Int = 1,
    val wealthLevel: Int = 1,
    val charmLevel: Int = 1,
    val giftsReceivedCoins: Long = 0L
)
