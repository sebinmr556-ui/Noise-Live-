package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PermContactCalendar
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.ProfileAccentColor
import com.example.model.UserProfile
import com.example.ui.components.*
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.StoryGradient

enum class ProfileTab {
    POSTS,
    REELS,
    SAVED,
    TAGGED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: AppRepository,
    onOpenGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val allPosts by repository.displayedFeed.collectAsState()
    val highlights by repository.highlights.collectAsState()
    val bookmarkedIds by repository.userBookmarkedPostIds.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showVipSheet by remember { mutableStateOf(false) }
    var showProgressionSheet by remember { mutableStateOf(false) }
    var showRankingsSheet by remember { mutableStateOf(false) }
    var showAdminSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showLoginModal by remember { mutableStateOf(false) }

    val userPosts = remember(allPosts, currentUser.id) {
        allPosts.filter { it.userId == currentUser.id || it.userId == "user_me" }
    }

    val savedPosts = remember(allPosts, bookmarkedIds) {
        allPosts.filter { bookmarkedIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (currentUser.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = DarkAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenGoogleSignIn,
                        modifier = Modifier.testTag("profile_google_sign_in_button")
                    ) {
                        Text(
                            text = "Google Account",
                            color = DarkAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Options")
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
            // Accent Color Top Strip
            item {
                val bannerColor = try {
                    Color(android.graphics.Color.parseColor(currentUser.bannerAccentColorHex))
                } catch (e: Exception) {
                    DarkAccent
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(bannerColor, bannerColor.copy(alpha = 0.5f))
                            )
                        )
                )
            }

            // Profile Header: Avatar & Stats
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Avatar with gradient border
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, StoryGradient, CircleShape)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentUser.avatarUrl,
                                contentDescription = currentUser.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Stats: Posts, Followers, Following
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(count = userPosts.size.toString(), label = "Posts")
                        ProfileStatItem(count = "${(currentUser.followersCount / 1000f).let { "%.1f".format(it) }}k", label = "Followers")
                        ProfileStatItem(count = currentUser.followingCount.toString(), label = "Following")
                    }
                }
            }

            // Bio & Details
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (currentUser.pronouns.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = currentUser.pronouns,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Permanent 7-digit numeric User ID with copy button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "ID: ${currentUser.numericId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy User ID",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(currentUser.numericId))
                                }
                        )
                    }

                    // 3-Pillar Level Badges + VIP Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (currentUser.isVipActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
                                modifier = Modifier.clickable { showVipSheet = true }
                            ) {
                                Text(
                                    text = "👑 VIP ${currentUser.vipTierLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFA500).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA500)),
                            modifier = Modifier.clickable { showProgressionSheet = true }
                        ) {
                            Text(
                                text = "💰 Wealth Lv.${currentUser.wealthLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA500),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF4081).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4081)),
                            modifier = Modifier.clickable { showProgressionSheet = true }
                        ) {
                            Text(
                                text = "💖 Charm Lv.${currentUser.charmLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF4081),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3897F0).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3897F0)),
                            modifier = Modifier.clickable { showProgressionSheet = true }
                        ) {
                            Text(
                                text = "⭐ Lv.${currentUser.userLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3897F0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkAccent.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = currentUser.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = currentUser.bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 18.sp
                    )

                    if (currentUser.website.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = DarkAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser.website.replace("https://", ""),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Economy Quick Hub Row (Wallet, VIP Center, Progression, Rankings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A)),
                            modifier = Modifier.weight(1f).clickable { showWalletSheet = true }.testTag("profile_quick_wallet")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text("🪙 Wallet", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("%,d".format(currentUser.coinBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A)),
                            modifier = Modifier.weight(1f).clickable { showVipSheet = true }.testTag("profile_quick_vip")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text("👑 VIP", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(if (currentUser.isVipActive) "Tier ${currentUser.vipTierLevel}" else "Join", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF69B4))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A)),
                            modifier = Modifier.weight(1f).clickable { showProgressionSheet = true }.testTag("profile_quick_progression")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text("⭐ Levels", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("Lv.${currentUser.userLevel}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3897F0))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E26),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A4A)),
                            modifier = Modifier.weight(1f).clickable { showRankingsSheet = true }.testTag("profile_quick_rankings")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text("🏆 Hall", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("Ranks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: Edit Profile, Share Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showEditProfileSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button")
                        ) {
                            Text("Edit profile", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("https://instawave.social/u/${currentUser.username}"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share profile", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Story Highlights Row
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Story Highlights",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(highlights) { highlight ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .padding(2.5.dp)
                                ) {
                                    AsyncImage(
                                        model = highlight.coverUrl,
                                        contentDescription = highlight.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = highlight.title,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Add new highlight
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Highlight",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "New",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Profile Tabs: Posts | Reels | Saved | Tagged
            item {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = DarkAccent,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) }
                ) {
                    Tab(
                        selected = selectedTab == ProfileTab.POSTS,
                        onClick = { selectedTab = ProfileTab.POSTS },
                        icon = { Icon(Icons.Outlined.GridOn, contentDescription = "Posts") }
                    )
                    Tab(
                        selected = selectedTab == ProfileTab.REELS,
                        onClick = { selectedTab = ProfileTab.REELS },
                        icon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = "Reels") }
                    )
                    Tab(
                        selected = selectedTab == ProfileTab.SAVED,
                        onClick = { selectedTab = ProfileTab.SAVED },
                        icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved") }
                    )
                    Tab(
                        selected = selectedTab == ProfileTab.TAGGED,
                        onClick = { selectedTab = ProfileTab.TAGGED },
                        icon = { Icon(Icons.Outlined.PermContactCalendar, contentDescription = "Tagged") }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                ProfileTab.POSTS -> {
                    if (userPosts.isEmpty()) {
                        item {
                            EmptyTabPlaceholder(title = "Share Photos", subtitle = "When you share photos, they'll appear on your profile.")
                        }
                    } else {
                        item {
                            ProfileGrid(posts = userPosts)
                        }
                    }
                }
                ProfileTab.SAVED -> {
                    if (savedPosts.isEmpty()) {
                        item {
                            EmptyTabPlaceholder(title = "Save Photos and Videos", subtitle = "Save items that you want to see again. No one is notified.")
                        }
                    } else {
                        item {
                            ProfileGrid(posts = savedPosts)
                        }
                    }
                }
                ProfileTab.REELS -> {
                    item {
                        EmptyTabPlaceholder(title = "Replays & Live Spaces", subtitle = "Your saved audio spaces and live video streams will appear here.")
                    }
                }
                ProfileTab.TAGGED -> {
                    item {
                        EmptyTabPlaceholder(title = "Photos of You", subtitle = "When people tag you in photos, they'll appear here.")
                    }
                }
            }
        }
    }

    // Edit Profile Bottom Sheet
    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            currentUser = currentUser,
            onDismiss = { showEditProfileSheet = false },
            onSave = { name, username, bio, website, _, _, colorHex, avatar ->
                repository.updateProfile(
                    username = username,
                    fullName = name,
                    bio = bio,
                    website = website,
                    avatarUrl = avatar ?: "",
                    bannerAccentHex = colorHex
                )
                showEditProfileSheet = false
            }
        )
    }

    // Virtual Coin Wallet & Purchase Center Sheet
    if (showWalletSheet) {
        WalletBottomSheet(
            repository = repository,
            onDismiss = { showWalletSheet = false }
        )
    }

    // VIP 1-10 Membership Center Sheet
    if (showVipSheet) {
        VipCenterBottomSheet(
            repository = repository,
            onDismiss = { showVipSheet = false }
        )
    }

    // 3-Pillar Level & Achievement Center Sheet
    if (showProgressionSheet) {
        ProgressionCenterBottomSheet(
            repository = repository,
            onDismiss = { showProgressionSheet = false }
        )
    }

    // Global Hall of Fame / Leaderboards Sheet
    if (showRankingsSheet) {
        GlobalRankingsBottomSheet(
            repository = repository,
            onDismiss = { showRankingsSheet = false }
        )
    }

    // Super Admin Dashboard Sheet
    if (showAdminSheet) {
        SuperAdminDashboardBottomSheet(
            repository = repository,
            onDismiss = { showAdminSheet = false }
        )
    }

    // Options Menu Sheet
    if (showOptionsMenu) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsMenu = false },
            containerColor = Color(0xFF141418),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Settings & Features",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Menu items
                ProfileMenuItem(
                    icon = "🪙",
                    title = "Coin Wallet & Buy Coins",
                    subtitle = "Balance: %,d Coins".format(currentUser.coinBalance),
                    onClick = {
                        showOptionsMenu = false
                        showWalletSheet = true
                    }
                )

                ProfileMenuItem(
                    icon = "👑",
                    title = "VIP Center (VIP 1 - 10)",
                    subtitle = if (currentUser.isVipActive) "Active: VIP ${currentUser.vipTierLevel}" else "Join VIP for exclusive perks",
                    onClick = {
                        showOptionsMenu = false
                        showVipSheet = true
                    }
                )

                ProfileMenuItem(
                    icon = "⭐",
                    title = "Levels & Progression",
                    subtitle = "User Lv.${currentUser.userLevel} • Wealth Lv.${currentUser.wealthLevel} • Charm Lv.${currentUser.charmLevel}",
                    onClick = {
                        showOptionsMenu = false
                        showProgressionSheet = true
                    }
                )

                ProfileMenuItem(
                    icon = "🏆",
                    title = "Global Leaderboard",
                    subtitle = "Wealth, Charm, and User Level Rankings",
                    onClick = {
                        showOptionsMenu = false
                        showRankingsSheet = true
                    }
                )

                ProfileMenuItem(
                    icon = "✏️",
                    title = "Edit Profile & Theme",
                    subtitle = "Update photo, bio, links, and accent banner",
                    onClick = {
                        showOptionsMenu = false
                        showEditProfileSheet = true
                    }
                )

                if (currentUser.isSuperAdmin || currentUser.isAdmin) {
                    ProfileMenuItem(
                        icon = "⚙️",
                        title = "Super Admin Dashboard",
                        subtitle = "User lookup, audited coin adjustments & audit logs",
                        onClick = {
                            showOptionsMenu = false
                            showAdminSheet = true
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showOptionsMenu = false
                            onOpenGoogleSignIn()
                        }
                        .padding(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = Color(0xFF3897F0), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Switch / Sign In with Google", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3897F0))
                }

                if (currentUser.isAuthenticated) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                repository.signOut()
                                showOptionsMenu = false
                                onOpenGoogleSignIn()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    }
                }
            }
        }
    }

    if (showLoginModal) {
        LoginRequiredDialog(
            onDismiss = { showLoginModal = false },
            onContinueWithGoogle = {
                // Trigger Google Sign-In with Credential Manager
                showLoginModal = false
                repository.triggerAuthPrompt("profile")
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1B1B22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282834)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileGrid(posts: List<com.example.model.Post>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val rows = posts.chunked(3)
        rows.forEach { rowPosts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rowPosts.forEach { post ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = post.caption,
                            contentScale = ContentScale.Crop,
                            colorFilter = PhotoFilterHelper.getColorFilter(post.filterApplied),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                // Fill remainder of row if less than 3
                for (i in 0 until (3 - rowPosts.size)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyTabPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(currentUser.fullName) }
    var username by remember { mutableStateOf(currentUser.username) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var website by remember { mutableStateOf(currentUser.website) }
    var pronouns by remember { mutableStateOf(currentUser.pronouns) }
    var category by remember { mutableStateOf(currentUser.category) }
    var selectedColorHex by remember { mutableStateOf(currentUser.bannerAccentColorHex) }
    var selectedAvatar by remember { mutableStateOf(currentUser.avatarUrl) }

    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("edit_profile_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            onSave(name, username, bio, website, pronouns, category, selectedColorHex, selectedAvatar)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Avatar selection
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(2.dp, DarkAccent, CircleShape)
                            .padding(2.dp)
                    ) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Avatar Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Choose Avatar Preset:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetAvatars) { avatarUrl ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (selectedAvatar == avatarUrl) 2.dp else 0.dp,
                                        color = if (selectedAvatar == avatarUrl) DarkAccent else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatar = avatarUrl }
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Profile Accent Theme Colors
            item {
                Text("Profile Banner Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProfileAccentColor.entries) { colorItem ->
                        val isSel = selectedColorHex == colorItem.hex
                        val parsedColor = Color(android.graphics.Color.parseColor(colorItem.hex))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedColorHex = colorItem.hex }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isSel) 3.dp else 0.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = colorItem.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Input Fields
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("edit_name_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("edit_username_input")
                )

                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it },
                    label = { Text("Pronouns (e.g. she/her, they/them)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Digital Creator, Musician)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("edit_bio_input"),
                    minLines = 3
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Links / Website") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
