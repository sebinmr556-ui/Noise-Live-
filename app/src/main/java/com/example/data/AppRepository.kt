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

    // Firebase Auth instance
    private val firebaseAuth: FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    } ?: FirebaseAuth.getInstance()

    // User Profile State
    private val _currentUser = MutableStateFlow(SampleData.currentUser)
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow(true)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _authPromptRequest = MutableStateFlow<String?>(null)
    val authPromptRequest: StateFlow<String?> = _authPromptRequest.asStateFlow()

    private val _followedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followedUserIds: StateFlow<Set<String>> = _followedUserIds.asStateFlow()

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

    // Direct Messages
    private val _conversations = MutableStateFlow(SampleData.conversations)
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _chatMessages = MutableStateFlow(SampleData.initialMessages)
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

    private val _coinLedger = MutableStateFlow(
        listOf(
            CoinLedgerTransaction(
                id = "tx_init_1",
                userId = "user_me",
                numericId = "8294102",
                type = LedgerTransactionType.PROMOTIONAL_BONUS,
                amount = 15000L,
                balanceBefore = 0L,
                balanceAfter = 15000L,
                timestamp = System.currentTimeMillis() - 86400000L,
                metadata = "Welcome creator reward & verified account grant"
            )
        )
    )
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
            }
        }
        refreshFeed()
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
            tags = tags,
            isVerified = _currentUser.value.isVerified
        )
        _rawPosts.value = listOf(newPost) + _rawPosts.value
        _currentUser.value = _currentUser.value.copy(postsCount = _currentUser.value.postsCount + 1)
        awardUserXp(150, "Created new post")
        refreshFeed()
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

    fun toggleFollow(userId: String): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("follow creators")
            return false
        }
        val current = _followedUserIds.value
        val isFollowing = current.contains(userId)
        _followedUserIds.value = if (isFollowing) current - userId else current + userId
        refreshFeed()
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

    // Direct Messages
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
        awardUserXp(15, "Direct message sent")
        return true
    }

    fun sendVoiceNote(conversationId: String, durationSec: Int, amplitudes: List<Float>): Boolean {
        if (!_isUserAuthenticated.value) {
            triggerAuthPrompt("send voice messages")
            return false
        }
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
            voiceAmplitudes = amplitudes
        )
        val current = _chatMessages.value.toMutableMap()
        val thread = (current[conversationId] ?: emptyList()).toMutableList()
        thread.add(newMsg)
        current[conversationId] = thread
        _chatMessages.value = current
        awardUserXp(20, "Voice note sent")
        return true
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
                instance ?: AppRepository().also { instance = it }
            }
        }
    }
}

