package com.example.model

/**
 * User Profile data model for NOICE LIVE social platform.
 * Stores core identity (username, display name / full name, avatar URI/URL, bio),
 * registration and new user onboarding status, privacy settings, follower counts,
 * and VIP membership level.
 */
data class UserProfile(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isNewUser: Boolean = false,
    val joinedAtMillis: Long = System.currentTimeMillis(),
    val profileSetupCompleted: Boolean = false,
    val isPrivate: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val coins: Long = 1000L,
    val vipLevel: Int = 1,
    val vipTier: VipTier = VipTier.MEMBER,
    val country: String = "Global",
    val gender: String = "Unspecified"
) {
    val displayName: String
        get() = fullName.ifBlank { username.ifBlank { "User" } }

    /**
     * Converts to Firestore-compatible Map for persisting under `users/{id}`.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "username" to username,
            "username_lower" to username.lowercase(),
            "fullName" to fullName,
            "avatarUrl" to avatarUrl,
            "bio" to bio,
            "isNewUser" to isNewUser,
            "joinedAtMillis" to joinedAtMillis,
            "profileSetupCompleted" to profileSetupCompleted,
            "isPrivate" to isPrivate,
            "followersCount" to followersCount,
            "followingCount" to followingCount,
            "coins" to coins,
            "vipLevel" to vipLevel,
            "vipTier" to vipTier.name,
            "country" to country,
            "gender" to gender
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            val tierName = map["vipTier"] as? String ?: VipTier.MEMBER.name
            val tier = try {
                VipTier.valueOf(tierName)
            } catch (e: Exception) {
                VipTier.MEMBER
            }
            return UserProfile(
                id = map["id"] as? String ?: "",
                username = map["username"] as? String ?: "",
                fullName = map["fullName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String ?: "",
                bio = map["bio"] as? String ?: "",
                isNewUser = (map["isNewUser"] as? Boolean) ?: false,
                joinedAtMillis = (map["joinedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                profileSetupCompleted = (map["profileSetupCompleted"] as? Boolean) ?: false,
                isPrivate = (map["isPrivate"] as? Boolean) ?: false,
                followersCount = (map["followersCount"] as? Number)?.toInt() ?: 0,
                followingCount = (map["followingCount"] as? Number)?.toInt() ?: 0,
                coins = (map["coins"] as? Number)?.toLong() ?: 1000L,
                vipLevel = (map["vipLevel"] as? Number)?.toInt() ?: 1,
                vipTier = tier,
                country = map["country"] as? String ?: "Global",
                gender = map["gender"] as? String ?: "Unspecified"
            )
        }
    }
}
