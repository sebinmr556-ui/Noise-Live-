package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstagramSignatureGradient

@Composable
fun StoryAvatarRing(
    avatarUrl: String,
    username: String,
    timestamp: String? = null,
    isSeen: Boolean = false,
    hasStory: Boolean = true,
    isAddStory: Boolean = false,
    size: Dp = 68.dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("story_avatar_${username}")
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            // Instagram Gradient Ring Border (#f09433 via #dc2743 to #bc1888)
            val ringModifier = if (isAddStory || !hasStory) {
                Modifier
                    .size(size)
                    .clip(CircleShape)
            } else if (!isSeen) {
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.5.dp, InstagramSignatureGradient, CircleShape)
            } else {
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            }

            Box(
                modifier = ringModifier.padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "$username avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            contentDescription = username,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size((size.value * 0.45f).dp)
                        )
                    }
                }
            }

            // Plus icon badge for Add Story
            if (isAddStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(DarkAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = username,
            fontSize = 11.sp,
            fontWeight = if (!isSeen && hasStory) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = size + 10.dp)
        )

        if (timestamp != null) {
            Text(
                text = timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
