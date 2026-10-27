package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.FeedFilter
import com.example.model.Post
import com.example.model.Story
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.PostCard
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.StoryViewerDialog
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstagramSignatureGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: AppRepository,
    onNavigateToMessages: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val posts by repository.displayedFeed.collectAsState()
    val rawStories by repository.stories.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val selectedFilter by repository.selectedFeedFilter.collectAsState()
    val commentsMap by repository.comments.collectAsState()

    // 24-hour automatic expiration filter
    val activeStories = remember(rawStories) {
        rawStories.filter { !it.isExpired }
    }

    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }
    var commentPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // NOICE LIVE gradient typography logo
                    Text(
                        text = "NOICE LIVE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            brush = InstagramSignatureGradient
                        ),
                        modifier = Modifier.testTag("app_logo")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Google Sign-In / Switch Account Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32323E)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onOpenGoogleSignIn() }
                            .testTag("home_google_sign_in_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Activity / Notifications Heart Icon
                    IconButton(
                        onClick = onNavigateToActivity,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }

                    // Direct Messages with unread badge
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = onNavigateToMessages,
                            modifier = Modifier.testTag("home_direct_messages_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Direct Messages",
                                tint = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            // Horizontal Stories Bar
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // "Your Story" button with (+) badge
                    item {
                        StoryAvatarRing(
                            avatarUrl = currentUser.avatarUrl,
                            username = "Your story",
                            isSeen = false,
                            hasStory = false,
                            isAddStory = true,
                            onClick = onNavigateToCreate
                        )
                    }

                    // Authentic user stories with gradient rings & timestamps (within 24h)
                    itemsIndexed(activeStories) { index, story ->
                        StoryAvatarRing(
                            avatarUrl = story.userAvatar,
                            username = story.username,
                            timestamp = story.formattedTimestamp,
                            isSeen = story.isSeen,
                            hasStory = true,
                            isAddStory = false,
                            onClick = { activeStoryIndex = index }
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }

            // Authentic User Feed or Clean Instagram Empty State
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Welcome to NOICE LIVE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "When you share photos and stories, they'll appear here on your feed.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Instagram signature gradient button
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(InstagramSignatureGradient)
                                    .testTag("empty_feed_share_button")
                            ) {
                                Text(
                                    text = "Share your first photo",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { repository.toggleLike(post.id) },
                        onCommentClick = { commentPostId = post.id },
                        onBookmarkClick = { repository.toggleBookmark(post.id) },
                        onShareClick = { /* Share */ },
                        onAuthorClick = {
                            if (post.userId == currentUser.id) onNavigateToProfile()
                        }
                    )
                }
            }
        }
    }

    // Story Viewer Modal
    activeStoryIndex?.let { index ->
        StoryViewerDialog(
            stories = activeStories,
            initialIndex = index,
            onDismiss = { activeStoryIndex = null },
            onReply = { story, reply ->
                val conversationId = "conv_${story.username.replace(".", "_")}"
                repository.sendTextMessage(conversationId, "Replied to story: $reply")
            }
        )
    }

    // Comments Bottom Sheet
    commentPostId?.let { postId ->
        val postComments = commentsMap[postId] ?: emptyList()
        CommentsBottomSheet(
            comments = postComments,
            currentUserAvatar = currentUser.avatarUrl,
            onDismiss = { commentPostId = null },
            onAddComment = { text ->
                repository.addComment(postId, text)
            }
        )
    }
}
