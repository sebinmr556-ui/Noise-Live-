package com.example.data

import com.example.model.*

object SampleData {
    // Authentic User Profile
    val currentUser = UserProfile(
        id = "user_me",
        numericId = "8294102",
        username = "creative_lens",
        fullName = "Creative Lens",
        avatarUrl = "", // Empty avatar prompts user to set or displays modern monogram
        bio = "Authentic moments captured in dark mode 📸 ✨\nSharing creativity on NOICE LIVE.",
        website = "",
        pronouns = "",
        category = "Creator",
        isVerified = false,
        followersCount = 0,
        followingCount = 0,
        postsCount = 0,
        isFollowing = false,
        isCurrentUser = true,
        bannerAccentColorHex = "#DC2743",
        coinBalance = 15000L,
        wealthPoints = 28500L,
        wealthLevel = 5,
        charmPoints = 19200L,
        charmLevel = 4,
        userXp = 4200L,
        userLevel = 6,
        vipTierLevel = 2,
        isAdmin = true,
        isSuperAdmin = true,
        isAuthenticated = true
    )

    // No dummy accounts or placeholder posts
    val users = listOf(currentUser)
    val stories = emptyList<Story>()
    val posts = emptyList<Post>()
    val highlights = emptyList<StoryHighlight>()

