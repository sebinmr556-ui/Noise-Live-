package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
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
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.PhotoFilter
import com.example.ui.components.PhotoFilterHelper
import com.example.ui.theme.DarkAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    repository: AppRepository,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Preset gallery images
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1080&q=80", // Tropical beach
        "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=1080&q=80", // Chicago skyline
        "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=1080&q=80", // Lake Bled
        "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=1080&q=80", // Portrait bokeh
        "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=1080&q=80"  // Cat golden light
    )

    var selectedImageUrl by remember { mutableStateOf(presetImages[0]) }
    var selectedFilter by remember { mutableStateOf(PhotoFilter.CLARENDON) }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("San Francisco, CA") }
    var isStoryTarget by remember { mutableStateOf(false) }

    // Android Zero-permission Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUrl = it.toString()
        }
    }

    val availableTags = listOf("photography", "sunset", "travel", "art", "vibes", "instawave")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isStoryTarget) "New Story" else "New Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPostCreated) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (!repository.isUserAuthenticated.value) {
                                repository.triggerAuthPrompt(if (isStoryTarget) "post a story" else "publish a post")
                                return@Button
                            }

                            val tagsList = availableTags.filter { caption.contains("#$it", ignoreCase = true) }
                            val finalTags = if (tagsList.isEmpty()) listOf("photography", "authentic") else tagsList

                            if (isStoryTarget) {
                                repository.createStory(selectedImageUrl)
                            } else {
                                repository.createPost(
                                    imageUrl = selectedImageUrl,
                                    caption = caption.ifBlank { "Moments captured ✨" },
                                    location = location,
                                    tags = finalTags
                                )
                            }
                            onPostCreated()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(com.example.ui.theme.InstagramSignatureGradient)
                            .testTag("publish_post_button")
                    ) {
                        Text(
                            text = "Share",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Photo Preview with Filter Applied
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Selected Photo",
                    contentScale = ContentScale.Crop,
                    colorFilter = PhotoFilterHelper.getColorFilter(selectedFilter),
                    modifier = Modifier.fillMaxSize()
                )

                // Photo picker button overlay
                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .testTag("pick_from_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Photo from Device"
                    )
                }
            }

            // Quick Preset Selection Row
            Text(
                text = "Choose Photo:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 10.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetImages) { url ->
                    val isSelected = selectedImageUrl == url
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) DarkAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedImageUrl = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Interactive Photo Filter Selector
            Text(
                text = "Photo Filters:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PhotoFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedFilter = filter }
                            .testTag("filter_chip_${filter.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) DarkAccent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            AsyncImage(
                                model = selectedImageUrl,
                                contentDescription = filter.displayName,
                                contentScale = ContentScale.Crop,
                                colorFilter = PhotoFilterHelper.getColorFilter(filter),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = filter.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkAccent else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Caption Input
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Write a caption...") },
                placeholder = { Text("Share the story behind this photo... #photography") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("post_caption_input"),
                minLines = 3
            )

            // Suggested Hashtag Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableTags) { tag ->
                    SuggestionChip(
                        onClick = {
                            caption = if (caption.endsWith(" ") || caption.isEmpty()) "$caption#$tag " else "$caption #$tag "
                        },
                        label = { Text("#$tag") }
                    )
                }
            }

            // Location Input
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Add Location") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = DarkAccent)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                singleLine = true
            )

            // Destination Selector: Feed vs Story
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isStoryTarget) "Publishing to: Your Story" else "Publishing to: Feed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isStoryTarget) "Visible for 24h at top of feed" else "Permanent post on your profile & feed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isStoryTarget,
                        onCheckedChange = { isStoryTarget = it },
                        modifier = Modifier.testTag("story_target_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
