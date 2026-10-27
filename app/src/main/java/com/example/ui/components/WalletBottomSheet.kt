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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.model.CoinLedgerTransaction
import com.example.model.CoinPackage
import com.example.model.LedgerTransactionType
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.InstagramSignatureGradient
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletBottomSheet(
    repository: AppRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val coinPackages by repository.coinPackages.collectAsState()
    val ledger by repository.coinLedger.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Buy Coins, 1 = Ledger History
    var selectedPackage by remember { mutableStateOf<CoinPackage?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentSuccessDialog by remember { mutableStateOf<CoinPackage?>(null) }
    var paymentErrorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141417),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = modifier.fillMaxHeight(0.92f).testTag("wallet_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Wallet Balance Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1F1F24),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33333A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Virtual Coin Balance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🪙",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "%,d".format(currentUser.coinBalance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = " Coins",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("User ID", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(currentUser.numericId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wealth Level", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("Lv.${currentUser.wealthLevel}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("VIP Status", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(if (currentUser.isVipActive) "VIP ${currentUser.vipTierLevel}" else "None", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF69B4))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector: Buy Coins vs Ledger
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                listOf("🪙 Buy Coins", "📜 Transaction Ledger").forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2E2E36) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A4A58)) else null,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { selectedTab = idx }
                            .testTag("wallet_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            if (selectedTab == 0) {
                // BUY COINS PACKAGES
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Select a coin package to top up instantly with verified payment:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(coinPackages.filter { it.isActive }) { pkg ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B1B20),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    pkg.isPopular -> Color(0xFFE1306C)
                                    pkg.isRecommended -> Color(0xFFFFD700)
                                    else -> Color(0xFF2C2C34)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("coin_package_${pkg.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262630)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🪙", fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pkg.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (pkg.badgeLabel != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (pkg.isPopular) Color(0xFFE1306C) else Color(0xFFFF9800)
                                            ) {
                                                Text(
                                                    text = pkg.badgeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "%,d Coins".format(pkg.coins),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFFD700)
                                        )
                                        if (pkg.bonusCoins > 0) {
                                            Text(
                                                text = " + %,d Bonus".format(pkg.bonusCoins),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Buy Price Button
                                Button(
                                    onClick = {
                                        selectedPackage = pkg
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pkg.isPopular) Color(0xFFE1306C) else Color(0xFF2C2C38),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("buy_button_${pkg.id}")
                                ) {
                                    Text(
                                        text = "$%.2f".format(pkg.priceUsd),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF131A24),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Payments processed with 256-bit SSL encryption. Coins credited atomically upon bank verification.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF90CAF9),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // TRANSACTION LEDGER
                if (ledger.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No wallet transactions yet", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ledger) { tx ->
                            val isCredit = tx.amount > 0
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            val dateString = dateFormat.format(Date(tx.timestamp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1A1A20),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282832)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                ) {
                                    Text(tx.type.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.type.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = tx.metadata,
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "$dateString • TX #${tx.id.takeLast(6)}",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isCredit) "+" else ""}${tx.amount} 🪙",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFFF5252)
                                        )
                                        Text(
                                            text = "Bal: %,d".format(tx.balanceAfter),
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Confirmation Modal
    if (selectedPackage != null) {
        val pkg = selectedPackage!!
        AlertDialog(
            onDismissRequest = { if (!isProcessingPayment) selectedPackage = null },
            containerColor = Color(0xFF1E1E24),
            title = {
                Text(
                    text = "Confirm Coin Purchase",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "You are purchasing ${pkg.name} for $%.2f.".format(pkg.priceUsd),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF141418),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Coins:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("%,d Coins".format(pkg.totalCoins), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Wealth Points Earned:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("+%,d pts".format(pkg.totalCoins), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                            }
                        }
                    }

                    if (isProcessingPayment) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFFE1306C))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying with payment gateway...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingPayment = true
                        repository.purchaseCoinPackage(pkg.id, "Google Play Billing") { success, msg ->
                            isProcessingPayment = false
                            if (success) {
                                paymentSuccessDialog = pkg
                                selectedPackage = null
                            } else {
                                paymentErrorMessage = msg
                            }
                        }
                    },
                    enabled = !isProcessingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                    modifier = Modifier.testTag("confirm_purchase_button")
                ) {
                    Text("Pay $%.2f".format(pkg.priceUsd), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedPackage = null },
                    enabled = !isProcessingPayment
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Payment Success Alert
    if (paymentSuccessDialog != null) {
        val pkg = paymentSuccessDialog!!
        AlertDialog(
            onDismissRequest = { paymentSuccessDialog = null },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purchase Successful!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = "%,d virtual coins have been added to your wallet. Your wealth level has been updated.".format(pkg.totalCoins),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { paymentSuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
