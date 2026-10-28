package com.example.data

import com.example.model.*

object SampleData {
    // Authentic User Profile (clean initial state, populated upon Google Sign-In)
    val currentUser = UserProfile(
        id = "user_guest",
        numericId = "1000001",
        username = "guest",
        fullName = "Guest User",
        avatarUrl = "",
        bio = "",
        website = "",
        pronouns = "",
        category = "Member",
        isVerified = false,
        followersCount = 0,
        followingCount = 0,
        postsCount = 0,
        isFollowing = false,
        isCurrentUser = true,
        bannerAccentColorHex = "#DC2743",
        coinBalance = 0L,
        wealthPoints = 0L,
        wealthLevel = 1,
        charmPoints = 0L,
        charmLevel = 1,
        userXp = 0L,
        userLevel = 1,
        vipTierLevel = 0,
        isAdmin = false,
        isSuperAdmin = false,
        isAuthenticated = false
    )

    // No dummy accounts or placeholder posts
    val users = emptyList<UserProfile>()
    val stories = emptyList<Story>()
    val posts = emptyList<Post>()
    val highlights = emptyList<StoryHighlight>()

    // Clean initial chatrooms: no dummy accounts or fake rooms
    val voiceRooms = emptyList<VoiceRoom>()

    val liveStreams = emptyList<LiveStream>()
    val conversations = emptyList<Conversation>()
    val initialMessages = emptyMap<String, List<ChatMessage>>()
    val initialNotifications = emptyList<ActivityNotification>()
}
