package com.example.model

/**
 * Core User Profile incorporating permanent numeric ID,
 * Firebase UID identity, virtual economy wallet, VIP status,
 * and 3-pillar level progression (User XP, Wealth, Charm).
 */
data class UserProfile(
    val id: String, // Firebase UID
    val numericId: String = "8294102", // Permanent 7-digit unique public User ID
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val bio: String,
    val website: String = "",
    val pronouns: String = "",
    val category: String = "Digital Creator",
    val isVerified: Boolean = false,
    val followersCount: Int = 1240,
    val followingCount: Int = 380,
    val postsCount: Int = 24,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val bannerAccentColorHex: String = "#E1306C",
    val isPrivate: Boolean = false,
    // Virtual Economy
    val coinBalance: Long = 12500L,
    // Wealth Level System (Coin spending & verified purchases)
    val wealthPoints: Long = 25000L,
    val wealthLevel: Int = 4,
    // Charm Level System (Gifts received as host/participant)
    val charmPoints: Long = 18500L,
    val charmLevel: Int = 3,
    // User XP & Level System (Activity, logins, chatting)
    val userXp: Long = 3450L,
    val userLevel: Int = 5,
    // VIP Membership Tier (0 = None, 1..10 = VIP 1..VIP 10)
    val vipTierLevel: Int = 2,
    val vipExpiryTimestamp: Long? = System.currentTimeMillis() + (28L * 24 * 3600 * 1000),
    val isAdmin: Boolean = false,
    val isSuperAdmin: Boolean = false,
    val isAuthenticated: Boolean = true
) {
    val isVipActive: Boolean
        get() = vipTierLevel > 0 && (vipExpiryTimestamp == null || vipExpiryTimestamp > System.currentTimeMillis())

    val vipTierConfig: VipTierConfig?
        get() = if (isVipActive) VipTierConfig.getTier(vipTierLevel) else null
}

enum class ProfileAccentColor(val label: String, val hex: String) {
    INSTAGRAM("Sunset Wave", "#E1306C"),
    NEON_PURPLE("Neon Purple", "#833AB4"),
    CYAN("Cyber Cyan", "#00B4D8"),
    AMBER("Golden Amber", "#F77737"),
    EMERALD("Mint Emerald", "#2EC4B6")
}

/**
 * 10-Tier VIP Membership definition with distinct perks, frames, and badge styling.
 */
data class VipTierConfig(
    val level: Int,
    val name: String,
    val badgeColorHex: String,
    val badgeIcon: String,
    val coinPriceMonthly: Long,
    val coinPriceQuarterly: Long,
    val coinPriceAnnual: Long,
    val benefits: List<String>,
    val frameBorderHex: String,
    val entranceEffect: String,
    val bonusCoinPercent: Int
) {
    companion object {
        val ALL_TIERS = listOf(
            VipTierConfig(
                level = 1,
                name = "VIP 1 Bronze Pioneer",
                badgeColorHex = "#CD7F32",
                badgeIcon = "🥉",
                coinPriceMonthly = 999L,
                coinPriceQuarterly = 2699L,
                coinPriceAnnual = 9999L,
                benefits = listOf("Bronze avatar badge", "5% coin purchase bonus", "Custom in-room chat bubble"),
                frameBorderHex = "#CD7F32",
                entranceEffect = "Gentle Bronze Sparkles",
                bonusCoinPercent = 5
            ),
            VipTierConfig(
                level = 2,
                name = "VIP 2 Silver Knight",
                badgeColorHex = "#C0C0C0",
                badgeIcon = "🥈",
                coinPriceMonthly = 2499L,
                coinPriceQuarterly = 6799L,
                coinPriceAnnual = 24999L,
                benefits = listOf("Silver metallic frame", "Exclusive in-room emote pack", "8% coin purchase bonus"),
                frameBorderHex = "#E0E0E0",
                entranceEffect = "Silver Halo Flash",
                bonusCoinPercent = 8
            ),
            VipTierConfig(
                level = 3,
                name = "VIP 3 Gold Noble",
                badgeColorHex = "#FFD700",
                badgeIcon = "🥇",
                coinPriceMonthly = 4999L,
                coinPriceQuarterly = 13499L,
                coinPriceAnnual = 49999L,
                benefits = listOf("Golden crown badge", "Stage seat priority", "10% coin purchase bonus", "Golden entrance banner"),
                frameBorderHex = "#FFD700",
                entranceEffect = "Golden Dust Explosion",
                bonusCoinPercent = 10
            ),
            VipTierConfig(
                level = 4,
                name = "VIP 4 Platinum Baron",
                badgeColorHex = "#E5E4E2",
                badgeIcon = "💎",
                coinPriceMonthly = 9999L,
                coinPriceQuarterly = 26999L,
                coinPriceAnnual = 99999L,
                benefits = listOf("Platinum animated avatar ring", "12% coin bonus", "Special gift unlocked"),
                frameBorderHex = "#E5E4E2",
                entranceEffect = "Crystal Ice Shards",
                bonusCoinPercent = 12
            ),
            VipTierConfig(
                level = 5,
                name = "VIP 5 Emerald Lord",
                badgeColorHex = "#50C878",
                badgeIcon = "❇️",
                coinPriceMonthly = 19999L,
                coinPriceQuarterly = 53999L,
                coinPriceAnnual = 199999L,
                benefits = listOf("Emerald glowing border", "15% coin purchase bonus", "Free room pin boost daily"),
                frameBorderHex = "#50C878",
                entranceEffect = "Emerald Lightning Burst",
                bonusCoinPercent = 15
            ),
            VipTierConfig(
                level = 6,
                name = "VIP 6 Sapphire King",
                badgeColorHex = "#0F52BA",
                badgeIcon = "🔷",
                coinPriceMonthly = 34999L,
                coinPriceQuarterly = 94499L,
                coinPriceAnnual = 349999L,
                benefits = listOf("Sapphire royal title", "Blue flame entrance animation", "18% coin bonus"),
                frameBorderHex = "#0F52BA",
                entranceEffect = "Deep Ocean Blue Flames",
                bonusCoinPercent = 18
            ),
            VipTierConfig(
                level = 7,
                name = "VIP 7 Ruby Emperor",
                badgeColorHex = "#9B111E",
                badgeIcon = "🔴",
                coinPriceMonthly = 59999L,
                coinPriceQuarterly = 161999L,
                coinPriceAnnual = 599999L,
                benefits = listOf("Kick immunity from non-host mods", "Red thunder entrance", "20% coin bonus"),
                frameBorderHex = "#9B111E",
                entranceEffect = "Crimson Lightning Blast",
                bonusCoinPercent = 20
            ),
            VipTierConfig(
                level = 8,
                name = "VIP 8 Diamond Sovereign",
                badgeColorHex = "#00F0FF",
                badgeIcon = "👑",
                coinPriceMonthly = 99999L,
                coinPriceQuarterly = 269999L,
                coinPriceAnnual = 999999L,
                benefits = listOf("Diamond laser avatar ring", "Customizable room background", "25% coin bonus"),
                frameBorderHex = "#00F0FF",
                entranceEffect = "Prismatic Diamond Shimmer",
                bonusCoinPercent = 25
            ),
            VipTierConfig(
                level = 9,
                name = "VIP 9 Obsidian Warlord",
                badgeColorHex = "#3B3C36",
                badgeIcon = "🌌",
                coinPriceMonthly = 199999L,
                coinPriceQuarterly = 539999L,
                coinPriceAnnual = 1999999L,
                benefits = listOf("Dark Nebula room entrance", "Super admin direct concierge line", "30% coin bonus"),
                frameBorderHex = "#8A2BE2",
                entranceEffect = "Cosmic Black Hole Portal",
                bonusCoinPercent = 30
            ),
            VipTierConfig(
                level = 10,
                name = "VIP 10 Celestial Deity",
                badgeColorHex = "#FF007F",
                badgeIcon = "🌟",
                coinPriceMonthly = 499999L,
                coinPriceQuarterly = 1349999L,
                coinPriceAnnual = 4999999L,
                benefits = listOf("Golden Dragon full-screen arrival", "Global chat announcement", "Permanent 40% bonus", "All supreme gifts"),
                frameBorderHex = "#FFD700",
                entranceEffect = "Golden Celestial Dragon Roar",
                bonusCoinPercent = 40
            )
        )

        fun getTier(level: Int): VipTierConfig? = ALL_TIERS.find { it.level == level }
    }
}