    // Falla-Style Social Voice Rooms
    val voiceRooms = listOf(
        VoiceRoom(
            id = "room_falla_1",
            numericRoomId = "772910",
            title = "Arabian Nights & Poetry Lounge 🌙",
            topicTag = "Regional",
            category = RoomCategory.REGIONAL,
            hostId = "host_amira",
            hostName = "Amira Al-Noor",
            hostAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600",
            isLive = true,
            isVip = false,
            createdAtText = "Live 45m",
            totalSlots = 8,
            charmPoints = 8420,
            speakers = listOf(
                Participant(
                    id = "host_amira",
                    name = "Amira",
                    username = "amira_voice",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                    role = ParticipantRole.OWNER,
                    vipTier = VipTier.DIAMOND,
                    userLevel = 28,
                    wealthLevel = 22,
                    charmLevel = 31,
                    isMuted = false,
                    isTalking = true,
                    audioLevel = 0.65f,
                    slotIndex = 0
                ),
                Participant(
                    id = "spk_tariq",
                    name = "Tariq",
                    username = "tariq_oud",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                    role = ParticipantRole.SPEAKER,
                    vipTier = VipTier.GOLD,
                    userLevel = 19,
                    isMuted = false,
                    isTalking = false,
                    audioLevel = 0.1f,
                    slotIndex = 1
                )
            ),
            listeners = listOf(
                Participant(
                    id = "lis_1",
                    name = "Layla",
                    username = "layla_star",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                    vipTier = VipTier.SILVER,
                    userLevel = 8
                ),
                Participant(
                    id = "lis_2",
                    name = "Zaid",
                    username = "zaid_beats",
                    avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200",
                    vipTier = VipTier.BRONZE,
                    userLevel = 11
                )
            ),
            settings = RoomSettings(
                name = "Arabian Nights & Poetry Lounge 🌙",
                seatCount = 8,
                category = RoomCategory.REGIONAL,
                announcement = "Welcome to the Poetry & Music Majlis. Recite or enjoy the acoustic vibes!",
                background = RoomBackground.SUNSET_LOUNGE
            )
        ),
        VoiceRoom(
            id = "room_falla_2",
            numericRoomId = "549218",
            title = "Global Karaoke & Singing Star 🎤",
            topicTag = "Music",
            category = RoomCategory.MUSIC,
            hostId = "host_sarah",
            hostName = "Sarah Melody",
            hostAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
            isLive = true,
            isVip = false,
            createdAtText = "Live 1h 20m",
            totalSlots = 8,
            charmPoints = 15900,
            speakers = listOf(
                Participant(
                    id = "host_sarah",
                    name = "Sarah",
                    username = "sarah_sing",
                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
                    role = ParticipantRole.OWNER,
                    vipTier = VipTier.CROWN_SUPREME,
                    userLevel = 35,
                    wealthLevel = 29,
                    charmLevel = 42,
                    isMuted = false,
                    isTalking = true,
                    audioLevel = 0.85f,
                    slotIndex = 0
                )
            ),
            listeners = listOf(
                Participant(
                    id = "lis_3",
                    name = "Karim",
                    username = "karim_music",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                    vipTier = VipTier.GOLD,
                    userLevel = 14
                )
            ),
            settings = RoomSettings(
                name = "Global Karaoke & Singing Star 🎤",
                seatCount = 8,
                category = RoomCategory.MUSIC,
                announcement = "Take a mic slot to sing your favorite song! Gifts and applause welcome 👏",
                background = RoomBackground.CYBER_NEON
            )
        ),
        VoiceRoom(
            id = "room_falla_3",
            numericRoomId = "100001",
            title = "VIP Royal Diamond Palace 👑",
            topicTag = "VIP Lounge",
            category = RoomCategory.VIP,
            hostId = "host_sultan",
            hostName = "Sultan Crown",
            hostAvatar = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200",
            coverUrl = "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=600",
            isLive = true,
            isVip = true,
            isPasswordProtected = false,
            createdAtText = "Live 3h",
            totalSlots = 9,
            charmPoints = 58900,
            speakers = listOf(
                Participant(
                    id = "host_sultan",
                    name = "Sultan",
                    username = "sultan_vip",
                    avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200",
                    role = ParticipantRole.OWNER,
                    vipTier = VipTier.CROWN_SUPREME,
                    userLevel = 50,
                    wealthLevel = 50,
                    charmLevel = 50,
                    isMuted = false,
                    isTalking = false,
                    audioLevel = 0.1f,
                    slotIndex = 0
                )
            ),
            listeners = emptyList(),
            settings = RoomSettings(
                name = "VIP Royal Diamond Palace 👑",
                seatCount = 9,
                category = RoomCategory.VIP,
                announcement = "Luxury VIP lounge for high contributors and top ranked members.",
                background = RoomBackground.GOLDEN_VIP
            )
        ),
        VoiceRoom(
            id = "room_falla_4",
            numericRoomId = "338914",
            title = "Lucky Dice & Ludo Battle 🎲",
            topicTag = "Gaming",
            category = RoomCategory.GAMING,
            hostId = "host_omar",
            hostName = "Omar Games",
            hostAvatar = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200",
            coverUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=600",
            isLive = true,
            isVip = false,
            createdAtText = "Live 25m",
            totalSlots = 6,
            charmPoints = 6200,
            speakers = listOf(
                Participant(
                    id = "host_omar",
                    name = "Omar",
                    username = "omar_dice",
                    avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200",
                    role = ParticipantRole.OWNER,
                    vipTier = VipTier.SILVER,
                    userLevel = 16,
                    isMuted = false,
                    isTalking = true,
                    audioLevel = 0.5f,
                    slotIndex = 0
                )
            ),
            listeners = emptyList(),
            settings = RoomSettings(
                name = "Lucky Dice & Ludo Battle 🎲",
                seatCount = 6,
                category = RoomCategory.GAMING,
                announcement = "Join our in-room Lucky Dice & Tic-Tac-Toe games! Winner takes coins 🏆",
                background = RoomBackground.RETRO_DISCO
            )
        )
    )

    val liveStreams = emptyList<LiveStream>()
    val conversations = emptyList<Conversation>()
    val initialMessages = emptyMap<String, List<ChatMessage>>()

    val initialNotifications = listOf(
        ActivityNotification(
            id = "notif_welcome",
            type = ActivityType.FOLLOW,
            actorName = "NOICE LIVE Team",
            actorUsername = "noicelive",
            actorAvatar = "",
            text = "Welcome to NOICE LIVE! Explore voice chatrooms, send gifts, and play live games.",
            timestampText = "1h",
            timestampMillis = System.currentTimeMillis() - 3600000
        )
    )
}
