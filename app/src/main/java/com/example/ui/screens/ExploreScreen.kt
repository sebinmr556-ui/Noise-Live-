package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.Post
import com.example.ui.components.PhotoFilterHelper
import com.example.ui.components.PostCard
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstagramSignatureGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("All") }
    var exploreMode by remember { mutableStateOf(0) } // 0 = Photos, 1 = Voice Rooms, 2 = Live Video
    var previewPost by remember { mutableStateOf<Post?>(null) }

    val allPosts by repository.displayedFeed.collectAsState()

    val topics = listOf("All", "📸 Photos", "🎙️ Spaces", "🎥 Live", "🎨 Art", "🚀 Tech")

    val matchedUser = remember(searchQuery) {
        if (searchQuery.isNotBlank()) repository.findUserByNumericId(searchQuery.trim()) else null
    }

    val filteredPosts = remember(searchQuery, selectedTopic, allPosts) {
        allPosts.filter { post ->
            val matchesSearch = searchQuery.isBlank() ||
                post.caption.contains(searchQuery, ignoreCase = true) ||
                post.username.contains(searchQuery, ignoreCase = true) ||
                post.tags.any { it.contains(searchQuery, ignoreCase = true) }

            matchesSearch
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Search Field in Instagram Dark Mode
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search creators, hashtags, spaces...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White
                                )
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
                        .testTag("explore_search_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode switch pills (Photos | Voice Rooms | Live Streams)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Photos", "🎙️ Voice Rooms", "🔴 Live Video").forEachIndexed { index, title ->
                        val isSelected = exploreMode == index
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DarkAccent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { exploreMode = index }
                                .testTag("explore_mode_$index")
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (exploreMode) {
                1 -> {
                    // Voice Rooms
                    VoiceRoomsScreen(repository = repository)
                }
                2 -> {
                    // Live Video
                    LiveStreamScreen(repository = repository)
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (matchedUser != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E1E26),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3897F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("matched_user_card")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF3897F0)), contentAlignment = Alignment.Center) {
                                        Text(matchedUser.fullName.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(matchedUser.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            if (matchedUser.isVipActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("👑VIP${matchedUser.vipTierLevel}", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("ID: ${matchedUser.numericId} • @${matchedUser.username}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text("Lv.${matchedUser.userLevel} • Wealth Lv.${matchedUser.wealthLevel} • Charm Lv.${matchedUser.charmLevel}", fontSize = 10.sp, color = Color(0xFF3897F0))
                                    }
                                    Button(
                                        onClick = { repository.toggleFollow(matchedUser.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (filteredPosts.isEmpty() && matchedUser == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Discover Creators & Spaces",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Search by exact 7-digit User ID (e.g. 8294102), hashtags, or creators.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { previewPost = post }
                                        .testTag("explore_grid_item_${post.id}")
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
                        }
                    }
                }
            }
        }
    }

    // Quick Post Preview Modal
    previewPost?.let { post ->
        Dialog(onDismissRequest = { previewPost = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                PostCard(
                    post = post,
                    onLikeClick = { repository.toggleLike(post.id) },
                    onCommentClick = { previewPost = null },
                    onBookmarkClick = { repository.toggleBookmark(post.id) },
                    onShareClick = { /* Share */ },
                    onAuthorClick = { previewPost = null }
                )
            }
        }
    }
}
}
