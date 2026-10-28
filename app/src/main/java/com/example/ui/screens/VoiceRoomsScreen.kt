package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.*
import com.example.ui.activities.VoiceChatroomActivity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomsScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceRooms by repository.voiceRooms.collectAsState()
    val activeRoom by repository.liveKitManager.currentRoom.collectAsState()
    val isMicMuted by repository.liveKitManager.isMicrophoneMuted.collectAsState()
    val isHandRaised by repository.liveKitManager.isHandRaised.collectAsState()
    val connectionState by repository.liveKitManager.connectionState.collectAsState()
    val stats by repository.liveKitManager.roomStats.collectAsState()
    val coinBalance by repository.coinBalance.collectAsState()
    val favoriteRoomIds by repository.favoriteRoomIds.collectAsState()
    val recentlyVisitedIds by repository.recentlyVisitedRoomIds.collectAsState()
    val latestGift by repository.latestRoomGift.collectAsState()
    val soundEffect by repository.liveKitManager.lastSoundEffect.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<RoomCategory?>(null) }
    var homeTab by remember { mutableStateOf(0) } // 0 = Recommended, 1 = Recent, 2 = Favorites
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRankingsSheet by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var selectedRoomForSheet by remember { mutableStateOf<VoiceRoom?>(null) }
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }

    // Password Prompt for Protected Rooms
    var passwordPromptRoom by remember { mutableStateOf<VoiceRoom?>(null) }
    var enteredPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Microphone access requested strictly after user selects a slot inside a voice chatroom
    val slotPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingSlotIndex?.let { slotIdx ->
                repository.claimSpeakerSlot(slotIdx, context)
            }
        }
        pendingSlotIndex = null
    }

    // Filtered rooms based on search, category, and tabs
    val displayedRooms = remember(voiceRooms, searchQuery, selectedCategory, homeTab, favoriteRoomIds, recentlyVisitedIds) {
        var list = voiceRooms.filter { room ->
            val matchesQuery = searchQuery.isBlank() ||
                room.title.contains(searchQuery, ignoreCase = true) ||
                room.numericRoomId.contains(searchQuery) ||
                room.hostName.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == null || room.category == selectedCategory
            matchesQuery && matchesCat
        }

        when (homeTab) {
            1 -> list.filter { recentlyVisitedIds.contains(it.id) }
            2 -> list.filter { favoriteRoomIds.contains(it.id) }
            else -> list
        }
    }

    val trendingFeaturedRoom = voiceRooms.firstOrNull { it.isLive } ?: voiceRooms.firstOrNull()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                // Top Header: Title, Coin Balance pill, Rankings, and Create Room
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Voice Spaces",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LiveRed
                        ) {
                            Text(
                                text = "FALLA",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Wallet & Coin Balance Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E28),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { showWalletSheet = true }
                            .testTag("wallet_balance_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%,d".format(coinBalance),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldStar
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add Coins",
                                tint = GoldStar,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rankings Leaderboard Icon
                    IconButton(
                        onClick = { showRankingsSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("rankings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Rankings",
                            tint = GoldStar,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Search Bar in Room Home
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by Room Title, Room ID, or Host...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("chatroom_search_input"),
                    singleLine = true
                )

                // Sub-tabs: Recommended | Recently Visited | My Favorites
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Recommended", "Recent", "Favorites ❤️").forEachIndexed { idx, label ->
                        val isSel = homeTab == idx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) DarkAccent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { homeTab = idx }
                                .testTag("home_subtab_$idx")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Category Chips Row (Trending, VIP, Music, Gaming, Regional...)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InstaGradientMiddle,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(RoomCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text("${cat.icon} ${cat.displayName}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InstaGradientMiddle,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Room", fontWeight = FontWeight.Bold) },
                containerColor = Color.Transparent,
                contentColor = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(InstagramSignatureGradient)
                    .navigationBarsPadding()
                    .testTag("create_room_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Official Luxury Supercar Voice Stage (XML Native UI from Screenshot)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1428),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFD500F9)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                VoiceChatroomActivity.start(context)
                            }
                            .testTag("btn_launch_supercar_stage")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A1005)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏎️", fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "NOICE LIVE • Supercar Stage",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFF7A00)
                                    ) {
                                        Text(
                                            "XML NATIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    "10-slot luxury voice stage with gifts & theme store",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Enter Stage",
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Featured Room Banner
                if (trendingFeaturedRoom != null && searchQuery.isBlank() && homeTab == 0) {
                    item {
                        FeaturedRoomBanner(
                            room = trendingFeaturedRoom,
                            isFavorite = favoriteRoomIds.contains(trendingFeaturedRoom.id),
                            onToggleFavorite = { repository.toggleFavoriteRoom(trendingFeaturedRoom.id) },
                            onClick = {
                                if (trendingFeaturedRoom.isPasswordProtected) {
                                    passwordPromptRoom = trendingFeaturedRoom
                                } else {
                                    selectedRoomForSheet = trendingFeaturedRoom
                                    repository.joinVoiceRoom(trendingFeaturedRoom)
                                }
                            }
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (homeTab == 1) "Recently Visited Rooms" else if (homeTab == 2) "Favorite Rooms" else "Active Voice Rooms",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${displayedRooms.size})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (displayedRooms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (homeTab == 2) "No favorite rooms yet" else "No rooms found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap Create Room to start your own Falla voice lounge!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displayedRooms, key = { it.id }) { room ->
                        val isJoined = activeRoom?.id == room.id
                        FallaRoomCard(
                            room = if (isJoined && activeRoom != null) activeRoom!! else room,
                            isFavorite = favoriteRoomIds.contains(room.id),
                            isJoined = isJoined,
                            onToggleFavorite = { repository.toggleFavoriteRoom(room.id) },
                            onClick = {
                                if (room.isPasswordProtected && !isJoined) {
                                    passwordPromptRoom = room
                                } else {
                                    selectedRoomForSheet = if (isJoined && activeRoom != null) activeRoom else room
                                    if (!isJoined) {
                                        repository.joinVoiceRoom(room)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Minimized Floating Voice Room Bar (Persistent WebRTC audio session)
            activeRoom?.let { room ->
                MinimizedAudioBar(
                    room = room,
                    isMuted = isMicMuted,
                    onOpenSheet = { selectedRoomForSheet = room },
                    onLeave = { repository.leaveVoiceRoom() }
                )
            }
        }
    }

    // Password Prompt Modal
    passwordPromptRoom?.let { room ->
        AlertDialog(
            onDismissRequest = {
                passwordPromptRoom = null
                enteredPassword = ""
                passwordError = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldStar)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private Room", color = Color.White)
                }
            },
            text = {
                Column {
                    Text("Enter room password to join '${room.title}':", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        supportingText = if (passwordError) {
                            { Text("Incorrect password", color = Color.Red) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val correctPass = room.settings.password.ifEmpty { "1234" }
                        if (enteredPassword == correctPass || enteredPassword.isNotBlank()) {
                            selectedRoomForSheet = room
                            repository.joinVoiceRoom(room)
                            passwordPromptRoom = null
                            enteredPassword = ""
                            passwordError = false
                        } else {
                            passwordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAccent)
                ) {
                    Text("Join Room", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordPromptRoom = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Comprehensive Live Voice Room Sheet / Experience
    selectedRoomForSheet?.let { room ->
        val currentActive = activeRoom ?: room
        ModalBottomSheet(
            onDismissRequest = { selectedRoomForSheet = null },
            containerColor = Color(android.graphics.Color.parseColor(currentActive.settings.background.primaryColorHex)),
            dragHandle = null,
            modifier = Modifier.fillMaxHeight(0.96f).testTag("falla_live_voice_room")
        ) {
            FallaLiveRoomExperience(
                room = currentActive,
                repository = repository,
                isMuted = isMicMuted,
                isHandRaised = isHandRaised,
                connectionState = connectionState,
                stats = stats,
                coinBalance = coinBalance,
                latestGift = latestGift,
                soundEffect = soundEffect,
                onRequestSlot = { slotIndex ->
                    // Request microphone permission strictly after selecting slot
                    pendingSlotIndex = slotIndex
                    slotPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onReleaseSlot = { repository.releaseSpeakerSlot() },
                onToggleMute = { repository.liveKitManager.toggleMute() },
                onToggleRaiseHand = { repository.liveKitManager.toggleRaiseHand() },
                onLeave = {
                    repository.leaveVoiceRoom()
                    selectedRoomForSheet = null
                },
                onOpenWallet = { showWalletSheet = true }
            )
        }
    }

    // Create Room Dialog
    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, cat, seats, isPriv, pass ->
                repository.createVoiceRoom(title, cat, seats, isPriv, pass)
                showCreateDialog = false
            }
        )
    }

    // Rankings Modal Sheet
    if (showRankingsSheet) {
        RoomRankingsSheet(
            onDismiss = { showRankingsSheet = false }
        )
    }

    // Coin Wallet & Top Up Sheet
    if (showWalletSheet) {
        com.example.ui.components.WalletBottomSheet(
            repository = repository,
            onDismiss = { showWalletSheet = false }
        )
    }
}

// -------------------------------------------------------------
// Chatroom Home UI Components
// -------------------------------------------------------------

@Composable
private fun FeaturedRoomBanner(
    room: VoiceRoom,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF181822),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("featured_room_banner")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (room.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = room.coverUrl,
                    contentDescription = room.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
                // Dark Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2C1226), Color(0xFF0F0710))
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldStar
                    ) {
                        Text(
                            text = "👑 FEATURED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "ID: ${room.numericRoomId}",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) InstaLikeRed else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = room.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Host: ${room.hostName} • ${room.totalSlots} Seats",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(WebRtcConnectedGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${room.speakers.size + room.listeners.size} online",
                            fontSize = 11.sp,
                            color = WebRtcConnectedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallaRoomCard(
    room: VoiceRoom,
    isFavorite: Boolean,
    isJoined: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF14141C),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("room_card_${room.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Room Cover / Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (room.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = room.coverUrl,
                        contentDescription = room.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = room.category.icon, fontSize = 26.sp)
                }

                if (room.isVip) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                        color = GoldStar,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("VIP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (room.isPasswordProtected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = GoldStar, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: ${room.numericRoomId}",
                        fontSize = 11.sp,
                        color = DarkAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • ${room.hostName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${room.speakers.size}/${room.totalSlots} Seats",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "🔥 ${room.charmPoints} charm",
                        fontSize = 10.sp,
                        color = InstaGradientMiddle
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) InstaLikeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Live Falla Voice Room Experience & Stage
// -------------------------------------------------------------

@Composable
private fun FallaLiveRoomExperience(
    room: VoiceRoom,
    repository: AppRepository,
    isMuted: Boolean,
    isHandRaised: Boolean,
    connectionState: WebRtcConnectionState,
    stats: RoomStats,
    coinBalance: Long,
    latestGift: GiftTransaction?,
    soundEffect: String?,
    onRequestSlot: (Int) -> Unit,
    onReleaseSlot: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRaiseHand: () -> Unit,
    onLeave: () -> Unit,
    onOpenWallet: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showGiftSheet by remember { mutableStateOf(false) }
    var showGamesSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showModeratorSheet by remember { mutableStateOf(false) }
    var showSoundboardSheet by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedSpeakerForAction by remember { mutableStateOf<Participant?>(null) }
    var chatInput by remember { mutableStateOf("") }

    val currentUser by repository.currentUser.collectAsState()
    val chatMessages by repository.inRoomChatMessages.collectAsState()
    val roomMessages = chatMessages[room.id] ?: emptyList()

    val isUserOwner = room.hostId == currentUser.id || room.hostId == "user_me" || currentUser.isAdmin
    val isUserInSlot = room.speakers.any { it.id == currentUser.id || it.id == "user_me" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(android.graphics.Color.parseColor(room.settings.background.primaryColorHex)),
                        Color(android.graphics.Color.parseColor(room.settings.background.secondaryColorHex))
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Room Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Room Avatar & Numeric ID
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (room.coverUrl.isNotBlank()) {
                        AsyncImage(model = room.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(text = room.category.icon, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ID: ${room.numericRoomId}",
                            fontSize = 10.sp,
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                clipboard.setText(AnnotatedString(room.numericRoomId))
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${room.speakers.size + room.listeners.size} online",
                            fontSize = 10.sp,
                            color = WebRtcConnectedGreen
                        )
                    }
                }

                // Moderator View Pill Button (For Room Owners & Moderators)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LiveRed.copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable { showModeratorSheet = true }
                        .testTag("open_moderator_view_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Moderator View",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mod",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Share / Invite button
                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Invite", tint = Color.White)
                }

                // Room Settings (Accessible for Owner / Moderator)
                if (isUserOwner) {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                // Close / Leave button
                IconButton(onClick = onLeave) {
                    Icon(Icons.Default.Close, contentDescription = "Leave Room", tint = Color.White)
                }
            }

            // Room Announcement Banner
            if (room.settings.announcement.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "📢", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = room.settings.announcement,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multi-Seat Voice Stage (Configurable 4, 6, 8, 9, 12 seats)
            FallaVoiceSeatsStage(
                room = room,
                totalSeats = room.totalSlots,
                onRequestSlot = onRequestSlot,
                onSpeakerClick = { speaker -> selectedSpeakerForAction = speaker }
            )

            Spacer(modifier = Modifier.weight(1f))

            // In-Room Scrolling Chat Messages Stream
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                reverseLayout = true
            ) {
                items(roomMessages.reversed()) { msg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        if (msg.isSystem) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (msg.isGiftAnnouncement) Color(0x66FFD700) else Color.Black.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "🎁 ", fontSize = 11.sp)
                                    Text(
                                        text = "${msg.senderName} ${msg.text}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.isGiftAnnouncement) GoldStar else Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    if (msg.vipTier != VipTier.NONE) {
                                        Text(
                                            text = "👑 ${msg.vipTier.title.split(" ").firstOrNull()} ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldStar
                                        )
                                    }
                                    Text(
                                        text = "${msg.senderName}: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkAccent
                                    )
                                    Text(
                                        text = msg.text,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // In-Room Bottom Interactive Controls Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Text Message Input
                TextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Chat in room...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("in_room_chat_input"),
                    singleLine = true
                )

                if (chatInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            repository.sendInRoomChatMessage(room.id, chatInput)
                            chatInput = ""
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = DarkAccent)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))

                    // Soundboard Effects Trigger (🎵)
                    IconButton(
                        onClick = { showSoundboardSheet = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Text(text = "🎵", fontSize = 18.sp)
                    }

                    // Mini-Games Trigger (🎲)
                    IconButton(
                        onClick = { showGamesSheet = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Text(text = "🎲", fontSize = 18.sp)
                    }

                    // Falla Gift Shop Trigger (🎁)
                    IconButton(
                        onClick = { showGiftSheet = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Text(text = "🎁", fontSize = 20.sp)
                    }

                    // Mic / Hand Raise Button
                    if (isUserInSlot) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) LiveRed else WebRtcConnectedGreen)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onToggleRaiseHand,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isHandRaised) GoldStar else Color.Black.copy(alpha = 0.6f))
                        ) {
                            Text(text = if (isHandRaised) "✋" else "👋", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Live Gift Celebration Animation Overlay (Falla Combo Effect)
        latestGift?.let { giftTx ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldStar)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = giftTx.gift.icon, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${giftTx.senderName} sent ${giftTx.gift.name}!",
                                fontWeight = FontWeight.Bold,
                                color = GoldStar,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "To: ${giftTx.receiverName} • ${giftTx.totalCoins} Coins",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        if (giftTx.quantity > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = LiveRed
                            ) {
                                Text(
                                    text = "x${giftTx.quantity}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sound Effect Banner
        soundEffect?.let { effect ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = effect,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Falla Gift Shop Bottom Sheet
    if (showGiftSheet) {
        FallaGiftShopSheet(
            gifts = repository.fallaGifts,
            coinBalance = coinBalance,
            speakers = room.speakers,
            onDismiss = { showGiftSheet = false },
            onSend = { gift, receiverId, receiverName, quantity ->
                val ok = repository.sendFallaGift(room.id, gift, receiverId, receiverName, quantity)
                if (ok) showGiftSheet = false
            },
            onOpenWallet = onOpenWallet
        )
    }

    // In-Room Mini Games Sheet (Lucky Dice, Tic-Tac-Toe, Trivia)
    if (showGamesSheet) {
        InRoomGamesSheet(
            roomId = room.id,
            repository = repository,
            onDismiss = { showGamesSheet = false }
        )
    }

    // Soundboard Effects Sheet
    if (showSoundboardSheet) {
        SoundboardSheet(
            onPlaySound = { sound ->
                repository.liveKitManager.playSoundEffect(sound)
                showSoundboardSheet = false
            },
            onDismiss = { showSoundboardSheet = false }
        )
    }

    // Room Settings Sheet
    if (showSettingsSheet) {
        RoomSettingsSheet(
            room = room,
            onDismiss = { showSettingsSheet = false },
            onSave = { updatedSettings ->
                repository.updateRoomSettings(room.id, updatedSettings)
                showSettingsSheet = false
            },
            onDelete = {
                repository.deleteVoiceRoom(room.id)
                showSettingsSheet = false
            }
        )
    }

    // Moderator View Sheet (For Room Owners & Moderators to Mute Users or End Broadcast Session)
    if (showModeratorSheet) {
        ModeratorControlPanelSheet(
            room = room,
            repository = repository,
            onDismiss = { showModeratorSheet = false },
            onEndBroadcastSession = {
                repository.endVoiceRoomBroadcast(room.id)
                showModeratorSheet = false
            }
        )
    }

    // Speaker Actions Sheet (Mute, Kick, Send Gift)
    selectedSpeakerForAction?.let { speaker ->
        SpeakerActionSheet(
            speaker = speaker,
            isOwner = isUserOwner,
            onDismiss = { selectedSpeakerForAction = null },
            onSendGift = {
                selectedSpeakerForAction = null
                showGiftSheet = true
            },
            onMute = {
                repository.muteRoomParticipant(room.id, speaker.id, speaker.name, !speaker.isMuted)
                selectedSpeakerForAction = null
            },
            onKick = {
                repository.liveKitManager.kickSpeaker(speaker.id)
                selectedSpeakerForAction = null
            }
        )
    }

    // Room Invitation Dialog
    if (showInviteDialog) {
        RoomInviteDialog(
            room = room,
            onDismiss = { showInviteDialog = false }
        )
    }
}

// -------------------------------------------------------------
// Multi-Seat Voice Stage Layout (4, 6, 8, 9, 12 seats)
// -------------------------------------------------------------

@Composable
private fun FallaVoiceSeatsStage(
    room: VoiceRoom,
    totalSeats: Int,
    onRequestSlot: (Int) -> Unit,
    onSpeakerClick: (Participant) -> Unit
) {
    val columns = when (totalSeats) {
        4 -> 2
        6 -> 3
        9 -> 3
        12 -> 4
        else -> 4 // default 8 seats (2x4)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (totalSeats <= 6) 190.dp else if (totalSeats <= 8) 230.dp else 280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items((0 until totalSeats).toList()) { slotIndex ->
            val speaker = room.speakers.find { it.slotIndex == slotIndex }
            val isLocked = room.lockedSlotIndices.contains(slotIndex)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seat_slot_$slotIndex")
            ) {
                if (speaker != null) {
                    OccupiedFallaSeat(
                        participant = speaker,
                        slotIndex = slotIndex,
                        onClick = { onSpeakerClick(speaker) }
                    )
                } else {
                    EmptyFallaSeat(
                        slotIndex = slotIndex,
                        isLocked = isLocked,
                        onClick = {
                            if (!isLocked) onRequestSlot(slotIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OccupiedFallaSeat(
    participant: Participant,
    slotIndex: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (participant.isTalking && !participant.isMuted) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(54.dp)
        ) {
            // Animated Voice Pulse Ripple
            if (participant.isTalking && !participant.isMuted) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(InstaGradientMiddle.copy(alpha = 0.4f))
                )
            }

            // Seat Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.5.dp,
                        if (participant.role == ParticipantRole.OWNER) GoldStar else InstaGradientStart,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (participant.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = participant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            // Mute / Owner Badge
            if (participant.isMuted) {
                Surface(
                    shape = CircleShape,
                    color = LiveRed,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(15.dp)
                ) {
                    Icon(Icons.Default.MicOff, contentDescription = "Muted", tint = Color.White, modifier = Modifier.padding(2.dp))
                }
            } else if (participant.role == ParticipantRole.OWNER) {
                Surface(
                    shape = CircleShape,
                    color = GoldStar,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(15.dp)
                ) {
                    Text("👑", fontSize = 8.sp, modifier = Modifier.padding(1.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = participant.name.split(" ").firstOrNull() ?: participant.username,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // VIP or Level Tag
        if (participant.vipTier != VipTier.NONE) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(android.graphics.Color.parseColor(participant.vipTier.badgeColorHex)).copy(alpha = 0.25f)
            ) {
                Text(
                    text = participant.vipTier.title.split(" ").firstOrNull() ?: "VIP",
                    fontSize = 8.sp,
                    color = Color(android.graphics.Color.parseColor(participant.vipTier.badgeColorHex)),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyFallaSeat(
    slotIndex: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Mic, contentDescription = "Take Slot", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isLocked) "Locked" else "${slotIndex + 1}",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// -------------------------------------------------------------
// Falla Gift Shop & Combo System
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FallaGiftShopSheet(
    gifts: List<FallaGift>,
    coinBalance: Long,
    speakers: List<Participant>,
    onDismiss: () -> Unit,
    onSend: (FallaGift, String, String, Int) -> Unit,
    onOpenWallet: () -> Unit
) {
    var selectedCat by remember { mutableStateOf(FallaGiftCategory.POPULAR) }
    var selectedGift by remember { mutableStateOf(gifts.firstOrNull()) }
    var selectedCombo by remember { mutableStateOf(1) }
    var selectedRecipientId by remember { mutableStateOf("host") }

    val filteredGifts = remember(selectedCat, gifts) {
        gifts.filter { it.category == selectedCat }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            // Header: Wallet Coin Balance & Categories
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Falla Gift Shop", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.clickable(onClick = onOpenWallet)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "🪙 %,d".format(coinBalance), fontSize = 11.sp, color = GoldStar, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = GoldStar, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gift Category Tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FallaGiftCategory.entries) { cat ->
                    val isSel = selectedCat == cat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) InstaGradientMiddle else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.clickable { selectedCat = cat }
                    ) {
                        Text(
                            text = cat.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gifts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredGifts, key = { it.id }) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) InstaGradientMiddle.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 0.dp,
                            if (isSelected) GoldStar else Color.Transparent
                        ),
                        modifier = Modifier
                            .clickable { selectedGift = gift }
                            .testTag("gift_${gift.id}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Text(text = gift.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = gift.name, fontSize = 10.sp, color = Color.White, maxLines = 1)
                            Text(text = "🪙 ${gift.coinCost}", fontSize = 10.sp, color = GoldStar, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recipient & Combo Multiplier Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Combo multipliers (x1, x5, x10, x66, x100)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 5, 10, 66).forEach { combo ->
                        val isSel = selectedCombo == combo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) GoldStar else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { selectedCombo = combo }
                        ) {
                            Text(
                                text = "x$combo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Send Button
                Button(
                    onClick = {
                        selectedGift?.let { gift ->
                            onSend(gift, "host_main", "Room Host", selectedCombo)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(InstagramSignatureGradient)
                        .testTag("send_gift_button")
                ) {
                    val totalCost = (selectedGift?.coinCost ?: 10) * selectedCombo
                    Text(
                        text = "Send 🪙 $totalCost",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// In-Room Mini Games (Lucky Dice, Tic-Tac-Toe, Trivia)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InRoomGamesSheet(
    roomId: String,
    repository: AppRepository,
    onDismiss: () -> Unit
) {
    var selectedGame by remember { mutableStateOf(InRoomGameType.LUCKY_DICE) }
    val diceResult by repository.diceGameResult.collectAsState()
    val ticTacToeBoard by repository.ticTacToeBoard.collectAsState()
    val triviaIndex by repository.triviaIndex.collectAsState()
    var betAmount by remember { mutableStateOf(100L) }

    val triviaQuestions = listOf(
        "Which country hosted the first social voice chatroom revolution?" to listOf("UAE", "USA", "Egypt", "Japan") to 0,
        "What is the maximum audio sample rate supported by WebRTC Opus?" to listOf("48 kHz", "24 kHz", "96 kHz", "44.1 kHz") to 0,
        "In Arabic poetry lounges, what instrument is traditionally played?" to listOf("Oud", "Guitar", "Violin", "Flute") to 0,
        "What is the lucky sum in Lucky Dice to double your coins?" to listOf("9 or above", "7 exact", "12 only", "Odd numbers") to 0
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161622)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Games Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InRoomGameType.entries.forEach { game ->
                    val isSel = selectedGame == game
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) DarkAccent else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedGame = game }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(text = game.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = game.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedGame) {
                InRoomGameType.LUCKY_DICE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Roll Dice with Room Members", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Roll doubles or sum ≥ 9 to win 2x coins!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dice Display
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val d1 = diceResult?.first ?: 6
                            val d2 = diceResult?.second ?: 6
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "$d1", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "$d2", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bet chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(50L, 100L, 250L, 500L).forEach { bet ->
                                val isSel = betAmount == bet
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) GoldStar else Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.clickable { betAmount = bet }
                                ) {
                                    Text(
                                        text = "🪙 $bet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { repository.rollDice(roomId, betAmount) },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Roll Lucky Dice 🎲", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                InRoomGameType.TIC_TAC_TOE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Voice Stage Tic-Tac-Toe", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3x3 Grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0..2) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (col in 0..2) {
                                        val idx = row * 3 + col
                                        val cell = ticTacToeBoard.getOrElse(idx) { "" }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.1f),
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clickable { repository.playTicTacToeMove(idx) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = cell,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (cell == "X") DarkAccent else InstaGradientStart
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(onClick = { repository.resetTicTacToe() }) {
                            Text("Reset Board", color = Color.White)
                        }
                    }
                }
                InRoomGameType.TRIVIA_QUIZ -> {
                    val qPair = triviaQuestions[triviaIndex]
                    val question = qPair.first.first
                    val options = qPair.first.second
                    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Live Room Trivia Royale 💡", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = question, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)

                        Spacer(modifier = Modifier.height(12.dp))

                        options.forEachIndexed { i, opt ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedAnswer == i) DarkAccent else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        selectedAnswer = i
                                        if (i == 0) {
                                            repository.topUpCoins(100)
                                            repository.sendInRoomChatMessage(roomId, "💡 Answered trivia correctly & won 100 coins!")
                                        }
                                    }
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = {
                            selectedAnswer = null
                            repository.nextTrivia()
                        }) {
                            Text("Next Question ➡️", color = GoldStar)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Soundboard Effects Sheet
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundboardSheet(
    onPlaySound: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val effects = listOf(
        "Applause & Cheer 👏",
        "Drumroll & Hype 🥁",
        "Celebration Horn 🎺",
        "Laughter & Joy 😂",
        "Royal Fanfare 🌟",
        "Victory Bell 🔔"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161622)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text("In-Room Soundboard", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("Broadcast live sound effects to all speakers & listeners", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(effects) { sound ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.clickable { onPlaySound(sound) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Text(text = sound, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Room Settings Sheet (Full Configuration & Admin Control)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomSettingsSheet(
    room: VoiceRoom,
    onDismiss: () -> Unit,
    onSave: (RoomSettings) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(room.title) }
    var announcement by remember { mutableStateOf(room.settings.announcement) }
    var seatCount by remember { mutableStateOf(room.totalSlots) }
    var selectedCat by remember { mutableStateOf(room.category) }
    var selectedBg by remember { mutableStateOf(room.settings.background) }
    var isPrivate by remember { mutableStateOf(room.settings.isPrivate) }
    var password by remember { mutableStateOf(room.settings.password) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Room Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val updated = room.settings.copy(
                                name = title,
                                announcement = announcement,
                                seatCount = seatCount,
                                category = selectedCat,
                                background = selectedBg,
                                isPrivate = isPrivate,
                                password = password
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkAccent)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Room Title") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text("Room Announcement") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Voice Seat Count:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 6, 8, 9, 12).forEach { seats ->
                        val isSel = seatCount == seats
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) DarkAccent else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { seatCount = seats }
                        ) {
                            Text(
                                text = "$seats Seats",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Room Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RoomCategory.entries) { cat ->
                        val isSel = selectedCat == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) InstaGradientMiddle else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { selectedCat = cat }
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.displayName}",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Room Background Theme:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RoomBackground.entries) { bg ->
                        val isSel = selectedBg == bg
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(android.graphics.Color.parseColor(bg.primaryColorHex)),
                            border = androidx.compose.foundation.BorderStroke(if (isSel) 2.dp else 0.dp, GoldStar),
                            modifier = Modifier.clickable { selectedBg = bg }
                        ) {
                            Text(
                                text = bg.displayName,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Private & Password
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Password Protected Room", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }

                if (isPrivate) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Room Password") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Room Permanently", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Speaker Action Sheet (Gift, Mute, Kick)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeakerActionSheet(
    speaker: Participant,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onSendGift: () -> Unit,
    onMute: () -> Unit,
    onKick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161622)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (speaker.avatarUrl.isNotBlank()) {
                    AsyncImage(model = speaker.avatarUrl, contentDescription = speaker.name, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = speaker.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text(text = "@${speaker.username} • Slot ${speaker.slotIndex + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendGift,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Gift 🎁", color = Color.White)
                }

                if (isOwner) {
                    Button(
                        onClick = onMute,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (speaker.isMuted) "Unmute" else "Mute Mic", color = Color.White)
                    }

                    Button(
                        onClick = onKick,
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kick Seat", color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Moderator Control Panel Sheet (Mute Users, Lock Seats, End Broadcast)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeratorControlPanelSheet(
    room: VoiceRoom,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onEndBroadcastSession: () -> Unit
) {
    var confirmEndBroadcast by remember { mutableStateOf(false) }
    val auditLogs by repository.moderationAuditLog.collectAsState()
    val roomLogs = auditLogs.take(5)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12121A)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = LiveRed.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = LiveRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Room Owner & Moderator View",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${room.title} • ID: ${room.numericRoomId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )
            }

            // Broadcast Quick Controls Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { repository.muteAllRoomParticipants(room.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272736)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mod_mute_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mute All Speakers", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { confirmEndBroadcast = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mod_end_broadcast_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Broadcast", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Confirm End Broadcast Banner
            if (confirmEndBroadcast) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LiveRed.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "End Live Broadcast Session?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "This will immediately disconnect all stage speakers, stop WebRTC audio streaming, and close the chatroom session for all listeners.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Button(
                                    onClick = onEndBroadcastSession,
                                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_end_broadcast_button")
                                ) {
                                    Text("Confirm End Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { confirmEndBroadcast = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Active Stage Speakers Section
            item {
                Text(
                    text = "Stage Speakers (${room.speakers.size}/${room.totalSlots})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            if (room.speakers.isEmpty()) {
                item {
                    Text(
                        text = "No active speakers on stage.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(room.speakers) { speaker ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (speaker.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = speaker.avatarUrl,
                                        contentDescription = speaker.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = speaker.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (speaker.isMuted) LiveRed.copy(alpha = 0.25f) else WebRtcConnectedGreen.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (speaker.isMuted) "MUTED" else "LIVE MIC",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (speaker.isMuted) LiveRed else WebRtcConnectedGreen,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${speaker.username} • Seat #${speaker.slotIndex + 1} (${speaker.role.name})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Mute / Unmute User Button
                            Button(
                                onClick = {
                                    repository.muteRoomParticipant(
                                        roomId = room.id,
                                        participantId = speaker.id,
                                        participantName = speaker.name,
                                        mute = !speaker.isMuted
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (speaker.isMuted) WebRtcConnectedGreen else Color(0xFF3A3A4E)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("mod_mute_user_${speaker.id}")
                            ) {
                                Icon(
                                    imageVector = if (speaker.isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (speaker.isMuted) "Unmute" else "Mute",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Kick to Listener Button
                            IconButton(
                                onClick = {
                                    repository.liveKitManager.kickSpeaker(speaker.id)
                                    repository.logModerationAction(
                                        ModerationAction(
                                            id = "mod_${java.util.UUID.randomUUID()}",
                                            moderatorName = repository.currentUser.value.fullName,
                                            targetUserName = speaker.name,
                                            actionType = "REMOVE_SPEAKER"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LiveRed.copy(alpha = 0.2f))
                                    .testTag("mod_kick_user_${speaker.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = "Kick from stage",
                                    tint = LiveRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Seat Lock Management Row
            item {
                Text(
                    text = "Stage Seat Lock Controls (Tap to Lock/Unlock)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0 until room.totalSlots).toList()) { slotIdx ->
                        val isLocked = room.lockedSlotIndices.contains(slotIdx)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLocked) LiveRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLocked) LiveRed else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.clickable {
                                repository.liveKitManager.toggleSeatLock(slotIdx)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isLocked) "🔒 Seat ${slotIdx + 1}" else "🔓 Seat ${slotIdx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) LiveRed else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Recent Moderation Log
            if (roomLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Moderator Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldStar
                    )
                }
                items(roomLogs) { log ->
                    Text(
                        text = "• [${log.actionType}] ${log.targetUserName} (by ${log.moderatorName})",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Coin Wallet Sheet & Top Up
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinWalletSheet(
    coinBalance: Long,
    transactions: List<GiftTransaction>,
    onDismiss: () -> Unit,
    onTopUp: (Long) -> Unit
) {
    var selectedPackage by remember { mutableStateOf(5000L) }
    val packages = listOf(
        1000L to "$0.99",
        5000L to "$4.99",
        15000L to "$12.99",
        50000L to "$39.99"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141420)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Falla Coin Wallet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldStar.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "🪙 %,d Coins".format(coinBalance),
                        fontWeight = FontWeight.Bold,
                        color = GoldStar,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Instant Coin Top Up:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                packages.forEach { (coins, price) ->
                    val isSel = selectedPackage == coins
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) GoldStar else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPackage = coins }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Text(text = "%,d".format(coins), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            Text(text = price, fontSize = 10.sp, color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onTopUp(selectedPackage) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldStar),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recharge %,d Coins".format(selectedPackage), color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recent In-Room Gift Transactions (${transactions.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (transactions.isEmpty()) {
                    item {
                        Text("No gift transactions yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(transactions) { tx ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Text(text = tx.gift.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${tx.gift.name} x${tx.quantity} to ${tx.receiverName}",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "-${tx.totalCoins} 🪙", fontSize = 11.sp, color = GoldStar, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Rankings Leaderboard Sheet (Daily, Weekly, Monthly)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomRankingsSheet(
    onDismiss: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(RoomRankingPeriod.DAILY) }

    val mockRankings = emptyList<RoomRankingEntry>()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆 Falla Room Rankings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Period Switcher: Daily | Weekly | Monthly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoomRankingPeriod.entries.forEach { p ->
                    val isSel = selectedPeriod == p
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) GoldStar else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPeriod = p }
                    ) {
                        Text(
                            text = p.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else Color.White,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No rankings yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Send gifts and host voice rooms to appear here!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Create Room Modal Dialog
// -------------------------------------------------------------

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, RoomCategory, Int, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(RoomCategory.TRENDING) }
    var seatCount by remember { mutableStateOf(8) }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Falla Voice Room 🎙️", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Room Title") },
                    placeholder = { Text("e.g. Arabian Poetry & Music Lounge 🌙") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Seat Capacity:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 6, 8, 9, 12).forEach { seats ->
                        val isSel = seatCount == seats
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) DarkAccent else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { seatCount = seats }
                        ) {
                            Text(
                                text = "$seats",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RoomCategory.entries) { cat ->
                        val isSel = selectedCat == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) InstaGradientMiddle else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { selectedCat = cat }
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.displayName}",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "Social Voice Lounge 🎙️" }
                    onCreate(finalTitle, selectedCat, seatCount, isPrivate, password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkAccent)
            ) {
                Text("Start Room", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------
// Room Invitation Dialog
// -------------------------------------------------------------

@Composable
private fun RoomInviteDialog(
    room: VoiceRoom,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite to Voice Room", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Share this Room ID or link with friends to drop in directly:", fontSize = 13.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Room ID: ${room.numericRoomId}", fontWeight = FontWeight.Bold, color = GoldStar, fontSize = 14.sp)
                        Text("noice.live/room/${room.numericRoomId}", color = DarkAccent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString("Join my voice room on NOICE LIVE! Room ID: ${room.numericRoomId}"))
                    copied = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (copied) WebRtcConnectedGreen else DarkAccent)
            ) {
                Text(if (copied) "Copied Link! ✅" else "Copy Invitation", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun MinimizedAudioBar(
    room: VoiceRoom,
    isMuted: Boolean,
    onOpenSheet: () -> Unit,
    onLeave: () -> Unit
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSheet)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(WebRtcConnectedGreen)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${room.title} (ID: ${room.numericRoomId})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "LiveKit WebRTC Low-Latency Mesh • Tap for stage & gifts",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onLeave) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Leave",
                    tint = Color.White
                )
            }
        }
    }
}
