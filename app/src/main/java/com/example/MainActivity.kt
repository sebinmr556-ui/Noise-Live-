package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppRepository
import com.example.model.UserProfile
import com.example.ui.components.NewUserBadgePill
import com.example.ui.screens.RegistrationScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AppRepository.getInstance(applicationContext)

        setContent {
            NoiceLiveTheme {
                val currentUser by repository.currentUser.collectAsState()
                var isEditingProfile by remember { mutableStateOf(false) }

                val shouldShowRegistration = !currentUser.profileSetupCompleted || isEditingProfile

                AnimatedContent(
                    targetState = shouldShowRegistration,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "RegistrationScreenTransition"
                ) { showRegistration ->
                    if (showRegistration) {
                        RegistrationScreen(
                            repository = repository,
                            onRegistrationComplete = {
                                isEditingProfile = false
                            }
                        )
                    } else {
                        HomeScreen(
                            userProfile = currentUser,
                            onEditProfileClick = {
                                isEditingProfile = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = LuxDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NOICE LIVE",
                            fontWeight = FontWeight.ExtraBold,
                            color = LuxGold,
                            letterSpacing = 1.sp
                        )
                        if (userProfile.isNewUser) {
                            NewUserBadgePill(compact = true)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LuxDarkBackground
                ),
                actions = {
                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier.testTag("home_edit_profile_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Profile Luxury Card
            Surface(
                color = LuxCardBackground,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(LuxNeonPurple.copy(alpha = 0.8f), LuxGold.copy(alpha = 0.8f))
                    )
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(listOf(LuxGold, LuxNeonPurple, LuxGold)),
                                    CircleShape
                                )
                                .background(LuxSurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = userProfile.avatarUrl,
                                    contentDescription = userProfile.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }

                        // Camera icon badge indicating photo was captured
                        Surface(
                            color = LuxGold,
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, Color.Black),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onEditProfileClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = userProfile.displayName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (userProfile.isNewUser) {
                            NewUserBadgePill(compact = true)
                        }
                    }

                    Text(
                        text = "@${userProfile.username.ifBlank { "member" }}",
                        color = LuxGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (userProfile.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = userProfile.bio,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuxSurfaceDark, RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(title = "VIP Tier", value = userProfile.vipTier.title)
                        ProfileStatItem(title = "Coins", value = "🪙 ${userProfile.coins}")
                        ProfileStatItem(title = "Followers", value = "${userProfile.followersCount}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Retake Camera Photo / Edit Profile Action Button
            OutlinedButton(
                onClick = onEditProfileClick,
                border = BorderStroke(1.dp, LuxGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = LuxGoldBright
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("retake_photo_edit_profile_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Update Photo with Camera / Edit Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}
