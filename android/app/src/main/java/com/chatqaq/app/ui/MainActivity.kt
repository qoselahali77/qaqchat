package com.chatqaq.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chatqaq.app.ChatApplication
import com.chatqaq.app.core.billing.BillingManager
import com.chatqaq.app.domain.model.AuthState
import com.chatqaq.app.ui.auth.AuthViewModel
import com.chatqaq.app.ui.auth.LoginScreen
import com.chatqaq.app.ui.auth.RegisterScreen
import com.chatqaq.app.ui.economy.WalletScreen
import com.chatqaq.app.ui.economy.WalletViewModel
import com.chatqaq.app.ui.profile.ProfileScreen
import com.chatqaq.app.ui.theme.BackgroundDark
import com.chatqaq.app.ui.theme.ChatQAQTheme
import com.chatqaq.app.ui.theme.Primary
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        val app = application as ChatApplication
        AuthViewModel.Factory(app.authRepository, app.tokenManager)
    }

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingManager = BillingManager(this, lifecycleScope)
        billingManager.startConnection()

        setContent {
            ChatQAQTheme {
                val authState by authViewModel.authState.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.MainProfile) }

                when (val state = authState) {
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundDark),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }

                    is AuthState.Authenticated -> {
                        when (currentScreen) {
                            Screen.MainProfile -> {
                                ProfileScreen(
                                    user = state.user,
                                    viewModel = authViewModel,
                                    onNavigateToWallet = { currentScreen = Screen.Wallet }
                                )
                            }
                            Screen.Wallet -> {
                                val app = application as ChatApplication
                                val walletViewModel = remember(state.user.id) {
                                    WalletViewModel(
                                        userId = state.user.id,
                                        economyRepository = app.economyRepository,
                                        billingManager = billingManager
                                    )
                                }
                                WalletScreen(
                                    viewModel = walletViewModel,
                                    onNavigateBack = { currentScreen = Screen.MainProfile }
                                )
                            }
                            else -> {
                                currentScreen = Screen.MainProfile
                            }
                        }
                    }

                    is AuthState.Unauthenticated, is AuthState.Error -> {
                        when (currentScreen) {
                            Screen.Register -> {
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = { currentScreen = Screen.Login }
                                )
                            }
                            else -> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRegister = { currentScreen = Screen.Register }
                                )
                            }
                        }
                    }

                    AuthState.Idle -> {
                        // Handled by Loading
                    }
                }
            }
        }
    }

    sealed interface Screen {
        data object Login : Screen
        data object Register : Screen
        data object MainProfile : Screen
        data object Wallet : Screen
    }
}
