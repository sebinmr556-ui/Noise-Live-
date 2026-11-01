package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R

/**
 * Resolves a VIP level (1..15) or badgeImageUrl string to the corresponding
 * 3D luxury VIP badge drawable resource (vip_badge_1 .. vip_badge_15).
 */
@DrawableRes
fun getVipBadgeDrawableRes(vipLevel: Int, badgeImageUrl: String = ""): Int {
    val parsedFromUrl = Regex("(\\d+)").find(badgeImageUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val effectiveLevel = (parsedFromUrl ?: vipLevel).coerceIn(1, 15)
    return when (effectiveLevel) {
        1 -> R.drawable.vip_badge_1
        2 -> R.drawable.vip_badge_2
        3 -> R.drawable.vip_badge_3
        4 -> R.drawable.vip_badge_4
        5 -> R.drawable.vip_badge_5
        6 -> R.drawable.vip_badge_6
        7 -> R.drawable.vip_badge_7
        8 -> R.drawable.vip_badge_8
        9 -> R.drawable.vip_badge_9
        10 -> R.drawable.vip_badge_10
        11 -> R.drawable.vip_badge_11
        12 -> R.drawable.vip_badge_12
        13 -> R.drawable.vip_badge_13
        14 -> R.drawable.vip_badge_14
        else -> R.drawable.vip_badge_15
    }
}

/**
 * Standalone 3D VIP Badge (VIP 1 to VIP 15) rendered with ContentScale.Fit (BoxFit.contain).
 */
@Composable
fun VipCrestBadgeImage(
    vipLevel: Int,
    badgeImageUrl: String = "",
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val isRemoteUrl = badgeImageUrl.startsWith("http://") || badgeImageUrl.startsWith("https://")
    if (isRemoteUrl) {
        AsyncImage(
            model = badgeImageUrl,
            contentDescription = "VIP $vipLevel Badge",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    } else {
        val drawableRes = getVipBadgeDrawableRes(vipLevel = vipLevel, badgeImageUrl = badgeImageUrl)
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "VIP ${vipLevel.coerceIn(1, 15)} Badge",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    }
}

/**
 * Color sets for various VIP levels (Bronze, Silver, Gold, Platinum, Celestial)
 * to enhance the visual distinction of the compact VIP badge.
 */
data class VipBadgeColors(
    val tierName: String,
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val accentGlowColor: Color = borderColor
)

/**
 * Returns distinct border, background, and text colors for various VIP tiers:
 * - Bronze (VIP 1 - 3): Warm copper/bronze tones (#231610, #CD7F32, #FED7AA)
 * - Silver (VIP 4 - 7): Polished chrome/silver tones (#131924, #94A3B8, #F1F5F9)
 * - Gold (VIP 8 - 10): Radiant rich gold tones (#261A07, #EAB308, #FEF08A)
 * - Platinum (VIP 11 - 13): Diamond ice-blue & royal tones (#160F2C, #38BDF8, #E0F2FE)
 * - Celestial (VIP 14 - 15): Prismatic royal crimson/gold (#1D092A, #F43F5E, #FFFBEB)
 */
fun getVipBadgeColors(vipLevel: Int): VipBadgeColors {
    val level = vipLevel.coerceIn(1, 15)
    return when (level) {
        in 1..3 -> VipBadgeColors(
            tierName = "Bronze",
            backgroundColor = Color(0xFF231610),
            borderColor = Color(0xFFCD7F32),
            textColor = Color(0xFFFED7AA),
            accentGlowColor = Color(0xFFD97706)
        )
        in 4..7 -> VipBadgeColors(
            tierName = "Silver",
            backgroundColor = Color(0xFF131924),
            borderColor = Color(0xFF94A3B8),
            textColor = Color(0xFFF1F5F9),
            accentGlowColor = Color(0xFFCBD5E1)
        )
        in 8..10 -> VipBadgeColors(
            tierName = "Gold",
            backgroundColor = Color(0xFF261A07),
            borderColor = Color(0xFFEAB308),
            textColor = Color(0xFFFEF08A),
            accentGlowColor = Color(0xFFF59E0B)
        )
        in 11..13 -> VipBadgeColors(
            tierName = "Platinum",
            backgroundColor = Color(0xFF160F2C),
            borderColor = Color(0xFF38BDF8),
            textColor = Color(0xFFE0F2FE),
            accentGlowColor = Color(0xFFA855F7)
        )
        else -> VipBadgeColors(
            tierName = "Celestial",
            backgroundColor = Color(0xFF1D092A),
            borderColor = Color(0xFFF43F5E),
            textColor = Color(0xFFFFFBEB),
            accentGlowColor = Color(0xFFEAB308)
        )
    }
}

/**
 * Compact standalone VIP pill/chip with tier-based dark luxury background,
 * distinct border color, badge icon, and single-line VIP level text.
 */
@Composable
fun CompactVipBadgePill(
    vipLevel: Int,
    badgeImageUrl: String = "",
    height: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val displayLevel = vipLevel.coerceIn(1, 15)
    val colors = getVipBadgeColors(displayLevel)
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.backgroundColor,
        border = BorderStroke(1.dp, colors.borderColor.copy(alpha = 0.9f)),
        modifier = modifier
            .height(height)
            .wrapContentWidth(unbounded = true)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            VipCrestBadgeImage(
                vipLevel = displayLevel,
                badgeImageUrl = badgeImageUrl,
                size = (height - 6.dp).coerceIn(11.dp, 15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "VIP $displayLevel",
                color = colors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Clean circular user avatar with optional compact VIP badge positioned neatly
 * at the bottom edge of the avatar circle (bottom: -4.dp to -6.dp) without any
 * oversized orange gradient circle container.
 */
@Composable
fun UserProfileBadge(
    profileImageUrl: String,
    badgeImageUrl: String = "",
    vipLevel: Int = 1,
    avatarRadius: Dp = 45.dp,
    badgeSize: Dp = 22.dp,
    isBadgeRightAligned: Boolean = true,
    showAnimatedVipRing: Boolean = false,
    isSpeaking: Boolean = false,
    audioLevel: Float = 0f,
    showOverlayBadge: Boolean = true,
    showBottomPillBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val avatarDiameter = avatarRadius * 2
    val activelySpeaking = isSpeaking && audioLevel > 0.05f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(avatarDiameter)
            .testTag("user_profile_badge_container")
    ) {
        // 1. Clean Circular Profile Avatar with speaking ring animation ONLY when activelySpeaking (audioLevel > 0.05f && !isMuted)
        AvatarCircleContent(
            profileImageUrl = profileImageUrl,
            diameter = avatarDiameter,
            hasGoldBorder = (showAnimatedVipRing && vipLevel > 0) || activelySpeaking,
            isActivelySpeaking = activelySpeaking,
            audioLevel = if (activelySpeaking) audioLevel else 0f
        )

        // 2. Compact VIP Badge positioned neatly at the bottom edge (bottom: -5.dp)
        if (showBottomPillBadge && (vipLevel > 0 || badgeImageUrl.isNotBlank())) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 5.dp)
                    .testTag("user_profile_vip_badge_overlay")
            ) {
                CompactVipBadgePill(
                    vipLevel = vipLevel.coerceIn(1, 15),
                    badgeImageUrl = badgeImageUrl,
                    height = badgeSize.coerceIn(18.dp, 22.dp)
                )
            }
        } else if (showOverlayBadge && (vipLevel > 0 || badgeImageUrl.isNotBlank())) {
            Box(
                modifier = Modifier
                    .align(if (isBadgeRightAligned) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset(
                        x = if (isBadgeRightAligned) 2.dp else (-2).dp,
                        y = 4.dp
                    )
                    .testTag("user_profile_vip_badge_overlay")
            ) {
                CompactVipBadgePill(
                    vipLevel = vipLevel.coerceIn(1, 15),
                    badgeImageUrl = badgeImageUrl,
                    height = badgeSize.coerceIn(18.dp, 22.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarCircleContent(
    profileImageUrl: String,
    diameter: Dp,
    hasGoldBorder: Boolean = true,
    isActivelySpeaking: Boolean = false,
    audioLevel: Float = 0f
) {
    val activeBorderWidth = if (isActivelySpeaking && audioLevel > 0.05f) {
        (2.2f + audioLevel.coerceIn(0f, 1f) * 1.6f).dp
    } else if (hasGoldBorder) {
        2.dp
    } else {
        1.dp
    }

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color(0xFF1A102A))
            .then(
                if (isActivelySpeaking && audioLevel > 0.05f) {
                    Modifier.border(
                        width = activeBorderWidth,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF10B981),
                                Color(0xFFFDE047),
                                Color(0xFFA855F7)
                            )
                        ),
                        shape = CircleShape
                    )
                } else if (hasGoldBorder) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFDE047),
                                Color(0xFFA855F7),
                                Color(0xFFEAB308)
                            )
                        ),
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "User Profile Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Avatar",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(diameter * 0.5f)
            )
        }
    }
}

/**
 * Distinct "New User" badge/tag displayed next to a user's name on their profile,
 * chat rooms, and participant lists when `isNewUser == true` or `joinedAt` is within 7 days.
 */
@Composable
fun NewUserBadgePill(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF064E3B).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF34D399)),
        modifier = modifier.testTag("new_user_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (compact) 5.dp else 7.dp,
                vertical = if (compact) 1.dp else 2.dp
            )
        ) {
            Text(
                text = if (compact) "🌱 NEW" else "🌱 NEW USER",
                color = Color(0xFFA7F3D0),
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}


