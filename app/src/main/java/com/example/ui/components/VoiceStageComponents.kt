package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FloatingRoomReaction
import com.example.model.Participant
import com.example.model.ParticipantRole
import com.example.model.UserProfile
import com.example.model.VipTier

private val LuxBgDark = Color(0xFF090514)
private val LuxCardBg = Color(0xFF140D26)
private val LuxNeonPurple = Color(0xFFA855F7)
private val LuxGold = Color(0xFFEAB308)
private val LuxCyan = Color(0xFF06B6D4)
private val LuxRed = Color(0xFFEF4444)
private val LuxGreen = Color(0xFF10B981)

/**
 * Instant notification banner displayed to the Host & Co-Hosts when any listener raises their hand
 * to request joining the speaker stage. Clicking 'Accept' moves the participant to an open slot.
 */
@Composable
fun HostSpeakRequestsBanner(
    pendingRequests: List<Participant>,
    onAcceptRequest: (Participant) -> Unit,
    onDeclineRequest: (Participant) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = pendingRequests.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val topRequester = pendingRequests.firstOrNull() ?: return@AnimatedVisibility
        Surface(
            color = Color(0xEB1B0F36),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(LuxNeonPurple, LuxGold)),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("host_speak_request_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = topRequester.avatarUrl,
                            contentDescription = topRequester.name,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, LuxGold, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "✋",
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(LuxNeonPurple, CircleShape)
                                .padding(2.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = topRequester.name,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (pendingRequests.size > 1) {
                                Surface(
                                    color = LuxGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "+${pendingRequests.size - 1} more",
                                        color = LuxGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Wants to join the speaker stage",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = { onDeclineRequest(topRequester) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Decline",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { onAcceptRequest(topRequester) },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("accept_speak_request_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept Request",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Accept",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Audience Listeners strip showing all listeners in the room, highlighting raised hands,
 * and allowing anyone to tap a listener to open their Mini Profile Card (or allowing Hosts/Co-Hosts to
 * invite them to the stage directly).
 */
@Composable
fun AudienceListenersStrip(
    listeners: List<Participant>,
    roomTags: List<String>,
    roomDescription: String,
    isHostOrCoHost: Boolean,
    onListenerClick: (Participant) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Room Topic Tags & Description Strip
        if (roomTags.isNotEmpty() || roomDescription.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                roomTags.forEach { tag ->
                    Surface(
                        color = LuxNeonPurple.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(0.8.dp, LuxNeonPurple.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "#$tag",
                            color = Color(0xFFE9D5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (roomDescription.isNotBlank()) {
                    Text(
                        text = roomDescription,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (listeners.isNotEmpty()) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = LuxNeonPurple.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headset,
                                contentDescription = "Audience Listeners",
                                tint = LuxGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Audience (${listeners.size})",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(listeners, key = { it.id }) { listener ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (listener.handRaised) LuxNeonPurple.copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.07f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (listener.handRaised) LuxGold else Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { onListenerClick(listener) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("audience_listener_${listener.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                AsyncImage(
                                    model = listener.avatarUrl,
                                    contentDescription = listener.name,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = listener.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (listener.showNewUserBadge) {
                                    NewUserBadgePill(compact = true)
                                }
                                if (listener.handRaised) {
                                    Text(text = "✋", fontSize = 11.sp)
                                } else if (isHostOrCoHost) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "Invite to Stage",
                                        tint = LuxGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Emoji Reaction Bar for sending real-time floating emoji reactions (❤️, 👍, 👏, 🔥, 🎉, 👑).
 */
@Composable
fun FloatingEmojiReactionBar(
    onReactionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("❤️", "👍", "👏", "🔥", "🎉", "👑")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC120B24))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("floating_emoji_reaction_bar"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onReactionSelected(emoji) }
                    .testTag("emoji_reaction_$emoji"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Animated Floating Emoji Reactions Overlay that renders upward-drifting emojis across the stage.
 */
@Composable
fun FloatingEmojiReactionOverlay(
    reactions: List<FloatingRoomReaction>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        reactions.forEachIndexed { index, reaction ->
            key(reaction.id) {
                val progress = remember { Animatable(0f) }
                val horizontalSeed = remember(reaction.id) {
                    ((reaction.id.hashCode() and 0x7FFFFFFF) % 140) - 70
                }
                LaunchedEffect(reaction.id) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
                    )
                }
                val alpha = (1f - progress.value).coerceIn(0f, 1f)
                val translateY = -progress.value * 420f
                val scale = 0.8f + (0.45f * kotlin.math.sin(progress.value * Math.PI).toFloat())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(end = (24 + (index % 3) * 28).dp, bottom = 92.dp)
                        .graphicsLayer {
                            translationY = translateY
                            translationX = horizontalSeed * progress.value
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    Text(
                        text = reaction.emoji,
                        fontSize = 30.sp
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = reaction.senderName.take(10),
                            color = LuxGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Gift Counter Pill shown beneath speaker avatars on the stage.
 */
@Composable
fun AnimatedSpeakerGiftCounterPill(
    giftsReceivedCoins: Long,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateIntAsState(
        targetValue = giftsReceivedCoins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "speakerGiftCounter"
    )
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(50),
        modifier = modifier.border(
            width = 0.8.dp,
            color = if (giftsReceivedCoins > 0) LuxGold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = if (giftsReceivedCoins > 0) "🎁" else "💎", fontSize = 9.sp)
            Text(
                text = formatGiftCountCompact(animatedCount.toLong()),
                color = if (giftsReceivedCoins > 0) LuxGold else Color(0xFF38BDF8),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun formatGiftCountCompact(value: Long): String {
    return when {
        value >= 1_000_000L -> String.format("%.1fM", value / 1_000_000f)
        value >= 1_000L -> String.format("%.1fK", value / 1_000f)
        else -> value.toString()
    }
}

/**
 * Rich Interactive Mini Profile Card BottomSheet opened when tapping any Speaker or Listener slot.
 * Displays avatar with VIPAnimatedBorder, bio, follow stats, Follow/Unfollow toggle, Send Gift,
 * and role-based Host/Co-Host stage & moderation controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMiniProfileSheet(
    participant: Participant,
    resolvedUserProfile: UserProfile?,
    currentUserId: String,
    isFollowing: Boolean,
    isCallerRoomOwner: Boolean,
    isCallerCoHostOrAdmin: Boolean,
    isTargetCoHost: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onSendGiftToUser: () -> Unit,
    onApproveSpeakRequest: () -> Unit,
    onInviteToStage: () -> Unit,
    onMoveToAudience: () -> Unit,
    onToggleMuteParticipant: () -> Unit,
    onToggleCoHostRole: () -> Unit,
    onKickParticipant: () -> Unit,
    onBanParticipant: () -> Unit
) {
    val isSelf = participant.id == currentUserId
    val canModerateTarget = (isCallerRoomOwner || isCallerCoHostOrAdmin) &&
        !isSelf &&
        participant.role != ParticipantRole.OWNER

    val vipTierInt = when {
        participant.vipTier.tierLevel >= 4 -> 3
        participant.vipTier.tierLevel >= 2 -> 2
        else -> 1
    }

    val bioText = resolvedUserProfile?.bio?.takeIf { it.isNotBlank() }
        ?: participant.bio.takeIf { it.isNotBlank() }
        ?: "NOICE Live Voice Creator & Music Enthusiast 🎙️✨"

    val followersCount = resolvedUserProfile?.followersCount ?: participant.followersCount
    val followingCount = resolvedUserProfile?.followingCount ?: participant.followingCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LuxBgDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("participant_mini_profile_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar + Synchronized VIPAnimatedBorder
            VIPAnimatedBorder(
                vipTier = vipTierInt,
                isCircular = true,
                cornerRadius = 48.dp
            ) {
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = participant.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name + Role Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = participant.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (participant.showNewUserBadge) {
                    NewUserBadgePill(compact = false)
                }
                val roleLabel = when {
                    participant.role == ParticipantRole.OWNER -> "HOST"
                    participant.role == ParticipantRole.CO_HOST || isTargetCoHost -> "CO-HOST"
                    participant.slotIndex >= 0 -> "SPEAKER #${participant.slotIndex + 1}"
                    participant.handRaised -> "✋ REQUESTING STAGE"
                    else -> "LISTENER"
                }
                val roleColor = when {
                    participant.role == ParticipantRole.OWNER -> LuxGold
                    participant.role == ParticipantRole.CO_HOST || isTargetCoHost -> LuxCyan
                    participant.slotIndex >= 0 -> LuxNeonPurple
                    else -> Color.White.copy(alpha = 0.6f)
                }
                Surface(
                    color = roleColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, roleColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = roleLabel,
                        color = roleColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "@${participant.username.ifBlank { participant.name.lowercase().replace(" ", "_") }}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Level, Wealth, Charm badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LuxNeonPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "LV.${participant.userLevel}",
                        color = Color(0xFFD8B4FE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = LuxGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "💎 Wealth ${participant.wealthLevel}",
                        color = LuxGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = Color(0xFFEC4899).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "✨ Charm ${participant.charmLevel}",
                        color = Color(0xFFF472B6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Bio
            Text(
                text = bioText,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row: Followers | Following | Stage Gifts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuxCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniStatColumn(value = followersCount.toString(), label = "Followers")
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                MiniStatColumn(value = followingCount.toString(), label = "Following")
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                MiniStatColumn(
                    value = "💎 ${participant.giftsReceivedCoins}",
                    label = "Stage Gifts"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Audience Actions: Follow / Unfollow & Send Gift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isSelf) {
                    Button(
                        onClick = onToggleFollow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color.White.copy(alpha = 0.12f) else LuxNeonPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("mini_profile_follow_button")
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = onSendGiftToUser,
                    colors = ButtonDefaults.buttonColors(containerColor = LuxGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("mini_profile_send_gift_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send Gift",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Host & Co-Host Stage and Moderation Controls
            if (canModerateTarget) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "STAGE & MODERATOR CONTROLS",
                    color = LuxGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isTargetOnStage = participant.slotIndex >= 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isTargetOnStage) {
                        // Listener -> Approve Speak Request OR Invite to Stage
                        Button(
                            onClick = {
                                if (participant.handRaised) onApproveSpeakRequest() else onInviteToStage()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("mini_profile_invite_or_approve_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (participant.handRaised) "Accept Request" else "Invite to Stage",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Speaker -> Mute/Unmute & Move to Audience (Demote)
                        OutlinedButton(
                            onClick = onToggleMuteParticipant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("mini_profile_mute_button")
                        ) {
                            Icon(
                                imageVector = if (participant.isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (participant.isMuted) LuxGreen else LuxGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (participant.isMuted) "Unmute Mic" else "Mute Mic",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onMoveToAudience,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("mini_profile_demote_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = LuxCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "To Audience",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCallerRoomOwner) {
                        OutlinedButton(
                            onClick = onToggleCoHostRole,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("mini_profile_cohost_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = LuxGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTargetCoHost) "Remove Co-Host" else "Make Co-Host",
                                color = LuxGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onKickParticipant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("mini_profile_kick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kick",
                            color = Color(0xFFF97316),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onBanParticipant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("mini_profile_ban_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = LuxRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ban",
                            color = LuxRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 10.sp
        )
    }
}

/**
 * Dialog shown when a Host or Co-Host taps an empty or locked speaker slot on the stage,
 * allowing them to either join that slot themselves, lock/unlock the slot, or invite a queued listener.
 */
@Composable
fun HostSlotActionDialog(
    slotIndex: Int,
    isSlotLocked: Boolean,
    pendingListeners: List<Participant>,
    audienceListeners: List<Participant>,
    onDismiss: () -> Unit,
    onTakeSeat: () -> Unit,
    onToggleLockSlot: () -> Unit,
    onAssignListenerToSlot: (Participant) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxCardBg,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSlotLocked) Icons.Default.Lock else Icons.Default.EventSeat,
                    contentDescription = null,
                    tint = LuxGold
                )
                Text(
                    text = "Stage Slot #${slotIndex + 1}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isSlotLocked)
                        "This slot is currently locked. Listeners cannot join unless invited or unlocked."
                    else
                        "Manage this speaker slot or invite an audience member onto the stage.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )

                Button(
                    onClick = {
                        onTakeSeat()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxNeonPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Join Slot #${slotIndex + 1} Myself", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onToggleLockSlot()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isSlotLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = LuxGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSlotLocked) "Unlock Slot #${slotIndex + 1}" else "Lock Slot #${slotIndex + 1}",
                        color = LuxGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                val candidates = (pendingListeners + audienceListeners).distinctBy { it.id }
                if (candidates.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = "Invite / Approve Listener to Slot #${slotIndex + 1}:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    candidates.take(4).forEach { listener ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    onAssignListenerToSlot(listener)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = listener.avatarUrl,
                                    contentDescription = listener.name,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = listener.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (listener.showNewUserBadge) {
                                    NewUserBadgePill(compact = true)
                                }
                                if (listener.handRaised) {
                                    Text("✋", fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = if (listener.handRaised) "Accept" else "Invite",
                                color = LuxGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
