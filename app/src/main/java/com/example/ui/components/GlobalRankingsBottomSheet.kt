package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import coil.compose.AsyncImage
import com.example.data.AppRepository

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val numericId: String,
    val username: String,
    val avatarUrl: String,
    val score: Long,
    val badgeLabel: String,
    val vipLevel: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalRankingsBottomSheet(
    repository: AppRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) } // 0 = Wealth, 1 = Charm, 2 = User Level
    var selectedTimeframe by remember { mutableStateOf(1) } // 0 = Daily, 1 = Weekly, 2 = All-Time

    // Authentic leaderboard entries (no dummy or fake accounts)
    val entries = remember(selectedCategory, selectedTimeframe, currentUser) {
        if (!currentUser.isAuthenticated) {
            emptyList<LeaderboardEntry>()
        } else {
            val score = when (selectedCategory) {
                0 -> currentUser.wealthPoints
                1 -> currentUser.charmPoints
                else -> currentUser.userXp
            }
            if (score > 0L) {
                val label = when (selectedCategory) {
                    0 -> "Lv.${currentUser.wealthLevel} Wealth"
                    1 -> "Lv.${currentUser.charmLevel} Charm"
                    else -> "Level ${currentUser.userLevel}"
                }
                listOf(
                    LeaderboardEntry(
                        rank = 1,
                        userId = currentUser.id,
                        numericId = currentUser.numericId,
                        username = currentUser.username,
                        avatarUrl = currentUser.avatarUrl,
                        score = score,
                        badgeLabel = label,
                        vipLevel = currentUser.vipTierLevel
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131317),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxHeight(0.92f).testTag("rankings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "🏆 Global Hall of Fame",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category selector tabs (Wealth, Charm, User Level)
            TabRow(
                selectedTabIndex = selectedCategory,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                listOf("💰 Wealth Rankings", "💖 Charm Rankings", "⭐ Level Rankings").forEachIndexed { idx, label ->
                    val isSelected = selectedCategory == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF282834) else Color(0xFF191920),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF4C4C62) else Color.Transparent),
                        modifier = Modifier
                            .padding(3.dp)
                            .clickable { selectedCategory = idx }
                            .testTag("ranking_cat_$idx")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeframe selector (Daily, Weekly, All-Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Daily", "Weekly", "All-Time").forEachIndexed { idx, label ->
                    val isSelected = selectedTimeframe == idx
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFE1306C).copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1306C)) else null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { selectedTimeframe = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFE1306C) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Podium for Top 3
            if (entries.size >= 3) {
                val first = entries[0]
                val second = entries[1]
                val third = entries[2]

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place (Silver)
                    PodiumPillar(entry = second, place = 2, crownColor = Color(0xFFC0C0C0), pillarHeight = 85.dp)
                    // 1st Place (Gold)
                    PodiumPillar(entry = first, place = 1, crownColor = Color(0xFFFFD700), pillarHeight = 110.dp)
                    // 3rd Place (Bronze)
                    PodiumPillar(entry = third, place = 3, crownColor = Color(0xFFCD7F32), pillarHeight = 70.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Leaderboard list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(entries) { index, entry ->
                    val isMe = entry.userId == currentUser.id

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMe) Color(0xFF272138) else Color(0xFF1B1B22),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMe) Color(0xFFE1306C) else Color(0xFF292934)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ranking_row_${entry.rank}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "#${entry.rank}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = when (entry.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> Color.White.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.width(32.dp)
                            )

                            Box(modifier = Modifier.size(42.dp)) {
                                if (entry.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = entry.avatarUrl,
                                        contentDescription = entry.username,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF3897F0),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(entry.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.username,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (entry.vipLevel > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("👑${entry.vipLevel}", fontSize = 10.sp, color = Color(0xFFFFD700))
                                    }
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE1306C)
                                        ) {
                                            Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = "ID: ${entry.numericId} • ${entry.badgeLabel}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }

                            Text(
                                text = "%,d".format(entry.score),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = when (selectedCategory) {
                                    0 -> Color(0xFFFFD700)
                                    1 -> Color(0xFFFF4081)
                                    else -> Color(0xFF3897F0)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumPillar(
    entry: LeaderboardEntry,
    place: Int,
    crownColor: Color,
    pillarHeight: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp)
    ) {
        Text(if (place == 1) "👑" else "🥈", fontSize = 18.sp)

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(2.dp, crownColor, CircleShape)
        ) {
            if (entry.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Surface(color = Color(0xFF3897F0), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(entry.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = entry.username,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )

        Text(
            text = "%,d".format(entry.score),
            fontSize = 10.sp,
            color = crownColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = Color(0xFF1E1E28),
            border = androidx.compose.foundation.BorderStroke(1.dp, crownColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().height(pillarHeight)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$place",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = crownColor
                )
            }
        }
    }
}
