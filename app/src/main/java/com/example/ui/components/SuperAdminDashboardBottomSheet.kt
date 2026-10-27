package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.CoinPackage
import com.example.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardBottomSheet(
    repository: AppRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val coinPackages by repository.coinPackages.collectAsState()
    val auditLogs by repository.adminAuditLogs.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0 = User Management, 1 = Coin Packages, 2 = Audit Log
    var searchUserIdQuery by remember { mutableStateOf("") }
    var searchedUser by remember { mutableStateOf<UserProfile?>(null) }
    var adjustmentAmountText by remember { mutableStateOf("") }
    var adjustmentReasonText by remember { mutableStateOf("") }
    var adjustmentSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111114),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxHeight(0.92f).testTag("super_admin_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Super Admin Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF5252).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                ) {
                    Text(
                        text = "ROOT ACCESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation tabs
            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                listOf("👤 User Lookup", "🪙 Packages", "📜 Audit Logs").forEachIndexed { idx, label ->
                    val isSelected = selectedSection == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF282834) else Color(0xFF181820),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C4C62)) else null,
                        modifier = Modifier
                            .padding(2.dp)
                            .clickable { selectedSection = idx }
                            .testTag("admin_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedSection) {
                0 -> {
                    // USER MANAGEMENT BY 7-DIGIT ID
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text("Search User by Exact 7-Digit Numeric ID:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = searchUserIdQuery,
                                    onValueChange = { searchUserIdQuery = it },
                                    placeholder = { Text("e.g. 8294102", fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFE1306C),
                                        unfocusedBorderColor = Color(0xFF33333E)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_user_id_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val user = repository.findUserByNumericId(searchUserIdQuery.trim())
                                        if (user != null) {
                                            searchedUser = user
                                            errorMessage = null
                                        } else {
                                            searchedUser = null
                                            errorMessage = "No user found with ID $searchUserIdQuery"
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                    modifier = Modifier.testTag("admin_search_btn")
                                ) {
                                    Text("Find", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (errorMessage != null) {
                            item {
                                Text(errorMessage ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                            }
                        }

                        val userToDisplay = searchedUser ?: currentUser
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1A1A22),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E3A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(userToDisplay.fullName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("@${userToDisplay.username} • UID: ${userToDisplay.id}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                            Text("Public ID: ${userToDisplay.numericId}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF3897F0))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFFD700).copy(alpha = 0.2f)
                                        ) {
                                            Text("VIP ${userToDisplay.vipTierLevel}", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Coin Balance:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("%,d 🪙".format(userToDisplay.coinBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Wealth Level:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Lv.${userToDisplay.wealthLevel} (%,d pts)".format(userToDisplay.wealthPoints), fontSize = 13.sp, color = Color(0xFFFFA500))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Charm Level:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Lv.${userToDisplay.charmLevel} (%,d pts)".format(userToDisplay.charmPoints), fontSize = 13.sp, color = Color(0xFFFF4081))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("User XP Level:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Lv.${userToDisplay.userLevel} (%,d xp)".format(userToDisplay.userXp), fontSize = 13.sp, color = Color(0xFF3897F0))
                                    }
                                }
                            }
                        }

                        // Audited Coin Adjustment Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF22161A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C2028)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Audited Coin Adjustment (Admin Only):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = adjustmentAmountText,
                                        onValueChange = { adjustmentAmountText = it },
                                        placeholder = { Text("Coin delta (+5000 or -2000)", fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFF5252),
                                            unfocusedBorderColor = Color(0xFF3E282E)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_delta_input")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = adjustmentReasonText,
                                        onValueChange = { adjustmentReasonText = it },
                                        placeholder = { Text("Mandatory audit reason (e.g. VIP compensation #481)", fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFF5252),
                                            unfocusedBorderColor = Color(0xFF3E282E)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_reason_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val delta = adjustmentAmountText.toLongOrNull()
                                            if (delta == null) {
                                                errorMessage = "Enter valid integer amount"
                                                return@Button
                                            }
                                            if (adjustmentReasonText.isBlank()) {
                                                errorMessage = "Mandatory audit reason required"
                                                return@Button
                                            }

                                            repository.adminAdjustCoins(userToDisplay.id, delta, adjustmentReasonText)
                                            adjustmentSuccessMessage = "Adjusted %,d coins with audit log.".format(delta)
                                            adjustmentAmountText = ""
                                            adjustmentReasonText = ""
                                            searchedUser = repository.currentUser.value
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_apply_adjustment")
                                    ) {
                                        Text("Apply Audited Balance Adjustment", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (adjustmentSuccessMessage != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1B3820),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = adjustmentSuccessMessage ?: "",
                                        color = Color(0xFF81C784),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // COIN PACKAGE MANAGEMENT
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text("Live Coin Shop Packages (Editable in Real-Time):", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                        items(coinPackages) { pkg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B1B22),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C38)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pkg.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("%,d Coins + %,d Bonus".format(pkg.coins, pkg.bonusCoins), fontSize = 12.sp, color = Color(0xFFFFD700))
                                    }
                                    Text("$%.2f".format(pkg.priceUsd), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Switch(
                                        checked = pkg.isActive,
                                        onCheckedChange = { repository.toggleCoinPackageActive(pkg.id) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE1306C))
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // AUDIT LOGS
                    if (auditLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No audit log events recorded yet.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(auditLogs) { log ->
                                val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF181820),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A38)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(log.action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(sdf.format(Date(log.timestamp)), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                                        }
                                        Text(log.details, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        Text("Admin: ${log.adminId} • Target: ${log.targetId}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
