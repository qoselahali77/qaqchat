package com.chatqaq.app.ui.economy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatqaq.app.data.local.entity.GiftEntity
import com.chatqaq.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftBottomSheet(
    receiverName: String,
    receiverId: String,
    roomId: String? = null,
    viewModel: WalletViewModel,
    onDismiss: () -> Unit
) {
    val wallet by viewModel.walletState.collectAsState()
    val gifts by viewModel.giftsState.collectAsState()
    var selectedGift by remember { mutableStateOf<GiftEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header: Send Gift to [Name]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Send Gift",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "To: $receiverName",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Primary)
                    )
                }

                // Current Coin Balance Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${wallet?.coinsBalance ?: 0} 🪙",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gifts Grid
            if (gifts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading gifts catalog...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(gifts) { gift ->
                        val isSelected = selectedGift?.id == gift.id
                        Surface(
                            modifier = Modifier
                                .clickable { selectedGift = gift },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Primary.copy(alpha = 0.25f) else SurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) Primary else BorderColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    contentDescription = gift.name,
                                    tint = if (isSelected) Primary else Color(0xFFFBBF24),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gift.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${gift.coinPrice} 🪙",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Send Button
            Button(
                onClick = {
                    selectedGift?.let { gift ->
                        viewModel.sendGift(
                            receiverId = receiverId,
                            giftId = gift.id,
                            roomId = roomId
                        )
                        onDismiss()
                    }
                },
                enabled = selectedGift != null && (wallet?.coinsBalance ?: 0) >= (selectedGift?.coinPrice ?: Long.MAX_VALUE),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (selectedGift == null) {
                    Text("Select a Gift to Send")
                } else if ((wallet?.coinsBalance ?: 0) < selectedGift!!.coinPrice) {
                    Text("Insufficient Coins (${selectedGift!!.coinPrice} required)")
                } else {
                    Text("Send ${selectedGift!!.name} (${selectedGift!!.coinPrice} 🪙)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
