package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.VipTierConfig
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipCenterBottomSheet(
    repository: AppRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val vipTiers by repository.vipTiers.collectAsState()

    var selectedTier by remember { mutableStateOf<VipTierConfig>(vipTiers.firstOrNull { it.level == currentUser.vipTierLevel.coerceAtLeast(1) } ?: vipTiers.first()) }
    var selectedDurationMonths by remember { mutableStateOf(1) } // 1, 3, 12
    var isProcessing by remember { mutableStateOf(false) }
    var activationSuccessAlert by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121215),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxHeight(0.92f).testTag("vip_center_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Current VIP Status Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1B29),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6A3B9E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF321E52)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser.isVipActive) "👑" else "🛡️",
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentUser.isVipActive) "VIP ${currentUser.vipTierLevel} Active" else "No Active VIP",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (currentUser.isVipActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD700)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        val expiryText = if (currentUser.vipExpiryTimestamp != null && currentUser.isVipActive) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            "Expires ${sdf.format(Date(currentUser.vipExpiryTimestamp!!))}"
                        } else {
                            "Unlock exclusive frames, entrance effects, and coin bonuses"
                        }

                        Text(
                            text = expiryText,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration selector pills (1 Month, 3 Months, 1 Year)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "1 Month", 3 to "3 Months (-10%)", 12 to "1 Year (-20%)").forEach { (months, label) ->
                    val isSelected = selectedDurationMonths == months
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF6A3B9E) else Color(0xFF1E1E26),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF9C27B0) else Color(0xFF2C2C38)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDurationMonths = months }
                            .testTag("vip_duration_$months")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10 VIP Tiers List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(vipTiers) { tier ->
                    val isSelected = selectedTier.level == tier.level
                    val cost = when (selectedDurationMonths) {
                        3 -> tier.coinPriceQuarterly
                        12 -> tier.coinPriceAnnual
                        else -> tier.coinPriceMonthly
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF231D31) else Color(0xFF191822),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color(android.graphics.Color.parseColor(tier.badgeColorHex)) else Color(0xFF2A2838)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTier = tier }
                            .testTag("vip_tier_card_${tier.level}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(tier.badgeIcon, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tier.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(android.graphics.Color.parseColor(tier.badgeColorHex)).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "+${tier.bonusCoinPercent}% Bonus",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(android.graphics.Color.parseColor(tier.badgeColorHex)),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Entrance: ${tier.entranceEffect}",
                                        fontSize = 11.sp,
                                        color = Color(android.graphics.Color.parseColor(tier.badgeColorHex))
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "%,d 🪙".format(cost),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = "/${if (selectedDurationMonths == 1) "mo" else "${selectedDurationMonths}mo"}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Membership Privileges:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )

                                tier.benefits.forEach { benefit ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(benefit, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        isProcessing = true
                                        repository.purchaseVipTier(tier.level, selectedDurationMonths, useCoins = true) { success, msg ->
                                            isProcessing = false
                                            if (success) {
                                                activationSuccessAlert = tier.name
                                            } else {
                                                errorMessage = msg
                                            }
                                        }
                                    },
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(android.graphics.Color.parseColor(tier.badgeColorHex))
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("activate_vip_button")
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = "Activate ${tier.name} (%,d 🪙)".format(cost),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activationSuccessAlert != null) {
        AlertDialog(
            onDismissRequest = { activationSuccessAlert = null },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIP Activated!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = "Congratulations! You have activated $activationSuccessAlert. Your exclusive profile badge, frames, and voice room entry effects are now active.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { activationSuccessAlert = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text("Superb!", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = Color(0xFF1E1E24),
            title = { Text("VIP Purchase Failed", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
            text = { Text(errorMessage ?: "", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
