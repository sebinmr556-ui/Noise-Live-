package com.example.model

/**
 * VIP Tiers for NOICE LIVE luxury experience.
 */
enum class VipTier(val tierNumber: Int, val title: String, val tierLevel: Int = tierNumber) {
    MEMBER(1, "Bronze VIP", 1),
    SILVER(2, "Silver VIP", 2),
    GOLD(3, "Gold VIP", 3)
}