/**
 * Configurable Coin Packages for the in-app Coin Purchase Center.
 */
data class CoinPackage(
    val id: String,
    val name: String,
    val coins: Long,
    val bonusCoins: Long = 0L,
    val priceUsd: Double,
    val badgeLabel: String? = null,
    val isPopular: Boolean = false,
    val isRecommended: Boolean = false,
    val isActive: Boolean = true
) {
    val totalCoins: Long get() = coins + bonusCoins
}

/**
 * Immutable Coin Wallet Ledger Transaction.
 */
enum class LedgerTransactionType(val label: String, val icon: String) {
    COIN_PURCHASE("Coin Purchase", "💳"),
    PROMOTIONAL_BONUS("Promotional Bonus", "🎁"),
    GIFT_SENT("Gift Sent", "🚀"),
    GIFT_RECEIVED("Gift Received", "💎"),
    VIP_PURCHASE("VIP Membership", "👑"),
    ADMIN_ADJUSTMENT("Admin Adjustment", "⚙️"),
    GAME_REWARD("Game Reward", "🎲")
}

data class CoinLedgerTransaction(
    val id: String,
    val userId: String,
    val numericId: String,
    val type: LedgerTransactionType,
    val amount: Long, // positive for credit, negative for debit
    val balanceBefore: Long,
    val balanceAfter: Long,
    val timestamp: Long,
    val metadata: String
)

/**
 * Level & Progression Mathematical Calculator.
 */
object LevelCalculator {
    // User XP Progression: 100 * level required per level
    fun calculateUserLevel(xp: Long): Int {
        var level = 1
        var requiredXp = 500L
        var accumulated = 0L
        while (level < 100) {
            if (xp < accumulated + requiredXp) break
            accumulated += requiredXp
            level++
            requiredXp = (level * 600L)
        }
        return level
    }

    fun xpForNextLevel(currentLevel: Int): Long {
        return (currentLevel + 1) * 600L
    }

    // Wealth Level Progression (Coins spent/purchased): Levels 1 to 50
    fun calculateWealthLevel(points: Long): Int {
        var level = 1
        var threshold = 1000L
        while (level < 50 && points >= threshold) {
            level++
            threshold = (threshold * 1.5).toLong()
        }
        return level
    }

    fun wealthPointsForNextLevel(currentLevel: Int): Long {
        var threshold = 1000L
        for (i in 1..currentLevel) {
            threshold = (threshold * 1.5).toLong()
        }
        return threshold
    }

    // Charm Level Progression (Gifts received value): Levels 1 to 50
    fun calculateCharmLevel(points: Long): Int {
        var level = 1
        var threshold = 800L
        while (level < 50 && points >= threshold) {
            level++
            threshold = (threshold * 1.45).toLong()
        }
        return level
    }

    fun charmPointsForNextLevel(currentLevel: Int): Long {
        var threshold = 800L
        for (i in 1..currentLevel) {
            threshold = (threshold * 1.45).toLong()
        }
        return threshold
    }
}
