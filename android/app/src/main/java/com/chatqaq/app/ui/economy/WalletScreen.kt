package com.chatqaq.app.ui.economy

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatqaq.app.ui.theme.*

data class CoinPack(val productId: String, val coins: Int, val price: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val wallet by viewModel.walletState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmountText by remember { mutableStateOf("") }
    var paymentMethodText by remember { mutableStateOf("Bank Transfer") }
    var accountDetailsText by remember { mutableStateOf("") }

    val coinPacks = remember {
        listOf(
            CoinPack("coins_tier_1", 100, "$0.99"),
            CoinPack("coins_tier_2", 550, "$4.99"),
            CoinPack("coins_tier_3", 1200, "$9.99"),
            CoinPack("coins_tier_4", 2600, "$19.99"),
            CoinPack("coins_tier_5", 7000, "$49.99"),
            CoinPack("coins_tier_6", 15000, "$99.99")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet & Balances", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Notification / Alert Banner
            AnimatedVisibility(visible = uiState is EconomyUiState.Success) {
                if (uiState is EconomyUiState.Success) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = (uiState as EconomyUiState.Success).message,
                            style = MaterialTheme.typography.bodyMedium.copy(color = SuccessColor),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState is EconomyUiState.Error) {
                if (uiState is EconomyUiState.Error) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = (uiState as EconomyUiState.Error).errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(color = ErrorColor),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. Coins Balance Card (Spent only on gifts - Non Withdrawable)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1B4B), // Dark Amber / Indigo tint
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coins Balance",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, color = TextPrimary)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Gifts Only",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "${wallet?.coinsBalance ?: 0} 🪙",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Used exclusively to send gifts and support creators in rooms. Non-withdrawable.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Earnings / Diamonds Balance Card (Eligible for Withdrawal only)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF064E3B), // Emerald / Cyan tint
                border = androidx.compose.foundation.BorderStroke(1.dp, Secondary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = Secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Creator Earnings",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, color = TextPrimary)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Secondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Withdrawable",
                                style = MaterialTheme.typography.labelSmall.copy(color = Secondary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val earnings = wallet?.earningsBalance ?: 0L
                    val estimatedUsd = earnings / 100.0

                    Text(
                        text = "$earnings 💎 (~$${String.format("%.2f", estimatedUsd)} USD)",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Accumulated exclusively from gifts received from other users. 100 Diamonds = $1.00 USD.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryVariant)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Payout / Withdrawal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Google Play In-App Purchase Coin Packs
            Text(
                text = "Buy Coins (Google Play Billing)",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)
            )
            Text(
                text = "Official in-app purchase verified directly by Google Play",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextSecondary),
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            coinPacks.chunked(2).forEach { rowPacks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPacks.forEach { pack ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceElevated.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${pack.coins} 🪙",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (context is Activity) {
                                            viewModel.purchaseCoinPack(context, pack.productId)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text(text = pack.price, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Withdrawal Request Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Request Earnings Withdrawal") },
            text = {
                Column {
                    Text(
                        text = "Enter the amount of diamonds to cash out (Minimum 1000 💎 = $10.00 USD).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = withdrawAmountText,
                        onValueChange = { withdrawAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount in Diamonds") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paymentMethodText,
                        onValueChange = { paymentMethodText = it },
                        label = { Text("Payment Method (e.g., Bank, PayPal)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountDetailsText,
                        onValueChange = { accountDetailsText = it },
                        label = { Text("Account Details / IBAN / Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmountText.toLongOrNull() ?: 0L
                        if (amount >= 1000) {
                            viewModel.requestWithdrawal(
                                amountDiamonds = amount,
                                paymentMethod = paymentMethodText,
                                paymentDetails = mapOf("details" to accountDetailsText)
                            )
                            showWithdrawDialog = false
                        }
                    }
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
