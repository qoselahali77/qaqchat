package com.chatqaq.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatqaq.app.domain.model.User
import com.chatqaq.app.ui.auth.AuthViewModel
import com.chatqaq.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    viewModel: AuthViewModel,
    onNavigateToWallet: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var displayNameInput by remember(user) { mutableStateOf(user.displayName) }
    var bioInput by remember(user) { mutableStateOf(user.bio ?: "") }
    var selectedStatus by remember(user) { mutableStateOf(user.status) }

    val statusColor = when (user.status.lowercase()) {
        "online" -> StatusOnline
        "away" -> StatusAway
        "dnd" -> StatusDnd
        else -> StatusOffline
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = ErrorColor
                        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with Status Indicator
            Box(
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                // Status Badge Dot
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BackgroundDark)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
            )

            // Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceElevated.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                        )
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Primary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = BorderColor.copy(alpha = 0.4f)
                    )

                    if (isEditing) {
                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("Bio / Status Message") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    displayName = displayNameInput,
                                    status = selectedStatus,
                                    bio = bioInput
                                )
                                isEditing = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Save Changes")
                        }
                    } else {
                        ProfileInfoRow(label = "Email", value = user.email)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(label = "Status", value = user.status.replaceFirstChar { it.uppercase() })
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(label = "About Me", value = user.bio ?: "No bio provided yet")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet & Balances Shortcut Button
            Button(
                onClick = onNavigateToWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "My Wallet & Earnings (Coins / Diamonds)",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ready for Phase 2 Chat indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 Architecture Ready",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, color = Secondary)
                    )
                    Text(
                        text = "Authentication and User Base are fully established with Offline-First Room DB & Token Refresh. Next: Phase 2 Text Chat & WebSockets.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
