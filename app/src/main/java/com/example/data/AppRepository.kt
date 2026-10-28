package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminAuditLogEntry(
    val id: String,
    val action: String,
    val adminId: String,
    val targetId: String,
    val details: String,
    val timestamp: Long
)

class AppRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val firestoreDb: FirebaseFirestore? = null
) {
    private val recommendationEngine = FeedRecommendationEngine()
    val liveKitManager = LiveKitManager(scope)

    // Firebase Firestore instance
    val firestore: FirebaseFirestore? = firestoreDb ?: run {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    // Firebase Auth instance
    private val firebaseAuth: FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    } ?: FirebaseAuth.getInstance()

    // User Profile State
    private val _currentUser = MutableStateFlow(SampleData.currentUser)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _authPromptRequest = MutableStateFlow<String?>(null)
    val authPromptRequest: StateFlow<String?> = _authPromptRequest.asStateFlow()

    private val _followedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followedUserIds: StateFlow<Set<String>> = _followedUserIds.asStateFlow()

    private val _suggestedCreators = MutableStateFlow(
        listOf(
            UserProfile(
                id = "creator_elena_v",
                numericId = "7392014",
                username = "elena_vision",
                fullName = "Elena Vance",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                bio = "Visual storyteller & digital creator 📸 Live audio host",
                category = "Creator",
                isVerified = true,
                followersCount = 4820,
                followingCount = 312,
                postsCount = 18,
                userLevel = 12,
                wealthLevel = 8,
                charmLevel = 14
            ),
            UserProfile(
                id = "creator_kai_sound",
                numericId = "8492011",
                username = "kai_audio",
                fullName = "Kai Sterling",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
                bio = "Music producer • Supercar lounge resident DJ 🎧",
                category = "Musician",
                isVerified = true,
                followersCount = 12500,
                followingCount = 480,
                postsCount = 34,
                userLevel = 18,
                wealthLevel = 15,
                charmLevel = 22
            ),
            UserProfile(
                id = "creator_maya_art",
                numericId = "9102834",
                username = "maya_craft",
                fullName = "Maya Chen",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80",
                bio = "Cyber aesthetic art & 3D renders ✨ Live streaming daily",
                category = "Artist",
                isVerified = false,
                followersCount = 2890,
                followingCount = 190,
                postsCount = 12,
                userLevel = 7,
                wealthLevel = 5,
                charmLevel = 9
            ),
            UserProfile(
                id = "creator_zane_apex",
                numericId = "6029481",
                username = "zane_apex",
                fullName = "Zane Miller",
                avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80",
                bio = "Motorsport & exotic car photography 🏎️ Track days",
                category = "Automotive",
                isVerified = true,
                followersCount = 19400,
                followingCount = 620,
                postsCount = 45,
                userLevel = 24,
                wealthLevel = 28,
                charmLevel = 30
            )
        )
    )
    val suggestedCreators: StateFlow<List<UserProfile>> = _suggestedCreators.asStateFlow()

    // Authentic Posts & Feed State
    private val _rawPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _selectedFeedFilter = MutableStateFlow(FeedFilter.FOR_YOU)
    val selectedFeedFilter: StateFlow<FeedFilter> = _selectedFeedFilter.asStateFlow()

    private val _displayedFeed = MutableStateFlow<List<Post>>(emptyList())
    val displayedFeed: StateFlow<List<Post>> = _displayedFeed.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    private val _userBookmarkedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userBookmarkedPostIds: StateFlow<Set<String>> = _userBookmarkedPostIds.asStateFlow()

    // Stories State with 24h Expiration
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _highlights = MutableStateFlow<List<StoryHighlight>>(emptyList())
    val highlights: StateFlow<List<StoryHighlight>> = _highlights.asStateFlow()

    // Comments for posts
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    // Notifications / Activity
    private val _notifications = MutableStateFlow(SampleData.initialNotifications)
    val notifications: StateFlow<List<ActivityNotification>> = _notifications.asStateFlow()

    // Voice Rooms
    private val _voiceRooms = MutableStateFlow(SampleData.voiceRooms)
    val voiceRooms: StateFlow<List<VoiceRoom>> = _voiceRooms.asStateFlow()

    // Live Video Streams
    private val _liveStreams = MutableStateFlow(SampleData.liveStreams)
    val liveStreams: StateFlow<List<LiveStream>> = _liveStreams.asStateFlow()

    private val _activeLiveStream = MutableStateFlow<LiveStream?>(null)
    val activeLiveStream: StateFlow<LiveStream?> = _activeLiveStream.asStateFlow()

    private val _streamGifts = MutableStateFlow<List<StreamGift>>(emptyList())
    val streamGifts: StateFlow<List<StreamGift>> = _streamGifts.asStateFlow()

    // Direct Messages & Online Presence
    private val _myOnlinePresence = MutableStateFlow(OnlinePresenceState.ONLINE)
    val myOnlinePresence: StateFlow<OnlinePresenceState> = _myOnlinePresence.asStateFlow()

    private val defaultConversations = listOf(
        Conversation(
            id = "conv_elena",
            participantId = "creator_elena_v",
            participantName = "Elena Vance",
            participantUsername = "elena_vision",
            participantAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
            isOnline = true,
            presenceState = OnlinePresenceState.ONLINE,
            lastActiveText = "Active now",
            lastMessageText = "🎤 Voice message (0:08)",
            lastMessageTimestamp = "2m",
            unreadCount = 1
        ),
        Conversation(
            id = "conv_kai",
            participantId = "creator_kai_sound",
            participantName = "Kai Sterling",
            participantUsername = "kai_audio",
            participantAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
            isOnline = true,
            presenceState = OnlinePresenceState.ONLINE,
            lastActiveText = "Active now",
            lastMessageText = "The supercar audio stage was insane tonight! 🎧",
            lastMessageTimestamp = "14m",
            unreadCount = 0
        ),
        Conversation(
            id = "conv_maya",
            participantId = "creator_maya_art",
            participantName = "Maya Chen",
            participantUsername = "maya_craft",
            participantAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80",
            isOnline = false,
            presenceState = OnlinePresenceState.AWAY,
            lastActiveText = "Active 18m ago",
            lastMessageText = "Sent you the 3D render preview ✨",
            lastMessageTimestamp = "1h",
            unreadCount = 0
        ),
        Conversation(
            id = "conv_zane",
            participantId = "creator_zane_apex",
            participantName = "Zane Miller",
            participantUsername = "zane_apex",
            participantAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80",
            isOnline = false,
            presenceState = OnlinePresenceState.OFFLINE,
            lastActiveText = "Active 3h ago",
            lastMessageText = "Next track day photoshoot is this Sunday 🏎️",
            lastMessageTimestamp = "3h",
            unreadCount = 0
        )
    )

    private val defaultInitialMessages = mapOf(
        "conv_elena" to listOf(
            ChatMessage(
                id = "msg_e_1",
                conversationId = "conv_elena",
                senderId = "creator_elena_v",
                senderName = "Elena Vance",
                senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                text = "Hey! Loved the energy in the live supercar chatroom earlier! 🔥",
                timestampText = "10:14 AM",
                isMine = false,
                isRead = true
            ),
            ChatMessage(
                id = "msg_e_2",
                conversationId = "conv_elena",
                senderId = "user_guest",
                senderName = "Me",
                senderAvatar = "",
                text = "Thanks Elena! The sound stage and background theme were awesome.",
                timestampText = "10:16 AM",
                isMine = true,
                isRead = true
            ),
            ChatMessage(
                id = "msg_e_3",
                conversationId = "conv_elena",
                senderId = "creator_elena_v",
                senderName = "Elena Vance",
                senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                text = "Voice message",
                timestampText = "10:18 AM",
                isMine = false,
                isRead = false,
                messageType = MessageType.VOICE_NOTE,
                voiceDurationSec = 8,
                voiceAmplitudes = listOf(0.4f, 0.7f, 0.9f, 0.6f, 0.8f, 0.5f, 0.9f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f)
            )
        ),
        "conv_kai" to listOf(
            ChatMessage(
                id = "msg_k_1",
                conversationId = "conv_kai",
                senderId = "creator_kai_sound",
                senderName = "Kai Sterling",
                senderAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
                text = "The supercar audio stage was insane tonight! 🎧",
                timestampText = "Yesterday",
                isMine = false,
                isRead = true
            ),
            ChatMessage(
                id = "msg_k_2",
                conversationId = "conv_kai",
                senderId = "creator_kai_sound",
                senderName = "Kai Sterling",
                senderAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
                text = "Audio clip",
                timestampText = "Yesterday",
                isMine = false,
                isRead = true,
                messageType = MessageType.VOICE_NOTE,
                voiceDurationSec = 14,
                voiceAmplitudes = listOf(0.5f, 0.8f, 0.95f, 0.7f, 0.85f, 0.6f, 0.9f, 0.75f, 0.65f, 0.8f, 0.4f, 0.7f, 0.9f, 0.5f)
            )
        )
    )

    private val _conversations = MutableStateFlow(defaultConversations)
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _chatMessages = MutableStateFlow(defaultInitialMessages)
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    // -------------------------------------------------------------
    // Virtual Economy, Ledger, VIP, & Levels
    // -------------------------------------------------------------
    private val _coinPackages = MutableStateFlow(
        listOf(
            CoinPackage("pkg_starter", "Starter Pack", 100L, 0L, 0.99),
            CoinPackage("pkg_basic", "Basic Pack", 500L, 25L, 4.99),
            CoinPackage("pkg_popular", "Popular Pack", 1200L, 100L, 9.99, badgeLabel = "POPULAR", isPopular = true),
            CoinPackage("pkg_premium", "Premium Pack", 3000L, 500L, 24.99, badgeLabel = "RECOMMENDED", isRecommended = true),
            CoinPackage("pkg_ultimate", "Ultimate Pack", 10000L, 2000L, 79.99, badgeLabel = "VIP BONUS"),
            CoinPackage("pkg_whale", "Whale Supreme", 50000L, 15000L, 349.99, badgeLabel = "BEST VALUE")
        )
    )
    val coinPackages: StateFlow<List<CoinPackage>> = _coinPackages.asStateFlow()

    private val _coinLedger = MutableStateFlow<List<CoinLedgerTransaction>>(emptyList())
    val coinLedger: StateFlow<List<CoinLedgerTransaction>> = _coinLedger.asStateFlow()

    private val _vipTiers = MutableStateFlow(VipTierConfig.ALL_TIERS)
    val vipTiers: StateFlow<List<VipTierConfig>> = _vipTiers.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow(
        listOf(
            AdminAuditLogEntry(
                id = "log_init",
                action = "SECURITY_POLICY_INIT",
                adminId = "root_superadmin",
                targetId = "system",
                details = "Initialized zero-trust Firestore security rules & verified LiveKit gateway token endpoints",
                timestamp = System.currentTimeMillis() - 7200000L
            )
        )
    )
    val adminAuditLogs: StateFlow<List<AdminAuditLogEntry>> = _adminAuditLogs.asStateFlow()

    // In-Room Chat & Gifts State
    private val _inRoomChatMessages = MutableStateFlow<Map<String, List<InRoomChatMessage>>>(emptyMap())
    val inRoomChatMessages: StateFlow<Map<String, List<InRoomChatMessage>>> = _inRoomChatMessages.asStateFlow()

    private val _giftTransactions = MutableStateFlow<List<GiftTransaction>>(emptyList())
    val giftTransactions: StateFlow<List<GiftTransaction>> = _giftTransactions.asStateFlow()

    private val _latestRoomGift = MutableStateFlow<GiftTransaction?>(null)
    val latestRoomGift: StateFlow<GiftTransaction?> = _latestRoomGift.asStateFlow()

    private val _favoriteRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteRoomIds: StateFlow<Set<String>> = _favoriteRoomIds.asStateFlow()

    private val _recentlyVisitedRoomIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyVisitedRoomIds: StateFlow<List<String>> = _recentlyVisitedRoomIds.asStateFlow()

    private val _moderationAuditLog = MutableStateFlow<List<ModerationAction>>(emptyList())
    val moderationAuditLog: StateFlow<List<ModerationAction>> = _moderationAuditLog.asStateFlow()

    private val _diceGameResult = MutableStateFlow<Pair<Int, Int>?>(null)
    val diceGameResult: StateFlow<Pair<Int, Int>?> = _diceGameResult.asStateFlow()

    private val _ticTacToeBoard = MutableStateFlow(List(9) { "" })
    val ticTacToeBoard: StateFlow<List<String>> = _ticTacToeBoard.asStateFlow()

    private val _triviaIndex = MutableStateFlow(0)
    val triviaIndex: StateFlow<Int> = _triviaIndex.asStateFlow()

    // Falla Gifts Catalog
    val fallaGifts = listOf(
        FallaGift("g_rose", "Rose", "🌹", 10, FallaGiftCategory.POPULAR, "rose_petal_burst"),
        FallaGift("g_coffee", "Arabic Coffee", "☕", 50, FallaGiftCategory.NORMAL, "steam_aroma"),
        FallaGift("g_wings", "Angel Wings", "🪽", 100, FallaGiftCategory.NORMAL, "wings_spread"),
        FallaGift("g_mic", "Golden Mic", "🎙️", 200, FallaGiftCategory.PREMIUM, "golden_soundwave"),
        FallaGift("g_ring", "Diamond Ring", "💍", 500, FallaGiftCategory.PREMIUM, "diamond_sparkle"),
        FallaGift("g_car", "Supercar", "🏎️", 1200, FallaGiftCategory.LUXURY, "car_driveby"),
        FallaGift("g_yacht", "Luxury Yacht", "🛥️", 3000, FallaGiftCategory.LUXURY, "ocean_waves"),
        FallaGift("g_crown", "Royal Crown", "👑", 5000, FallaGiftCategory.VIP, "crown_coronation"),
        FallaGift("g_rocket", "Galaxy Rocket", "🚀", 10000, FallaGiftCategory.ANIMATED, "rocket_launch")
    )

    init {
        // Listen to Firebase Auth state
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _isUserAuthenticated.value = true
                val existing = _currentUser.value
                val updated = existing.copy(
                    id = user.uid,
                    fullName = user.displayName ?: existing.fullName,
                    avatarUrl = user.photoUrl?.toString() ?: existing.avatarUrl,
                    isAuthenticated = true
                )
                _currentUser.value = updated
                startUserFirestoreSync(user.uid, user.displayName, user.photoUrl?.toString())
            }
        }
        setupFirestorePostsListener()
        refreshFeed()
    }

    private fun startUserFirestoreSync(uid: String, displayName: String?, photoUrl: String?) {
        val db = firestore ?: return
        val userRef = db.collection("users").document(uid)

        userRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val cur = _currentUser.value
                val bio = snapshot.getString("bio") ?: cur.bio
                val avatarUrl = snapshot.getString("avatarUrl") ?: cur.avatarUrl
                val fullName = snapshot.getString("fullName") ?: cur.fullName
                val username = snapshot.getString("username") ?: cur.username
                val website = snapshot.getString("website") ?: cur.website
                val numericId = snapshot.getString("numericId") ?: cur.numericId
                val bannerAccent = snapshot.getString("bannerAccentColorHex") ?: cur.bannerAccentColorHex
                val coinBalance = snapshot.getLong("coinBalance") ?: cur.coinBalance
                val postsCount = snapshot.getLong("postsCount")?.toInt() ?: cur.postsCount
                val followersCount = snapshot.getLong("followersCount")?.toInt() ?: cur.followersCount
                val followingCount = snapshot.getLong("followingCount")?.toInt() ?: cur.followingCount

                _currentUser.value = cur.copy(
                    id = uid,
                    bio = bio,
                    avatarUrl = avatarUrl,
                    fullName = fullName,
                    username = username,
                    website = website,
                    numericId = numericId,
                    bannerAccentColorHex = bannerAccent,
                    coinBalance = coinBalance,
                    postsCount = postsCount,
                    followersCount = followersCount,
                    followingCount = followingCount,
                    isAuthenticated = true
                )
            } else if (snapshot != null && !snapshot.exists()) {
                val initialNumericId = (1000000 + (Math.abs(uid.hashCode()) % 9000000)).toString()
                val initialUsername = (displayName?.replace("\\s+".toRegex(), "_")?.lowercase()
                    ?: "user_${uid.take(6)}")
                val initialData = hashMapOf<String, Any>(
                    "uid" to uid,
                    "numericId" to initialNumericId,
                    "username" to initialUsername,
                    "fullName" to (displayName ?: "NOICE Creator"),
                    "avatarUrl" to (photoUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80"),
                    "bio" to "Living life in high definition ✨ | NOICE LIVE Creator 🎤",
                    "website" to "",
                    "postsCount" to 0,
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "bannerAccentColorHex" to "#DC2743",
                    "coinBalance" to 0L,
                    "wealthLevel" to 1,
                    "charmLevel" to 1,
                    "userLevel" to 1,
                    "vipTierLevel" to 0,
                    "isAdmin" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                userRef.set(initialData)
            }
        }

        setupFirestoreFollowsListener(uid)
        setupFirestoreFollowersListener(uid)
    }

    private fun setupFirestoreFollowsListener(uid: String) {
        val db = firestore ?: return
        db.collection("follows")
            .whereEqualTo("followerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val followedIds = snapshot.documents.mapNotNull { it.getString("followingId") }.toSet()
                _followedUserIds.value = followedIds
                _currentUser.value = _currentUser.value.copy(followingCount = followedIds.size)
            }
    }

    private fun setupFirestoreFollowersListener(uid: String) {
        val db = firestore ?: return
        db.collection("follows")
            .whereEqualTo("followingId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val count = snapshot.size()
                _currentUser.value = _currentUser.value.copy(followersCount = count)
            }
    }

    private fun setupFirestorePostsListener() {
        val db = firestore ?: return
        db.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val firestorePosts = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
                    val caption = doc.getString("caption") ?: ""
                    val username = doc.getString("username") ?: "Creator"
                    val userAvatar = doc.getString("userAvatar") ?: ""
                    val location = doc.getString("location") ?: ""
                    val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                    val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val filterName = doc.getString("filterApplied") ?: PhotoFilter.NORMAL.name
                    val filter = try { PhotoFilter.valueOf(filterName) } catch (e: Exception) { PhotoFilter.NORMAL }

                    Post(
                        id = id,
                        userId = userId,
                        username = username,
                        userAvatar = userAvatar,
                        imageUrl = imageUrl,
                        caption = caption,
                        location = location,
                        likesCount = likesCount,
                        commentsCount = commentsCount,
                        timestampMillis = timestamp,
                        timestampText = formatTimestamp(timestamp),
                        tags = tags,
                        filterApplied = filter,
                        isLiked = _userLikedPostIds.value.contains(id),
                        isBookmarked = _userBookmarkedPostIds.value.contains(id)
                    )
                }
                if (firestorePosts.isNotEmpty()) {
                    _rawPosts.value = firestorePosts
                    refreshFeed()
                }
            }
    }

    private fun formatTimestamp(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / (60 * 1000)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            else -> "${minutes / 1440}d ago"
        }
    }

    // -------------------------------------------------------------
    // Auth & Permission Checks
    // -------------------------------------------------------------
    fun triggerAuthPrompt(action: String) {
        _authPromptRequest.value = action
    }

    fun clearAuthPrompt() {
        _authPromptRequest.value = null
    }

    fun signInWithGoogleIdToken(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            val cur = _currentUser.value
                            val newUser = cur.copy(
                                id = firebaseUser.uid,
                                fullName = firebaseUser.displayName ?: cur.fullName,
                                avatarUrl = firebaseUser.photoUrl?.toString() ?: cur.avatarUrl,
                                isAuthenticated = true
                            )
                            _currentUser.value = newUser
                            _isUserAuthenticated.value = true
                            startUserFirestoreSync(firebaseUser.uid, firebaseUser.displayName, firebaseUser.photoUrl?.toString())
                            clearAuthPrompt()
                            onComplete(true, null)
                        } else {
                            onComplete(false, "Authentication succeeded but user profile was null")
                        }
                    }
                    .addOnFailureListener { exception ->
                        onComplete(false, exception.localizedMessage ?: "Sign-in failed")
                    }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Authentication error")
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _isUserAuthenticated.value = false
        val anon = _currentUser.value.copy(
            isAuthenticated = false
        )
        _currentUser.value = anon
    }

    // -------------------------------------------------------------
    // Virtual Economy & Coin Shop
    // -------------------------------------------------------------
    fun purchaseCoinPackage(packageId: String, paymentMethod: String, onResult: (Boolean, String) -> Unit) {
        val pkg = _coinPackages.value.find { it.id == packageId }
        if (pkg == null) {
            onResult(false, "Package not found")
            return
        }

        scope.launch {
            delay(800) // Simulate verified secure payment gateway roundtrip
            val before = _currentUser.value.coinBalance
            val after = before + pkg.totalCoins
            val newWealthPoints = _currentUser.value.wealthPoints + pkg.totalCoins
            val newWealthLevel = LevelCalculator.calculateWealthLevel(newWealthPoints)

            val updatedUser = _currentUser.value.copy(
                coinBalance = after,
                wealthPoints = newWealthPoints,
                wealthLevel = newWealthLevel
            )
            _currentUser.value = updatedUser

            val tx = CoinLedgerTransaction(
                id = "tx_${UUID.randomUUID()}",
                userId = updatedUser.id,
                numericId = updatedUser.numericId,
                type = LedgerTransactionType.COIN_PURCHASE,
                amount = pkg.totalCoins,
                balanceBefore = before,
                balanceAfter = after,
                timestamp = System.currentTimeMillis(),
                metadata = "Verified purchase: ${pkg.name} ($%.2f via $paymentMethod)".format(pkg.priceUsd)
            )
            _coinLedger.value = listOf(tx) + _coinLedger.value

            logAdminAudit(
                action = "COIN_PURCHASE_VERIFIED",
                targetId = updatedUser.id,
                details = "Order ${tx.id}: Credited %,d coins for $%.2f".format(pkg.totalCoins, pkg.priceUsd)
            )
            onResult(true, "Purchase successful! %,d coins credited.".format(pkg.totalCoins))
        }
    }

    fun deductCoins(amount: Long, reason: String): Boolean {
        if (_currentUser.value.coinBalance < amount) return false
        val before = _currentUser.value.coinBalance
        val after = before - amount
        _currentUser.value = _currentUser.value.copy(coinBalance = after)
        val tx = CoinLedgerTransaction(
            id = "tx_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            numericId = _currentUser.value.numericId,
            type = LedgerTransactionType.GIFT_SENT,
            amount = -amount,
            balanceBefore = before,
            balanceAfter = after,
            timestamp = System.currentTimeMillis(),
            metadata = reason
        )
        _coinLedger.value = listOf(tx) + _coinLedger.value
        return true
    }

    fun purchaseVipTier(tierLevel: Int, durationMonths: Int, useCoins: Boolean, onResult: (Boolean, String) -> Unit) {
        val tier = VipTierConfig.getTier(tierLevel)
        if (tier == null) {
            onResult(false, "VIP Tier not found")
            return
        }

        val cost = when (durationMonths) {
            3 -> tier.coinPriceQuarterly
            12 -> tier.coinPriceAnnual
            else -> tier.coinPriceMonthly
        }

        if (useCoins && _currentUser.value.coinBalance < cost) {
            onResult(false, "Insufficient coin balance. You need %,d coins.".format(cost))
            return
        }

        scope.launch {
            val before = _currentUser.value.coinBalance
            val after = if (useCoins) before - cost else before
            val durationMillis = durationMonths.toLong() * 30L * 24 * 3600 * 1000
            val newExpiry = System.currentTimeMillis() + durationMillis
            val wealthGained = cost
            val newWealth = _currentUser.value.wealthPoints + wealthGained
            val newWealthLevel = LevelCalculator.calculateWealthLevel(newWealth)

            val updated = _currentUser.value.copy(
                coinBalance = after,
                vipTierLevel = tier.level,
                vipExpiryTimestamp = newExpiry,
                wealthPoints = newWealth,
                wealthLevel = newWealthLevel
            )
            _currentUser.value = updated

            val tx = CoinLedgerTransaction(
                id = "tx_${UUID.randomUUID()}",
                userId = updated.id,
                numericId = updated.numericId,
                type = LedgerTransactionType.VIP_PURCHASE,
                amount = -cost,
                balanceBefore = before,
                balanceAfter = after,
                timestamp = System.currentTimeMillis(),
                metadata = "Activated ${tier.name} ($durationMonths month membership)"
            )
            _coinLedger.value = listOf(tx) + _coinLedger.value

            logAdminAudit(
                action = "VIP_MEMBERSHIP_ACTIVATION",
                targetId = updated.id,
                details = "Activated VIP Level ${tier.level} for $durationMonths months"
            )
            onResult(true, "VIP Level ${tier.level} successfully activated!")
        }
    }

    fun adminAdjustCoins(targetUserId: String, deltaCoins: Long, reason: String) {
        val user = _currentUser.value
        val before = user.coinBalance
        val after = (before + deltaCoins).coerceAtLeast(0L)

        val updated = user.copy(coinBalance = after)
        _currentUser.value = updated

        val tx = CoinLedgerTransaction(
            id = "tx_adj_${UUID.randomUUID()}",
            userId = user.id,
            numericId = user.numericId,
            type = LedgerTransactionType.ADMIN_ADJUSTMENT,
            amount = deltaCoins,
            balanceBefore = before,
            balanceAfter = after,
            timestamp = System.currentTimeMillis(),
            metadata = "Admin adjustment: $reason"
        )
        _coinLedger.value = listOf(tx) + _coinLedger.value

        logAdminAudit(
            action = "ADMIN_BALANCE_ADJUSTMENT",
            targetId = user.id,
            details = "Adjusted %,d coins. Reason: $reason".format(deltaCoins)
        )
    }

    fun toggleCoinPackageActive(packageId: String) {
        _coinPackages.value = _coinPackages.value.map {
            if (it.id == packageId) it.copy(isActive = !it.isActive) else it
        }
    }

    fun findUserByNumericId(numericId: String): UserProfile? {
        val clean = numericId.trim()
        if (_currentUser.value.numericId == clean) return _currentUser.value
        return SampleData.users.find { it.numericId == clean }
    }

    fun awardUserXp(xpDelta: Long, reason: String) {
        val cur = _currentUser.value
        val newXp = cur.userXp + xpDelta
        val newLevel = LevelCalculator.calculateUserLevel(newXp)
        _currentUser.value = cur.copy(userXp = newXp, userLevel = newLevel)
    }

    fun awardCharmPoints(charmDelta: Long) {
        val cur = _currentUser.value
        val newCharm = cur.charmPoints + charmDelta
        val newLevel = LevelCalculator.calculateCharmLevel(newCharm)
        _currentUser.value = cur.copy(charmPoints = newCharm, charmLevel = newLevel)
    }

    private fun logAdminAudit(action: String, targetId: String, details: String) {
        val entry = AdminAuditLogEntry(
            id = "log_${UUID.randomUUID()}",
            action = action,
            adminId = _currentUser.value.id,
            targetId = targetId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        _adminAuditLogs.value = listOf(entry) + _adminAuditLogs.value
    }

    // -------------------------------------------------------------
    // Feed & Post Operations
    // -------------------------------------------------------------
    fun setFeedFilter(filter: FeedFilter) {
        _selectedFeedFilter.value = filter
        refreshFeed()
    }

    private fun refreshFeed() {
        val currentPosts = _rawPosts.value
        val ranked = when (_selectedFeedFilter.value) {
            FeedFilter.FOR_YOU -> recommendationEngine.rankForYou(
                posts = currentPosts,
                currentUser = _currentUser.value,
                followedUserIds = _followedUserIds.value,
                userLikedPostIds = _userLikedPostIds.value,
                userBookmarkedPostIds = _userBookmarkedPostIds.value
            )
            FeedFilter.FOLLOWING -> recommendationEngine.getFollowingFeed(
                posts = currentPosts,
                followedUserIds = _followedUserIds.value
            )
            FeedFilter.TRENDING -> recommendationEngine.getTrendingFeed(currentPosts)
        }
        _displayedFeed.value = ranked
    }

    fun toggleLike(postId: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("like this post")
            return false
        }
        val currentLiked = _userLikedPostIds.value.contains(postId)
        val updatedLikes = if (currentLiked) {
            _userLikedPostIds.value - postId
        } else {
            _userLikedPostIds.value + postId
        }
        _userLikedPostIds.value = updatedLikes

        _rawPosts.value = _rawPosts.value.map { post ->
            if (post.id == postId) {
                val newCount = if (currentLiked) post.likesCount - 1 else post.likesCount + 1
                post.copy(isLiked = !currentLiked, likesCount = newCount.coerceAtLeast(0))
            } else {
                post
            }
        }
        awardUserXp(10, "Liked post")
        refreshFeed()
        return true
    }

    fun toggleBookmark(postId: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("save this post")
            return false
        }
        val current = _userBookmarkedPostIds.value
        val updated = if (current.contains(postId)) current - postId else current + postId
        _userBookmarkedPostIds.value = updated
        _rawPosts.value = _rawPosts.value.map { post ->
            if (post.id == postId) post.copy(isBookmarked = updated.contains(postId)) else post
        }
        refreshFeed()
        return true
    }

    fun addComment(postId: String, text: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("comment on this post")
            return false
        }
        if (text.isBlank()) return false
        val newComment = Comment(
            id = "c_${UUID.randomUUID()}",
            postId = postId,
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            userAvatar = _currentUser.value.avatarUrl,
            text = text.trim(),
            timestampText = "Just now"
        )
        val currentMap = _comments.value.toMutableMap()
        val list = (currentMap[postId] ?: emptyList()).toMutableList()
        list.add(0, newComment)
        currentMap[postId] = list
        _comments.value = currentMap

        _rawPosts.value = _rawPosts.value.map { post ->
            if (post.id == postId) post.copy(commentsCount = post.commentsCount + 1) else post
        }
        awardUserXp(25, "Added comment")
        refreshFeed()
        return true
    }

    fun createPost(
        caption: String,
        imageUrl: String,
        location: String = "",
        tags: List<String> = emptyList()
    ): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("publish a post")
            return false
        }
        val newPost = Post(
            id = "post_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            userAvatar = _currentUser.value.avatarUrl,
            location = location,
            imageUrl = imageUrl,
            caption = caption,
            likesCount = 0,
            commentsCount = 0,
            timestampText = "Just now",
            timestampMillis = System.currentTimeMillis(),
            tags = tags,
            isVerified = _currentUser.value.isVerified
        )
        _rawPosts.value = listOf(newPost) + _rawPosts.value
        _currentUser.value = _currentUser.value.copy(postsCount = _currentUser.value.postsCount + 1)
        awardUserXp(150, "Created new post")
        refreshFeed()

        // Persist post document to Firebase Firestore
        val postMap = hashMapOf<String, Any>(
            "id" to newPost.id,
            "userId" to newPost.userId,
            "username" to newPost.username,
            "userAvatar" to newPost.userAvatar,
            "imageUrl" to newPost.imageUrl,
            "caption" to newPost.caption,
            "location" to newPost.location,
            "likesCount" to 0,
            "commentsCount" to 0,
            "tags" to tags,
            "timestamp" to newPost.timestampMillis
        )
        firestore?.collection("posts")?.document(newPost.id)?.set(postMap)

        // Increment user postsCount in Firestore
        val uid = _currentUser.value.id
        if (uid.isNotBlank() && uid != "user_guest") {
            firestore?.collection("users")?.document(uid)?.set(
                mapOf("postsCount" to _currentUser.value.postsCount),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
        return true
    }

    fun createStory(mediaUrl: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("post a story")
            return false
        }
        val newStory = Story(
            id = "story_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            userAvatar = _currentUser.value.avatarUrl,
            mediaUrl = mediaUrl,
            timestampMillis = System.currentTimeMillis()
        )
        _stories.value = listOf(newStory) + _stories.value
        awardUserXp(80, "Created story")
        return true
    }

    fun toggleFollow(
        userId: String,
        targetUsername: String = "",
        targetAvatar: String = ""
    ): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("follow creators")
            return false
        }
        val currentUserId = _currentUser.value.id
        if (userId == currentUserId || userId.isBlank()) return false

        val currentFollowed = _followedUserIds.value
        val isFollowing = currentFollowed.contains(userId)
        val newFollowed = if (isFollowing) currentFollowed - userId else currentFollowed + userId
        _followedUserIds.value = newFollowed

        // Immediately update local following count on current user profile
        val curUser = _currentUser.value
        val updatedFollowingCount = if (isFollowing) maxOf(0, curUser.followingCount - 1) else curUser.followingCount + 1
        _currentUser.value = curUser.copy(followingCount = updatedFollowingCount)

        // Update suggested creators local follower counts
        _suggestedCreators.value = _suggestedCreators.value.map { creator ->
            if (creator.id == userId) {
                val newCount = if (isFollowing) maxOf(0, creator.followersCount - 1) else creator.followersCount + 1
                creator.copy(followersCount = newCount, isFollowing = !isFollowing)
            } else creator
        }

        refreshFeed()

        // Sync with Firebase Firestore
        val db = firestore
        if (db != null && currentUserId.isNotBlank() && currentUserId != "user_guest") {
            val followDocId = "${currentUserId}_${userId}"
            val followRef = db.collection("follows").document(followDocId)
            val curUserRef = db.collection("users").document(currentUserId)
            val targetUserRef = db.collection("users").document(userId)

            if (!isFollowing) {
                // Follow action
                val followMap = hashMapOf<String, Any>(
                    "id" to followDocId,
                    "followerId" to currentUserId,
                    "followingId" to userId,
                    "followerUsername" to _currentUser.value.username,
                    "followerAvatar" to _currentUser.value.avatarUrl,
                    "targetUsername" to targetUsername,
                    "targetAvatar" to targetAvatar,
                    "timestamp" to System.currentTimeMillis()
                )
                followRef.set(followMap)

                // Subcollections
                curUserRef.collection("following").document(userId)
                    .set(mapOf("userId" to userId, "timestamp" to System.currentTimeMillis()))
                targetUserRef.collection("followers").document(currentUserId)
                    .set(mapOf("userId" to currentUserId, "timestamp" to System.currentTimeMillis()))

                // Atomic count increments in Firestore
                curUserRef.set(
                    mapOf("followingCount" to com.google.firebase.firestore.FieldValue.increment(1)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                targetUserRef.set(
                    mapOf("followersCount" to com.google.firebase.firestore.FieldValue.increment(1)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else {
                // Unfollow action
                followRef.delete()

                // Subcollections
                curUserRef.collection("following").document(userId).delete()
                targetUserRef.collection("followers").document(currentUserId).delete()

                // Atomic count decrements in Firestore
                curUserRef.set(
                    mapOf("followingCount" to com.google.firebase.firestore.FieldValue.increment(-1)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                targetUserRef.set(
                    mapOf("followersCount" to com.google.firebase.firestore.FieldValue.increment(-1)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        }
        return true
    }

    fun updateProfile(
        username: String,
        fullName: String,
        bio: String,
        website: String,
        avatarUrl: String,
        bannerAccentHex: String
    ) {
        _currentUser.value = _currentUser.value.copy(
            username = username,
            fullName = fullName,
            bio = bio,
            website = website,
            avatarUrl = avatarUrl,
            bannerAccentColorHex = bannerAccentHex
        )

        // Persist to Firebase Firestore
        val uid = firebaseAuth.currentUser?.uid ?: _currentUser.value.id
        if (uid.isNotBlank() && uid != "user_guest") {
            val userUpdates = hashMapOf<String, Any>(
                "username" to username,
                "fullName" to fullName,
                "bio" to bio,
                "website" to website,
                "avatarUrl" to avatarUrl,
                "bannerAccentColorHex" to bannerAccentHex,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("users")?.document(uid)?.set(
                userUpdates,
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
    }

    // -------------------------------------------------------------
    // Falla Voice Room Operations & LiveKit WebRTC
    // -------------------------------------------------------------
    fun joinVoiceRoom(room: VoiceRoom, asSpeaker: Boolean = false) {
        _recentlyVisitedRoomIds.value = listOf(room.id) + (_recentlyVisitedRoomIds.value - room.id)
        liveKitManager.connectToRoom(room, asSpeaker = asSpeaker)
    }

    fun leaveVoiceRoom() {
        liveKitManager.disconnect()
    }

    val coinBalance: StateFlow<Long> = MutableStateFlow(_currentUser.value.coinBalance).apply {
        scope.launch {
            _currentUser.collect { value = it.coinBalance }
        }
    }

    fun topUpCoins(amount: Long) {
        val before = _currentUser.value.coinBalance
        val after = before + amount
        _currentUser.value = _currentUser.value.copy(coinBalance = after)
        val tx = CoinLedgerTransaction(
            id = "tx_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            numericId = _currentUser.value.numericId,
            type = LedgerTransactionType.PROMOTIONAL_BONUS,
            amount = amount,
            balanceBefore = before,
            balanceAfter = after,
            timestamp = System.currentTimeMillis(),
            metadata = "Wallet top up: %,d coins".format(amount)
        )
        _coinLedger.value = listOf(tx) + _coinLedger.value
    }

    fun claimSpeakerSlot(slotIndex: Int, context: android.content.Context? = null) {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("claim a voice stage seat")
            return
        }
        liveKitManager.claimSpeakerSlot(slotIndex, context)
    }

    fun releaseSpeakerSlot() {
        liveKitManager.releaseSpeakerSlot()
    }

    fun toggleFavoriteRoom(roomId: String) {
        val current = _favoriteRoomIds.value
        _favoriteRoomIds.value = if (current.contains(roomId)) current - roomId else current + roomId
    }

    fun updateRoomSettings(roomId: String, newSettings: RoomSettings) {
        _voiceRooms.value = _voiceRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(
                    title = newSettings.name,
                    totalSlots = newSettings.seatCount,
                    category = newSettings.category,
                    isPasswordProtected = newSettings.isPrivate && newSettings.password.isNotBlank(),
                    settings = newSettings
                )
            } else {
                room
            }
        }
        if (liveKitManager.currentRoom.value?.id == roomId) {
            val updated = _voiceRooms.value.find { it.id == roomId }
            if (updated != null) {
                liveKitManager.connectToRoom(updated, asSpeaker = liveKitManager.currentRoom.value?.speakers?.any { it.id == _currentUser.value.id } == true)
            }
        }
    }

    fun deleteVoiceRoom(roomId: String) {
        if (liveKitManager.currentRoom.value?.id == roomId) {
            leaveVoiceRoom()
        }
        _voiceRooms.value = _voiceRooms.value.filter { it.id != roomId }
    }

    fun muteRoomParticipant(roomId: String, participantId: String, participantName: String, mute: Boolean) {
        if (mute) {
            liveKitManager.muteSpeakerByMod(participantId)
        } else {
            liveKitManager.unmuteSpeakerByMod(participantId)
        }
        _voiceRooms.value = _voiceRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(
                    speakers = room.speakers.map { sp ->
                        if (sp.id == participantId) sp.copy(isMuted = mute, isTalking = if (mute) false else sp.isTalking, audioLevel = if (mute) 0f else sp.audioLevel) else sp
                    }
                )
            } else room
        }
        logModerationAction(
            ModerationAction(
                id = "mod_${UUID.randomUUID()}",
                moderatorName = _currentUser.value.fullName,
                targetUserName = participantName,
                actionType = if (mute) "MUTE_USER" else "UNMUTE_USER"
            )
        )
        sendInRoomChatMessage(
            roomId,
            if (mute) "🛡️ Moderator muted @$participantName" else "🛡️ Moderator unmuted @$participantName"
        )
    }

    fun muteAllRoomParticipants(roomId: String) {
        liveKitManager.muteAllSpeakers()
        _voiceRooms.value = _voiceRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(
                    speakers = room.speakers.map { sp ->
                        sp.copy(isMuted = true, isTalking = false, audioLevel = 0f)
                    }
                )
            } else room
        }
        logModerationAction(
            ModerationAction(
                id = "mod_${UUID.randomUUID()}",
                moderatorName = _currentUser.value.fullName,
                targetUserName = "All Stage Speakers",
                actionType = "MUTE_ALL"
            )
        )
        sendInRoomChatMessage(roomId, "🛡️ Room owner muted all stage microphones")
    }

    fun endVoiceRoomBroadcast(roomId: String) {
        logModerationAction(
            ModerationAction(
                id = "mod_${UUID.randomUUID()}",
                moderatorName = _currentUser.value.fullName,
                targetUserName = "Broadcast Session ($roomId)",
                actionType = "END_BROADCAST"
            )
        )
        sendInRoomChatMessage(roomId, "🛑 Room owner ended the broadcast session.")
        liveKitManager.endBroadcastSession()
        _voiceRooms.value = _voiceRooms.value.filter { it.id != roomId }
    }

    fun logModerationAction(action: ModerationAction) {
        _moderationAuditLog.value = listOf(action) + _moderationAuditLog.value
    }

    // In-Room Games logic
    fun rollDice(roomId: String, betCoins: Long): Pair<Int, Int> {
        val user = _currentUser.value
        if (user.coinBalance >= betCoins) {
            _currentUser.value = user.copy(coinBalance = user.coinBalance - betCoins)
        }
        val d1 = (1..6).random()
        val d2 = (1..6).random()
        _diceGameResult.value = d1 to d2

        if (d1 == d2 || (d1 + d2) >= 9) {
            val winAmount = betCoins * 2
            _currentUser.value = _currentUser.value.copy(coinBalance = _currentUser.value.coinBalance + winAmount)
            sendInRoomChatMessage(roomId, "🎲 Rolled [$d1, $d2] and won $winAmount coins!")
        } else {
            sendInRoomChatMessage(roomId, "🎲 Rolled [$d1, $d2] in Lucky Dice")
        }
        return d1 to d2
    }

    fun playTicTacToeMove(index: Int) {
        val board = _ticTacToeBoard.value.toMutableList()
        if (index in 0..8 && board[index].isEmpty()) {
            val countX = board.count { it == "X" }
            val countO = board.count { it == "O" }
            board[index] = if (countX <= countO) "X" else "O"
            _ticTacToeBoard.value = board
        }
    }

    fun resetTicTacToe() {
        _ticTacToeBoard.value = List(9) { "" }
    }

    fun nextTrivia() {
        _triviaIndex.value = (_triviaIndex.value + 1) % 4
    }

    fun sendFallaGift(
        roomId: String,
        gift: FallaGift,
        receiverId: String,
        receiverName: String,
        quantity: Int = 1
    ): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send gifts in voice rooms")
            return false
        }
        val totalCost = gift.coinCost.toLong() * quantity
        if (_currentUser.value.coinBalance < totalCost) {
            return false // Insufficient funds
        }

        val before = _currentUser.value.coinBalance
        val after = before - totalCost
        val newWealth = _currentUser.value.wealthPoints + totalCost
        val newWealthLevel = LevelCalculator.calculateWealthLevel(newWealth)

        _currentUser.value = _currentUser.value.copy(
            coinBalance = after,
            wealthPoints = newWealth,
            wealthLevel = newWealthLevel
        )

        // Increment recipient charm points
        awardCharmPoints(totalCost)

        val tx = CoinLedgerTransaction(
            id = "tx_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            numericId = _currentUser.value.numericId,
            type = LedgerTransactionType.GIFT_SENT,
            amount = -totalCost,
            balanceBefore = before,
            balanceAfter = after,
            timestamp = System.currentTimeMillis(),
            metadata = "Sent ${gift.name} ${gift.icon} x$quantity to $receiverName"
        )
        _coinLedger.value = listOf(tx) + _coinLedger.value

        val transaction = GiftTransaction(
            id = "tx_${UUID.randomUUID()}",
            roomId = roomId,
            senderId = _currentUser.value.id,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            receiverId = receiverId,
            receiverName = receiverName,
            gift = gift,
            quantity = quantity,
            totalCoins = totalCost.toInt(),
            timestampText = "Just now",
            timestampMillis = System.currentTimeMillis()
        )

        _giftTransactions.value = listOf(transaction) + _giftTransactions.value
        _latestRoomGift.value = transaction

        val chatMsg = InRoomChatMessage(
            id = "sys_${UUID.randomUUID()}",
            roomId = roomId,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            vipTier = VipTier.GOLD,
            userLevel = _currentUser.value.userLevel,
            text = "sent ${gift.name} ${gift.icon} x$quantity to $receiverName",
            isSystem = true,
            isGiftAnnouncement = true
        )
        addInRoomMessage(roomId, chatMsg)

        scope.launch {
            delay(3500)
            if (_latestRoomGift.value?.id == transaction.id) {
                _latestRoomGift.value = null
            }
        }
        return true
    }

    fun sendInRoomChatMessage(roomId: String, text: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("chat in voice rooms")
            return false
        }
        if (text.isBlank()) return false
        val msg = InRoomChatMessage(
            id = "chat_${UUID.randomUUID()}",
            roomId = roomId,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            vipTier = VipTier.GOLD,
            userLevel = _currentUser.value.userLevel,
            text = text.trim()
        )
        addInRoomMessage(roomId, msg)
        awardUserXp(15, "In-room chat")
        return true
    }

    private fun addInRoomMessage(roomId: String, msg: InRoomChatMessage) {
        val current = _inRoomChatMessages.value.toMutableMap()
        val list = (current[roomId] ?: emptyList()).toMutableList()
        list.add(msg)
        current[roomId] = list
        _inRoomChatMessages.value = current
    }

    fun createVoiceRoom(
        title: String,
        category: RoomCategory = RoomCategory.TRENDING,
        seatCount: Int = 8,
        isPrivate: Boolean = false,
        password: String = ""
    ): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("create a voice room")
            return false
        }
        val randomNumId = (100000 + (Math.random() * 900000).toInt()).toString()
        val newRoom = VoiceRoom(
            id = "room_${UUID.randomUUID()}",
            numericRoomId = randomNumId,
            title = title,
            topicTag = category.displayName,
            category = category,
            hostId = _currentUser.value.id,
            hostName = _currentUser.value.fullName,
            hostAvatar = _currentUser.value.avatarUrl,
            isLive = true,
            isVip = category == RoomCategory.VIP,
            isPasswordProtected = isPrivate && password.isNotBlank(),
            createdAtText = "Started just now",
            speakers = listOf(
                Participant(
                    id = _currentUser.value.id,
                    name = _currentUser.value.fullName,
                    username = _currentUser.value.username,
                    avatarUrl = _currentUser.value.avatarUrl,
                    role = ParticipantRole.OWNER,
                    vipTier = VipTier.GOLD,
                    userLevel = _currentUser.value.userLevel,
                    wealthLevel = _currentUser.value.wealthLevel,
                    charmLevel = _currentUser.value.charmLevel,
                    isMuted = false,
                    isTalking = true,
                    audioLevel = 0.5f,
                    slotIndex = 0
                )
            ),
            listeners = emptyList(),
            totalSlots = seatCount,
            charmPoints = 0,
            settings = RoomSettings(
                name = title,
                seatCount = seatCount,
                category = category,
                isPrivate = isPrivate,
                password = password
            )
        )
        _voiceRooms.value = listOf(newRoom) + _voiceRooms.value
        awardUserXp(120, "Hosted voice room")
        joinVoiceRoom(newRoom, asSpeaker = true)
        return true
    }

    fun toggleFollowBack(notificationId: String) {
        val list = _notifications.value.map {
            if (it.id == notificationId) it.copy(isFollowedBack = !it.isFollowedBack) else it
        }
        _notifications.value = list
    }

    fun setMyOnlinePresence(state: OnlinePresenceState) {
        _myOnlinePresence.value = state
    }

    // Direct Messages
    fun startOrGetConversation(user: UserProfile): Conversation {
        val existing = _conversations.value.find { it.participantId == user.id }
        if (existing != null) return existing

        val newConv = Conversation(
            id = "conv_${user.id}",
            participantId = user.id,
            participantName = user.fullName,
            participantUsername = user.username,
            participantAvatar = user.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80" },
            isOnline = true,
            presenceState = OnlinePresenceState.ONLINE,
            lastActiveText = "Active now",
            lastMessageText = "Started a conversation",
            lastMessageTimestamp = "Just now",
            unreadCount = 0
        )
        _conversations.value = listOf(newConv) + _conversations.value
        return newConv
    }

    fun markConversationAsRead(conversationId: String) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) conv.copy(unreadCount = 0) else conv
        }
    }

    fun sendDirectMessage(recipientId: String, text: String): Boolean {
        return sendTextMessage(recipientId, text)
    }

    fun sendTextMessage(conversationId: String, text: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send direct messages")
            return false
        }
        if (text.isBlank()) return false
        val newMsg = ChatMessage(
            id = "m_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = _currentUser.value.id,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            text = text.trim(),
            timestampText = "Just now",
            timestampMillis = System.currentTimeMillis(),
            isMine = true,
            isRead = true,
            messageType = MessageType.TEXT
        )
        val current = _chatMessages.value.toMutableMap()
        val thread = (current[conversationId] ?: emptyList()).toMutableList()
        thread.add(newMsg)
        current[conversationId] = thread
        _chatMessages.value = current

        // Update conversation summary and bring to top
        updateConversationSummary(conversationId, text.trim())
        awardUserXp(15, "Direct message sent")
        return true
    }

    fun sendVoiceNote(
        conversationId: String,
        durationSec: Int,
        amplitudes: List<Float>,
        audioFilePath: String? = null
    ): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send voice messages")
            return false
        }
        val durationDisplay = "0:${durationSec.toString().padStart(2, '0')}"
        val newMsg = ChatMessage(
            id = "m_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = _currentUser.value.id,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            text = "Voice message",
            timestampText = "Just now",
            timestampMillis = System.currentTimeMillis(),
            isMine = true,
            isRead = true,
            messageType = MessageType.VOICE_NOTE,
            voiceDurationSec = durationSec,
            voiceAmplitudes = amplitudes,
            audioFilePath = audioFilePath
        )
        val current = _chatMessages.value.toMutableMap()
        val thread = (current[conversationId] ?: emptyList()).toMutableList()
        thread.add(newMsg)
        current[conversationId] = thread
        _chatMessages.value = current

        updateConversationSummary(conversationId, "🎤 Voice message ($durationDisplay)")
        awardUserXp(20, "Voice note sent")
        return true
    }

    fun sendPhotoMessage(conversationId: String, photoUrl: String, caption: String = ""): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send photos")
            return false
        }
        val newMsg = ChatMessage(
            id = "m_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = _currentUser.value.id,
            senderName = _currentUser.value.fullName,
            senderAvatar = _currentUser.value.avatarUrl,
            text = caption.ifEmpty { "Sent a photo" },
            photoUrl = photoUrl,
            timestampText = "Just now",
            timestampMillis = System.currentTimeMillis(),
            isMine = true,
            isRead = true,
            messageType = MessageType.PHOTO
        )
        val current = _chatMessages.value.toMutableMap()
        val thread = (current[conversationId] ?: emptyList()).toMutableList()
        thread.add(newMsg)
        current[conversationId] = thread
        _chatMessages.value = current

        updateConversationSummary(conversationId, "📸 Photo")
        awardUserXp(15, "Photo sent")
        return true
    }

    private fun updateConversationSummary(conversationId: String, lastText: String) {
        val convList = _conversations.value.toMutableList()
        val idx = convList.indexOfFirst { it.id == conversationId }
        if (idx >= 0) {
            val updated = convList.removeAt(idx).copy(
                lastMessageText = lastText,
                lastMessageTimestamp = "Just now",
                unreadCount = 0
            )
            convList.add(0, updated)
            _conversations.value = convList
        }
    }

    fun reactToMessage(conversationId: String, messageId: String, emoji: String) {
        val current = _chatMessages.value.toMutableMap()
        val thread = (current[conversationId] ?: emptyList()).map { msg ->
            if (msg.id == messageId) {
                msg.copy(reactionEmoji = if (msg.reactionEmoji == emoji) null else emoji)
            } else msg
        }
        current[conversationId] = thread
        _chatMessages.value = current
    }

    // Live Video Streams
    fun openLiveStream(stream: LiveStream) {
        _activeLiveStream.value = stream
    }

    fun closeLiveStream() {
        _activeLiveStream.value = null
    }

    fun sendLiveComment(text: String) {
        val stream = _activeLiveStream.value ?: return
        val newComment = LiveComment(
            id = "lc_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            avatarUrl = _currentUser.value.avatarUrl,
            text = text,
            isHost = false,
            timestampText = "Now"
        )
        _activeLiveStream.value = stream.copy(
            comments = stream.comments + newComment
        )
    }

    fun sendLiveHeart() {
        val stream = _activeLiveStream.value ?: return
        _activeLiveStream.value = stream.copy(
            totalLikes = stream.totalLikes + 1
        )
    }

    fun sendStreamGift(giftType: GiftType) {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send live stream gifts")
            return
        }
        val gift = StreamGift(
            id = "sg_${UUID.randomUUID()}",
            type = giftType,
            senderName = _currentUser.value.fullName
        )
        _streamGifts.value = _streamGifts.value + gift
    }

    fun sendStreamGift(gift: StreamGift) {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send live stream gifts")
            return
        }
        _streamGifts.value = _streamGifts.value + gift
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: android.content.Context? = null): AppRepository {
            return instance ?: synchronized(this) {
                val db = try {
                    val dbIdRes = context?.resources?.getIdentifier("firestore_database_id", "string", context.packageName) ?: 0
                    val dbId = if (dbIdRes != 0) context?.getString(dbIdRes) else null
                    if (!dbId.isNullOrBlank() && dbId != "(default)") {
                        FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), dbId)
                    } else {
                        FirebaseFirestore.getInstance()
                    }
                } catch (e: Exception) {
                    try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
                }
                instance ?: AppRepository(firestoreDb = db).also { instance = it }
            }
        }
    }
}

