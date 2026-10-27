package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.LevelCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionCenterBottomSheet(
    repository: AppRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    var selectedPillar by remember { mutableStateOf(0) } // 0 = User Level, 1 = Wealth, 2 = Charm

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141418),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxHeight(0.9f).testTag("progression_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "⭐ Level & Achievement Center",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Three distinct progression pillars tracking your activity, wealth, and popularity.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pillar selector tabs
            TabRow(
                selectedTabIndex = selectedPillar,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                listOf("⭐ User Level", "💰 Wealth Level", "💖 Charm Level").forEachIndexed { idx, label ->
                    val isSelected = selectedPillar == idx
                    val tint = when (idx) {
                        0 -> Color(0xFF3897F0)
                        1 -> Color(0xFFFFA500)
                        else -> Color(0xFFFF4081)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) tint.copy(alpha = 0.2f) else Color(0xFF1F1F24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) tint else Color.Transparent),
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { selectedPillar = idx }
                            .testTag("pillar_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) tint else Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedPillar) {
                    0 -> {
                        // USER LEVEL (XP)
                        val nextXp = LevelCalculator.xpForNextLevel(currentUser.userLevel)
                        val progress = (currentUser.userXp.toFloat() / nextXp.toFloat()).coerceIn(0f, 1f)

                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1B2230),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E5E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⭐ User Level", fontSize = 13.sp, color = Color(0xFF90CAF9))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Level ${currentUser.userLevel}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = Color(0xFF3897F0),
                                        trackColor = Color(0xFF141924),
                                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("%,d XP".format(currentUser.userXp), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                        Text("Next Level: %,d XP".format(nextXp), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        item {
                            Text("Daily Activity Missions:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        item {
                            MissionItem(title = "Daily App Check-In", reward = "+100 XP", isDone = true, onClaim = {})
                        }
                        item {
                            MissionItem(title = "Share a Photo Post or Story", reward = "+150 XP", isDone = false, onClaim = { repository.awardUserXp(150, "Shared post") })
                        }
                        item {
                            MissionItem(title = "Join a Voice Stage for 10m", reward = "+200 XP", isDone = false, onClaim = { repository.awardUserXp(200, "Voice stage session") })
                        }
                        item {
                            MissionItem(title = "Send 5 In-Room Chat Messages", reward = "+120 XP", isDone = false, onClaim = { repository.awardUserXp(120, "In-room chat participation") })
                        }
                    }

                    1 -> {
                        // WEALTH LEVEL (Coin Spending)
                        val nextPts = LevelCalculator.wealthPointsForNextLevel(currentUser.wealthLevel)
                        val progress = (currentUser.wealthPoints.toFloat() / nextPts.toFloat()).coerceIn(0f, 1f)

                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF282012),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B4D1A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💰 Wealth Level", fontSize = 13.sp, color = Color(0xFFFFD54F))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Wealth Lv.${currentUser.wealthLevel}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFC107))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = Color(0xFFFFA000),
                                        trackColor = Color(0xFF1E170A),
                                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("%,d Spending Pts".format(currentUser.wealthPoints), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                        Text("Next Tier: %,d Pts".format(nextPts), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1A1A20),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("How Wealth Level Works:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• 1 Coin spent on gifts, games, or VIP = 1 Wealth Point\n• Verified coin packages directly boost your lifetime wealth score\n• Unlocks prestigious golden ring badges in voice chatrooms", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 18.sp)
                                }
                            }
                        }
                    }

                    2 -> {
                        // CHARM LEVEL (Gifts Received)
                        val nextPts = LevelCalculator.charmPointsForNextLevel(currentUser.charmLevel)
                        val progress = (currentUser.charmPoints.toFloat() / nextPts.toFloat()).coerceIn(0f, 1f)

                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF2C1420),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF70284A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💖 Charm Level", fontSize = 13.sp, color = Color(0xFFFF80AB))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Charm Lv.${currentUser.charmLevel}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4081))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = Color(0xFFFF4081),
                                        trackColor = Color(0xFF1F0D16),
                                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("%,d Charm Pts".format(currentUser.charmPoints), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                        Text("Next Tier: %,d Pts".format(nextPts), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1A1A20),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("How Charm Level Works:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("• Receive Falla gifts while hosting or speaking on stage\n• Higher Charm levels qualify you for top weekly charm rankings\n• Displays exclusive pink star halo on your chatroom avatar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionItem(
    title: String,
    reward: String,
    isDone: Boolean,
    onClaim: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1B1B20),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282830)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Color(0xFF2E7D32) else Color(0xFF262632)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(reward, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3897F0))
            }
            if (!isDone) {
                Button(
                    onClick = onClaim,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3897F0)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Done", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }
    }
}
