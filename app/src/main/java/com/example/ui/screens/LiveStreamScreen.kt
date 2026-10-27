package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.GiftType
import com.example.model.LiveStream
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstaLikeRed
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.LiveRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FloatingHeart(
    val id: Long,
    val xOffset: Float,
    val emoji: String = "❤️"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val liveStreams by repository.liveStreams.collectAsState()
    val activeStream by repository.activeLiveStream.collectAsState()
    val streamGifts by repository.streamGifts.collectAsState()

    var showBroadcasterMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LiveRed
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Live Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showBroadcasterMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("go_live_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Go Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Trending Live Broadcasts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            items(liveStreams, key = { it.id }) { stream ->
                LiveStreamCard(
                    stream = stream,
                    onClick = { repository.openLiveStream(stream) }
                )
            }
        }
    }

    // Watch Live Stream Full Screen Experience
    activeStream?.let { stream ->
        LiveStreamPlayerDialog(
            stream = stream,
            gifts = streamGifts,
            onDismiss = { repository.closeLiveStream() },
            onSendComment = { repository.sendLiveComment(it) },
            onSendHeart = { repository.sendLiveHeart() },
            onSendGift = { repository.sendStreamGift(it) }
        )
    }

    // Go Live Broadcaster Modal
    if (showBroadcasterMode) {
        BroadcasterDialog(
            currentUser = repository.currentUser.value,
            onDismiss = { showBroadcasterMode = false }
        )
    }
}

@Composable
private fun LiveStreamCard(
    stream: LiveStream,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
            .testTag("live_stream_card_${stream.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = stream.streamPreviewUrl,
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges: LIVE + Viewer count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LiveRed
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stream.viewerCount}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = stream.hostAvatar,
                    contentDescription = stream.hostName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${stream.hostName} • ${stream.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Watch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LiveStreamPlayerDialog(
    stream: LiveStream,
    gifts: List<com.example.model.StreamGift>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
    onSendHeart: () -> Unit,
    onSendGift: (GiftType) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var floatingHearts by remember { mutableStateOf(listOf<FloatingHeart>()) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("live_stream_player")
        ) {
            // Live Stream Video Feed
            AsyncImage(
                model = stream.streamPreviewUrl,
                contentDescription = "Live Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradients top and bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            // Top Header: Host info & Viewers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = stream.hostAvatar,
                    contentDescription = stream.hostName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = stream.hostUsername,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LiveRed
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stream.viewerCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Floating Animated Hearts Layer
            floatingHearts.forEach { heart ->
                FloatingHeartItem(
                    heart = heart,
                    onFinished = {
                        floatingHearts = floatingHearts.filter { it.id != heart.id }
                    }
                )
            }

            // Latest Gift Banner Celebration (if recently sent)
            gifts.lastOrNull()?.let { gift ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 60.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = gift.type.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${gift.senderName} sent ${gift.type.label}!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bottom Area: Scrolling Comments & Interactive Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Scrolling Live Comments
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    reverseLayout = true
                ) {
                    items(stream.comments.reversed()) { comment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = "${comment.username} ",
                                fontWeight = FontWeight.Bold,
                                color = if (comment.isHost) DarkAccent else Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = comment.text,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Comment Input & Interactive Reaction Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text("Comment on live...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (commentText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSendComment(commentText)
                                commentText = ""
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Comment", tint = DarkAccent)
                        }
                    } else {
                        // Gift Button
                        IconButton(
                            onClick = { showGiftSheet = true },
                            modifier = Modifier.testTag("send_gift_button")
                        ) {
                            Text("🎁", fontSize = 22.sp)
                        }

                        // Floating Heart Explosion Trigger
                        IconButton(
                            onClick = {
                                onSendHeart()
                                val newHeart = FloatingHeart(
                                    id = System.nanoTime(),
                                    xOffset = Random.nextFloat() * 60f - 30f,
                                    emoji = listOf("❤️", "💖", "🔥", "✨").random()
                                )
                                floatingHearts = floatingHearts + newHeart
                            },
                            modifier = Modifier.testTag("send_live_heart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart",
                                tint = InstaLikeRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Gift Sending Modal Sheet
            if (showGiftSheet) {
                GiftSendingModal(
                    onDismiss = { showGiftSheet = false },
                    onSelectGift = { giftType ->
                        onSendGift(giftType)
                        showGiftSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingHeartItem(
    heart: FloatingHeart,
    onFinished: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(heart.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        onFinished()
    }

    val yOffset = (1f - animProgress.value) * 350f
    val alpha = (1f - animProgress.value).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = heart.emoji,
            fontSize = (24 + (animProgress.value * 12)).sp,
            modifier = Modifier
                .offset(x = heart.xOffset.dp, y = (-yOffset).dp)
                .alpha(alpha)
        )
    }
}

@Composable
private fun GiftSendingModal(
    onDismiss: () -> Unit,
    onSelectGift: (GiftType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color(0xFF1E1E28),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Send Super Gift to Streamer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GiftType.entries.forEach { gift ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onSelectGift(gift) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("gift_${gift.name}")
                    ) {
                        Text(text = gift.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = gift.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${gift.coins} pts", fontSize = 9.sp, color = DarkAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcasterDialog(
    currentUser: com.example.model.UserProfile,
    onDismiss: () -> Unit
) {
    var isBroadcasting by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(true) }

    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            while (true) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101018))
        ) {
            // Simulated Camera Stream
            AsyncImage(
                model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1080&q=80",
                contentDescription = "Camera Feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBroadcasting) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LiveRed
                    ) {
                        val minutes = durationSeconds / 60
                        val secs = durationSeconds % 60
                        Text(
                            text = "LIVE ${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera }
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Bottom Broadcast Trigger
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { isBroadcasting = !isBroadcasting },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBroadcasting) LiveRed else InstaPurple
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("broadcast_toggle_button")
                ) {
                    Text(
                        text = if (isBroadcasting) "End Broadcast" else "Start Live Broadcast 🔴",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
