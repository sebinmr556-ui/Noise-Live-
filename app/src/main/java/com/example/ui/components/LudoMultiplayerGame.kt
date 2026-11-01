package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoomLudoGameState
import com.example.model.RoomLudoSeat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LudoPlayerColor(
    val displayName: String,
    val primaryColor: Color,
    val lightColor: Color,
    val startTrackIndex: Int
) {
    RED("Red", Color(0xFFEF4444), Color(0xFFFCA5A5), 0),
    GREEN("Green", Color(0xFF10B981), Color(0xFF6EE7B7), 13),
    YELLOW("Yellow", Color(0xFFF59E0B), Color(0xFFFDE68A), 26),
    BLUE("Blue", Color(0xFF3B82F6), Color(0xFF93C5FD), 39)
}

data class LudoToken(
    val id: Int,
    val playerColor: LudoPlayerColor,
    val stepProgress: Int = -1 // -1 = in base, 0..50 = outer track, 51..55 = home stretch, 56 = home
) {
    val isInBase: Boolean get() = stepProgress < 0
    val isHome: Boolean get() = stepProgress >= 56
    val isInHomeStretch: Boolean get() = stepProgress in 51..55
    val globalTrackPos: Int
        get() = if (stepProgress in 0..50) (playerColor.startTrackIndex + stepProgress) % 52 else -1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoMultiplayerGameDialog(
    roomId: String,
    roomTitle: String,
    currentUserId: String = "",
    currentUserName: String,
    currentUserAvatar: String,
    currentUserVipLevel: Int = 1,
    ludoState: RoomLudoGameState? = null,
    onJoinSeat: (String) -> Unit = {},
    onLeaveSeat: (String) -> Unit = {},
    onRollDice: () -> Unit = {},
    onMoveToken: (String, Int) -> Unit = { _, _ -> },
    onResetGame: () -> Unit = {},
    onDismiss: () -> Unit,
    onAwardCoins: (Long) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val state = ludoState ?: RoomLudoGameState(isActive = true)

    val seatsByColor = remember(state.seats) {
        LudoPlayerColor.values().associateWith { color ->
            state.seats[color.name] ?: RoomLudoSeat(colorName = color.name)
        }
    }

    val tokensByColor = remember(state.tokenSteps) {
        LudoPlayerColor.values().associateWith { color ->
            val steps = state.tokenSteps[color.name] ?: listOf(-1, -1, -1, -1)
            (0..3).map { idx ->
                LudoToken(
                    id = idx,
                    playerColor = color,
                    stepProgress = steps.getOrElse(idx) { -1 }
                )
            }
        }
    }

    val activeColor = remember(state.currentTurnColor) {
        LudoPlayerColor.values().firstOrNull {
            it.name.equals(state.currentTurnColor, ignoreCase = true)
        } ?: LudoPlayerColor.RED
    }
    val activeSeat = seatsByColor[activeColor]
    val mySeat = state.seats.values.firstOrNull { it.playerId.isNotBlank() && it.playerId == currentUserId }
    val isSpectator = mySeat == null
    val isMyTurn = activeSeat?.playerId?.isNotBlank() == true && activeSeat.playerId == currentUserId
    val claimedSeatsCount = state.seats.values.count { it.playerId.isNotBlank() }

    // 15-second turn countdown timer synced with turnStartedAtMillis
    var turnRemainingSec by remember(state.turnStartedAtMillis, state.currentTurnColor) {
        mutableStateOf(15)
    }
    LaunchedEffect(state.turnStartedAtMillis, state.currentTurnColor, state.winnerName) {
        if (!state.winnerName.isNullOrBlank() || claimedSeatsCount < 2) {
            turnRemainingSec = 15
            return@LaunchedEffect
        }
        while (true) {
            val elapsed = ((System.currentTimeMillis() - state.turnStartedAtMillis) / 1000L).toInt()
            val rem = (15 - elapsed).coerceIn(0, 15)
            turnRemainingSec = rem
            if (rem <= 0 && isMyTurn && !state.hasRolled) {
                onRollDice()
                break
            }
            delay(500L)
        }
    }

    // Award coins once when local player wins
    var rewardedWinner by remember { mutableStateOf("") }
    LaunchedEffect(state.winnerName) {
        val winName = state.winnerName.orEmpty()
        if (winName.isNotBlank() &&
            winName != rewardedWinner &&
            mySeat != null &&
            winName == mySeat.playerName
        ) {
            rewardedWinner = winName
            onAwardCoins(500L)
        }
    }

    var isRollingAnim by remember { mutableStateOf(false) }
    var diceRotation by remember { mutableStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F091E),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(42.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color.White.copy(alpha = 0.3f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "♟️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ludo Royale (2–4P Synced)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSpectator) Color(0xFF374151) else Color(0xFF065F46)
                            ) {
                                Text(
                                    text = if (isSpectator) "👁 SPECTATOR" else "🎮 PLAYER (${mySeat?.colorName})",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$roomTitle • $claimedSeatsCount/4 Seats Claimed",
                            fontSize = 10.sp,
                            color = Color(0xFFEAB308)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetGame,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("ludo_reset_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Match",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4 Interactive Player Seat Claim Slots (Red, Green, Yellow, Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LudoPlayerColor.values().forEach { color ->
                    val seat = seatsByColor[color] ?: RoomLudoSeat(colorName = color.name)
                    val isClaimed = seat.playerId.isNotBlank()
                    val isMine = isClaimed && seat.playerId == currentUserId
                    val isTurn = activeColor == color && isClaimed

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isClaimed) Color(0xFF1B1130) else Color(0xFF120B20),
                        border = BorderStroke(
                            width = if (isTurn) 2.dp else 1.dp,
                            color = if (isTurn) color.primaryColor else color.primaryColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!isClaimed) {
                                    onJoinSeat(color.name)
                                } else if (isMine) {
                                    onLeaveSeat(color.name)
                                }
                            }
                            .testTag("ludo_seat_${color.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color.primaryColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = color.displayName,
                                    color = color.lightColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isClaimed) {
                                    if (isMine) "You (Tap Leave)" else seat.playerName
                                } else {
                                    "+ Join Seat"
                                },
                                color = if (isClaimed) Color.White else Color(0xFFFDE047),
                                fontSize = 9.sp,
                                fontWeight = if (isClaimed) FontWeight.SemiBold else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Turn & Countdown Timer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF19102B))
                    .border(
                        1.dp,
                        activeColor.primaryColor.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeColor.primaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeSeat?.playerId?.isNotBlank() == true) {
                            "Turn: ${activeSeat.playerName} (${activeColor.displayName})"
                        } else {
                            "Waiting for players to claim seats..."
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (claimedSeatsCount >= 2 && state.winnerName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (turnRemainingSec <= 5) Color(0xFF7F1D1D) else Color(0xFF2B1942)
                        ) {
                            Text(
                                text = "⏱ ${turnRemainingSec}s",
                                color = if (turnRemainingSec <= 5) Color(0xFFFCA5A5) else Color(0xFFFDE047),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = when {
                            isMyTurn -> "👤 Your Turn"
                            isSpectator -> "👁 Spectating"
                            else -> "⏳ Opponent Turn"
                        },
                        color = if (isMyTurn) Color(0xFF10B981) else Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Game Status Message Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1F1133),
                border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.statusMessage.ifBlank {
                        "Claim a seat (Red, Green, Yellow, or Blue) to start a live multiplayer Ludo match!"
                    },
                    color = Color(0xFFFDE047),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stylized Dark Luxury Ludo Board with Safe Stars & Real-Time Token Positions
            val activeDiceRoll: Int? = if (state.hasRolled) state.diceValue else null
            LudoBoardCanvas(
                seatsByColor = seatsByColor,
                tokensByColor = tokensByColor,
                activeColor = activeColor,
                isMyTurn = isMyTurn,
                currentDiceRoll = activeDiceRoll,
                onTokenClick = { token ->
                    if (isMyTurn && state.hasRolled) {
                        onMoveToken(token.playerColor.name, token.id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        Brush.sweepGradient(listOf(Color(0xFFEAB308), Color(0xFFA855F7), Color(0xFFEAB308))),
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color(0xFF0A0515))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Controls & 3D Dice Roll Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "★ Safe Squares: Starred track tiles protect tokens from cutting.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (isSpectator) {
                            "You are watching in real-time Spectator Mode. Tap any open seat above to join!"
                        } else if (isMyTurn) {
                            if (!state.hasRolled) "Tap the 3D dice to roll for your turn!"
                            else "Rolled ${state.diceValue}! Tap one of your ${mySeat?.colorName} tokens to advance."
                        } else {
                            "Live board updates automatically as players roll and move."
                        },
                        color = Color(0xFFFDE047),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 3D Animated Dice Button
                val canRoll = isMyTurn && !state.hasRolled && !isRollingAnim && state.winnerName.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (canRoll) {
                                    listOf(Color(0xFF4C1D95), Color(0xFF7E22CE), Color(0xFFA855F7))
                                } else {
                                    listOf(Color(0xFF1F1630), Color(0xFF2B1E40))
                                }
                            )
                        )
                        .border(
                            2.dp,
                            if (state.hasRolled) Color(0xFFEAB308)
                            else if (canRoll) Color(0xFFFDE047)
                            else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = canRoll) {
                            isRollingAnim = true
                            coroutineScope.launch {
                                repeat(5) {
                                    diceRotation += 72f
                                    delay(55)
                                }
                                isRollingAnim = false
                                onRollDice()
                            }
                        }
                        .testTag("ludo_dice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (state.diceValue) {
                                1 -> "⚀"
                                2 -> "⚁"
                                3 -> "⚂"
                                4 -> "⚃"
                                5 -> "⚄"
                                6 -> "⚅"
                                else -> "🎲"
                            },
                            fontSize = 30.sp,
                            color = if (state.diceValue == 6) Color(0xFFFDE047) else Color.White,
                            modifier = Modifier.rotate(diceRotation)
                        )
                        Text(
                            text = if (state.hasRolled) "ROLLED ${state.diceValue}" else if (canRoll) "ROLL" else "WAIT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LudoBoardCanvas(
    seatsByColor: Map<LudoPlayerColor, RoomLudoSeat>,
    tokensByColor: Map<LudoPlayerColor, List<LudoToken>>,
    activeColor: LudoPlayerColor,
    isMyTurn: Boolean,
    currentDiceRoll: Int?,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeIndices = remember { setOf(0, 8, 13, 21, 26, 34, 39, 47) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background Grid & Track Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / 15f
            val cellH = h / 15f

            // Draw Cross Track Grid Lines
            for (r in 0 until 15) {
                for (c in 0 until 15) {
                    val inTopLeft = r < 6 && c < 6
                    val inTopRight = r < 6 && c >= 9
                    val inBottomLeft = r >= 9 && c < 6
                    val inBottomRight = r >= 9 && c >= 9
                    val inCenter = r in 6..8 && c in 6..8
                    if (!inTopLeft && !inTopRight && !inBottomLeft && !inBottomRight && !inCenter) {
                        val isHomePathRed = r == 7 && c in 1..5
                        val isHomePathGreen = c == 7 && r in 1..5
                        val isHomePathYellow = r == 7 && c in 9..13
                        val isHomePathBlue = c == 7 && r in 9..13
                        val tileColor = when {
                            isHomePathRed -> Color(0xFF7F1D1D).copy(alpha = 0.65f)
                            isHomePathGreen -> Color(0xFF064E3B).copy(alpha = 0.65f)
                            isHomePathYellow -> Color(0xFF78350F).copy(alpha = 0.65f)
                            isHomePathBlue -> Color(0xFF1E3A8A).copy(alpha = 0.65f)
                            else -> Color(0xFF181028)
                        }
                        drawRect(
                            color = tileColor,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.12f),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Red Base (Top Left: 6x6)
            drawRoundRect(
                color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(cellW * 6, cellH * 6),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(0f, 0f),
                size = Size(cellW * 6, cellH * 6),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Green Base (Top Right: 6x6)
            drawRoundRect(
                color = Color(0xFF064E3B).copy(alpha = 0.5f),
                topLeft = Offset(cellW * 9, 0f),
                size = Size(cellW * 6, cellH * 6),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(cellW * 9, 0f),
                size = Size(cellW * 6, cellH * 6),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Blue Base (Bottom Left: 6x6)
            drawRoundRect(
                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                topLeft = Offset(0f, cellH * 9),
                size = Size(cellW * 6, cellH * 6),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFF3B82F6),
                topLeft = Offset(0f, cellH * 9),
                size = Size(cellW * 6, cellH * 6),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Yellow Base (Bottom Right: 6x6)
            drawRoundRect(
                color = Color(0xFF78350F).copy(alpha = 0.5f),
                topLeft = Offset(cellW * 9, cellH * 9),
                size = Size(cellW * 6, cellH * 6),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(cellW * 9, cellH * 9),
                size = Size(cellW * 6, cellH * 6),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Safe Square Gold Circles on Track
            val safeGridCoords = listOf(
                1 to 6, 6 to 2, 2 to 8, 6 to 12,
                8 to 13, 12 to 8, 13 to 6, 8 to 1
            )
            safeGridCoords.forEach { (c, r) ->
                drawCircle(
                    color = Color(0xFFEAB308).copy(alpha = 0.45f),
                    radius = cellW * 0.35f,
                    center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)
                )
            }

            // Center Home Square (3x3)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEAB308), Color(0xFF1E1035)),
                    center = Offset(w / 2f, h / 2f),
                    radius = cellW * 2.5f
                ),
                topLeft = Offset(cellW * 6, cellH * 6),
                size = Size(cellW * 3, cellH * 3)
            )
            drawRect(
                color = Color(0xFFEAB308),
                topLeft = Offset(cellW * 6, cellH * 6),
                size = Size(cellW * 3, cellH * 3),
                style = Stroke(width = 2f)
            )
        }

        // Interactive Player Yards & Center Home Status
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Yards: Red & Green
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerYardBox(
                    color = LudoPlayerColor.RED,
                    seat = seatsByColor[LudoPlayerColor.RED],
                    tokens = tokensByColor[LudoPlayerColor.RED].orEmpty(),
                    isActive = activeColor == LudoPlayerColor.RED,
                    canMove = isMyTurn && activeColor == LudoPlayerColor.RED && currentDiceRoll != null,
                    currentDiceRoll = currentDiceRoll,
                    safeIndices = safeIndices,
                    onTokenClick = onTokenClick
                )

                PlayerYardBox(
                    color = LudoPlayerColor.GREEN,
                    seat = seatsByColor[LudoPlayerColor.GREEN],
                    tokens = tokensByColor[LudoPlayerColor.GREEN].orEmpty(),
                    isActive = activeColor == LudoPlayerColor.GREEN,
                    canMove = isMyTurn && activeColor == LudoPlayerColor.GREEN && currentDiceRoll != null,
                    currentDiceRoll = currentDiceRoll,
                    safeIndices = safeIndices,
                    onTokenClick = onTokenClick
                )
            }

            // Center Home Trophy & Active Track Summary
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B0F2E))
                    .border(2.dp, Color(0xFFEAB308), CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                val totalHome = tokensByColor.values.flatten().count { it.isHome }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "👑", fontSize = 16.sp)
                    Text(
                        text = "HOME ($totalHome)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFDE047)
                    )
                    Text(
                        text = "★ SAFE",
                        fontSize = 7.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom Yards: Blue & Yellow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerYardBox(
                    color = LudoPlayerColor.BLUE,
                    seat = seatsByColor[LudoPlayerColor.BLUE],
                    tokens = tokensByColor[LudoPlayerColor.BLUE].orEmpty(),
                    isActive = activeColor == LudoPlayerColor.BLUE,
                    canMove = isMyTurn && activeColor == LudoPlayerColor.BLUE && currentDiceRoll != null,
                    currentDiceRoll = currentDiceRoll,
                    safeIndices = safeIndices,
                    onTokenClick = onTokenClick
                )

                PlayerYardBox(
                    color = LudoPlayerColor.YELLOW,
                    seat = seatsByColor[LudoPlayerColor.YELLOW],
                    tokens = tokensByColor[LudoPlayerColor.YELLOW].orEmpty(),
                    isActive = activeColor == LudoPlayerColor.YELLOW,
                    canMove = isMyTurn && activeColor == LudoPlayerColor.YELLOW && currentDiceRoll != null,
                    currentDiceRoll = currentDiceRoll,
                    safeIndices = safeIndices,
                    onTokenClick = onTokenClick
                )
            }
        }
    }
}

@Composable
private fun PlayerYardBox(
    color: LudoPlayerColor,
    seat: RoomLudoSeat?,
    tokens: List<LudoToken>,
    isActive: Boolean,
    canMove: Boolean,
    currentDiceRoll: Int?,
    safeIndices: Set<Int>,
    onTokenClick: (LudoToken) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF140B24).copy(alpha = 0.92f),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) color.primaryColor else color.primaryColor.copy(alpha = 0.4f)
        ),
        modifier = Modifier.size(116.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (seat?.userName?.isNotBlank() == true) {
                    "${color.displayName} • ${seat.userName.take(7)}"
                } else {
                    "${color.displayName} (Open)"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color.lightColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tokens.take(2).forEach { token ->
                    TokenPawnView(
                        token = token,
                        color = color,
                        canMove = canMove,
                        currentDiceRoll = currentDiceRoll,
                        safeIndices = safeIndices,
                        onClick = { onTokenClick(token) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tokens.drop(2).take(2).forEach { token ->
                    TokenPawnView(
                        token = token,
                        color = color,
                        canMove = canMove,
                        currentDiceRoll = currentDiceRoll,
                        safeIndices = safeIndices,
                        onClick = { onTokenClick(token) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenPawnView(
    token: LudoToken,
    color: LudoPlayerColor,
    canMove: Boolean,
    currentDiceRoll: Int?,
    safeIndices: Set<Int>,
    onClick: () -> Unit
) {
    val roll = currentDiceRoll ?: 0
    val isValidMove = canMove && when {
        token.isInBase -> roll == 6
        token.isHome -> false
        else -> token.stepProgress + roll <= 56
    }
    val isSafe = token.globalTrackPos in safeIndices

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        token.isHome -> Color(0xFF10B981)
                        !token.isInBase -> color.primaryColor
                        else -> color.primaryColor.copy(alpha = 0.55f)
                    }
                )
                .border(
                    width = if (isValidMove) 2.5.dp else 1.dp,
                    color = if (isValidMove) Color(0xFFFDE047) else Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable(enabled = isValidMove, onClick = onClick)
                .testTag("ludo_token_${color.displayName}_${token.id}"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    token.isHome -> "👑"
                    token.isInBase -> "B${token.id + 1}"
                    token.isInHomeStretch -> "H${token.stepProgress - 50}"
                    isSafe -> "★"
                    else -> "${token.stepProgress}"
                },
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarromMultiplayerGameDialog(
    roomId: String,
    roomTitle: String,
    currentUserName: String,
    currentUserAvatar: String,
    currentUserVipLevel: Int = 1,
    onDismiss: () -> Unit,
    onAwardCoins: (Long) -> Unit = {}
) {
    var playerScore by remember { mutableStateOf(0) }
    var opponentScore by remember { mutableStateOf(0) }
    var queenPocketedBy by remember { mutableStateOf<String?>(null) }
    var strikerAngle by remember { mutableStateOf(0f) }
    var strikerPower by remember { mutableStateOf(0.7f) }
    var remainingWhitePucks by remember { mutableStateOf(9) }
    var remainingBlackPucks by remember { mutableStateOf(9) }
    var statusMessage by remember { mutableStateOf("Aim your striker & shoot to pocket White Carrom Men or the Red Queen!") }
    var isPlayerTurn by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161026),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯 Carrom Royale", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF7C3AED)) {
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF22183B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(currentUserName.ifBlank { "You" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("White Pucks: $playerScore pts", color = Color(0xFF34D399), fontSize = 11.sp)
                    }
                    Text("VS", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Challenger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Black Pucks: $opponentScore pts", color = Color(0xFFF472B6), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF5C3A21))
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBD5B3)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val pocketR = 16.dp.toPx()

                        listOf(
                            Offset(pocketR, pocketR),
                            Offset(w - pocketR, pocketR),
                            Offset(pocketR, h - pocketR),
                            Offset(w - pocketR, h - pocketR)
                        ).forEach { pos ->
                            drawCircle(color = Color(0xFF1F1209), radius = pocketR, center = pos)
                        }

                        drawCircle(
                            color = Color(0xFFB91C1C),
                            radius = 34.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        if (queenPocketedBy == null) {
                            drawCircle(color = Color(0xFFDC2626), radius = 8.dp.toPx(), center = center)
                        }

                        for (i in 0 until remainingWhitePucks) {
                            val angleRad = Math.toRadians((i * 40).toDouble())
                            val dist = 22.dp.toPx()
                            val px = center.x + (Math.cos(angleRad) * dist).toFloat()
                            val py = center.y + (Math.sin(angleRad) * dist).toFloat()
                            drawCircle(color = Color(0xFFFFFBEB), radius = 7.dp.toPx(), center = Offset(px, py))
                            drawCircle(color = Color(0xFF78350F), radius = 7.dp.toPx(), center = Offset(px, py), style = Stroke(1.5f))
                        }
                        for (i in 0 until remainingBlackPucks) {
                            val angleRad = Math.toRadians((i * 40 + 20).toDouble())
                            val dist = 36.dp.toPx()
                            val px = center.x + (Math.cos(angleRad) * dist).toFloat()
                            val py = center.y + (Math.sin(angleRad) * dist).toFloat()
                            drawCircle(color = Color(0xFF1E293B), radius = 7.dp.toPx(), center = Offset(px, py))
                        }

                        val strikerX = (w * 0.5f) + (strikerAngle * (w * 0.3f))
                        val strikerY = h - 26.dp.toPx()
                        drawCircle(color = Color(0xFF38BDF8), radius = 10.dp.toPx(), center = Offset(strikerX, strikerY))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = statusMessage,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Aim Position", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                Slider(
                    value = strikerAngle,
                    onValueChange = { strikerAngle = it },
                    valueRange = -1f..1f,
                    modifier = Modifier.height(26.dp)
                )

                Text("Strike Power: ${(strikerPower * 100).toInt()}%", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                Slider(
                    value = strikerPower,
                    onValueChange = { strikerPower = it },
                    valueRange = 0.2f..1f,
                    modifier = Modifier.height(26.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isPlayerTurn && (remainingWhitePucks > 0 || remainingBlackPucks > 0),
                onClick = {
                    isPlayerTurn = false
                    val hitRoll = Random.nextFloat() * strikerPower
                    when {
                        queenPocketedBy == null && hitRoll > 0.58f -> {
                            queenPocketedBy = currentUserName
                            playerScore += 50
                            onAwardCoins(250L)
                            statusMessage = "👑 Red Queen pocketed & covered! +50 pts & +250 Coins!"
                        }
                        remainingWhitePucks > 0 && hitRoll > 0.25f -> {
                            val pocketed = if (hitRoll > 0.65f && remainingWhitePucks >= 2) 2 else 1
                            remainingWhitePucks -= pocketed
                            playerScore += pocketed * 20
                            onAwardCoins(pocketed * 50L)
                            statusMessage = "🎯 Clean strike! Pocketed $pocketed White Carrom ${if (pocketed > 1) "Men" else "Man"}!"
                        }
                        else -> {
                            statusMessage = "Striker grazed the rim! Opponent's turn..."
                        }
                    }

                    scope.launch {
                        delay(700)
                        if (remainingBlackPucks > 0) {
                            val oppHit = Random.nextBoolean()
                            if (oppHit) {
                                remainingBlackPucks -= 1
                                opponentScore += 20
                            }
                        }
                        isPlayerTurn = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                modifier = Modifier.testTag("carrom_strike_button")
            ) {
                Text("Strike Carrom", color = Color(0xFF1E1035), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    playerScore = 0
                    opponentScore = 0
                    queenPocketedBy = null
                    remainingWhitePucks = 9
                    remainingBlackPucks = 9
                    statusMessage = "New Carrom Royale board racked!"
                }
            ) {
                Text("Reset Board", color = Color.White.copy(alpha = 0.8f))
            }
        }
    )
}
