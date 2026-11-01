package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable VIP Animated Border powered by the centralized [VIPAnimationManager].
 * Strictly clips to [outerShape] BEFORE drawing the sweep gradient so the border never
 * overflows into a large circle outside the component bounds.
 */
@Composable
fun VIPAnimatedBorder(
    vipTier: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    innerBackgroundColor: Color = Color(0xFF0B0716),
    isCircular: Boolean = false,
    borderPadding: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveTier = vipTier.coerceIn(0, 3)
    val syncState = VIPAnimationManager.rememberSyncedState()
    val rotationDegrees = syncState.rotationForTier(effectiveTier)

    val gradientColors = when (effectiveTier) {
        3 -> listOf(
            Color(0xFFEAB308), // Rich Gold
            Color(0xFFA855F7), // Neon Purple
            Color(0xFFF59E0B), // Warm Gold
            Color(0xFFEAB308)  // Rich Gold
        )
        2 -> listOf(
            Color(0xFFA855F7), // Neon Purple (#a855f7)
            Color(0xFFEAB308), // Rich Gold (#eab308)
            Color(0xFFD946EF), // Royal Magenta
            Color(0xFFEAB308), // Rich Gold (#eab308)
            Color(0xFFA855F7)  // Neon Purple (#a855f7)
        )
        1 -> listOf(
            Color(0xFF448AFF), // Blue Accent
            Color(0xFFA855F7), // Neon Purple
            Color(0xFF448AFF)  // Blue Accent
        )
        else -> listOf(
            Color(0xFF2E224A),
            Color(0xFF3B2D5E),
            Color(0xFF2E224A)
        )
    }

    val paddingDp = borderPadding ?: when (effectiveTier) {
        3 -> 2.dp
        2 -> 1.5.dp
        1 -> 1.5.dp
        else -> 1.dp
    }

    val outerShape = if (isCircular) CircleShape else RoundedCornerShape(cornerRadius)
    val innerCornerRadius = (cornerRadius - paddingDp).coerceAtLeast(4.dp)
    val innerShape = if (isCircular) CircleShape else RoundedCornerShape(innerCornerRadius)

    Box(
        modifier = modifier
            .clip(outerShape)
            .drawBehind {
                rotate(degrees = if (effectiveTier > 0) rotationDegrees else 0f) {
                    val maxRadius = size.maxDimension * 0.75f
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = gradientColors,
                            center = Offset(size.width / 2f, size.height / 2f)
                        ),
                        radius = maxRadius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            .padding(paddingDp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(innerShape)
                .background(innerBackgroundColor),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Compact, standalone VIP badge pill/chip with a subtle dark luxury background (#1F112B),
 * a thin gold border, the badge icon, and single-line VIP level text.
 * Used inline next to usernames on the Profile Screen, in chat messages, and at the bottom
 * edge of Voice Chatroom seats.
 */
@Composable
fun VIPAnimatedUsernameBadge(
    vipTier: Int,
    vipLevel: Int = vipTier,
    compactHeight: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val displayLevel = vipLevel.coerceIn(1, 15)
    val effectiveTier = vipTier.coerceIn(1, 3)
    val colors = getVipBadgeColors(displayLevel)

    Surface(
        shape = RoundedCornerShape(50),
        color = colors.backgroundColor,
        border = BorderStroke(1.dp, colors.borderColor.copy(alpha = 0.9f)),
        modifier = modifier
            .height(compactHeight)
            .wrapContentWidth(unbounded = true)
            .testTag("vip_badge_tier_$effectiveTier")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            VipCrestBadgeImage(
                vipLevel = displayLevel,
                size = (compactHeight - 6.dp).coerceIn(12.dp, 16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "VIP $displayLevel",
                color = colors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Dark luxury chatroom message item with UserProfileBadge,
 * gold VIP username styling, compact VIP badge pill, and #120d22 neon-bordered bubble.
 */
@Composable
fun VIPChatMessageItem(
    userName: String,
    message: String,
    vipTier: Int,
    vipLevel: Int = vipTier,
    avatarUrl: String = "",
    isBadgeRightAligned: Boolean = true,
    isSystem: Boolean = false,
    isGiftAnnouncement: Boolean = false,
    isNewUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayVipLevel = vipLevel.coerceIn(0, 15)
    val effectiveTier = when {
        displayVipLevel == 0 && vipTier == 0 -> 0
        vipTier in 1..3 && displayVipLevel <= 3 -> vipTier
        displayVipLevel >= 8 -> 3
        displayVipLevel >= 4 -> 2
        else -> 1
    }

    if (isSystem) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1F112B),
                border = BorderStroke(
                    1.dp,
                    if (isGiftAnnouncement) Color(0xFFEAB308) else Color(0xFFA855F7).copy(alpha = 0.6f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isGiftAnnouncement) "🎁 " else "✨ ",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (userName == "System" || userName.isBlank()) message else "$userName $message",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGiftAnnouncement) Color(0xFFEAB308) else Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }
        return
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 6.dp)
            .testTag("vip_chat_message_item")
    ) {
        UserProfileBadge(
            profileImageUrl = avatarUrl,
            badgeImageUrl = "vip_badge_${displayVipLevel.coerceAtLeast(1)}",
            vipLevel = displayVipLevel.coerceAtLeast(1),
            avatarRadius = 18.dp,
            badgeSize = 16.dp,
            isBadgeRightAligned = isBadgeRightAligned,
            showAnimatedVipRing = false,
            showOverlayBadge = false
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // VIP User Name & Compact VIP Badge Pill Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    color = if (effectiveTier > 0) Color(0xFFEAB308) else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isNewUser) {
                    Spacer(modifier = Modifier.width(5.dp))
                    NewUserBadgePill(compact = true)
                }
                if (effectiveTier > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VIPAnimatedUsernameBadge(
                        vipTier = effectiveTier,
                        vipLevel = displayVipLevel.coerceAtLeast(effectiveTier),
                        compactHeight = 18.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dark Luxury Message Bubble (#120d22 with #a855f7 50% opacity border for VIPs)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF120D22),
                border = BorderStroke(
                    width = 1.dp,
                    color = when (effectiveTier) {
                        3 -> Color(0xFFEAB308).copy(alpha = 0.55f)
                        2 -> Color(0xFFA855F7).copy(alpha = 0.55f)
                        1 -> Color(0xFFA855F7).copy(alpha = 0.45f)
                        else -> Color.Transparent
                    }
                )
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
