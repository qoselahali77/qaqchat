package com.chatqaq.app.ui.economy

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatqaq.app.core.billing.BillingManager
import com.chatqaq.app.core.billing.PurchaseResult
import com.chatqaq.app.data.local.entity.GiftEntity
import com.chatqaq.app.data.local.entity.WalletEntity
import com.chatqaq.app.data.repository.EconomyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface EconomyUiState {
    data object Idle : EconomyUiState
    data object Loading : EconomyUiState
    data class Success(val message: String) : EconomyUiState
    data class Error(val errorMessage: String) : EconomyUiState
}

class WalletViewModel(
    private val userId: String,
    private val economyRepository: EconomyRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    val walletState: StateFlow<WalletEntity?> = economyRepository.observeWallet(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val giftsState: StateFlow<List<GiftEntity>> = economyRepository.observeGifts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<EconomyUiState>(EconomyUiState.Idle)
    val uiState: StateFlow<EconomyUiState> = _uiState.asStateFlow()

    init {
        refreshWalletAndGifts()
        observeBillingEvents()
    }

    fun refreshWalletAndGifts() {
        viewModelScope.launch {
            economyRepository.fetchWallet(userId)
            economyRepository.fetchGifts()
        }
    }

    private fun observeBillingEvents() {
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { result ->
                when (result) {
                    is PurchaseResult.Success -> {
                        _uiState.value = EconomyUiState.Loading
                        val verifyResult = economyRepository.verifyGooglePlayPurchase(
                            userId = userId,
                            orderId = result.orderId,
                            productId = result.productId,
                            purchaseToken = result.purchaseToken
                        )
                        verifyResult.fold(
                            onSuccess = { res ->
                                _uiState.value = EconomyUiState.Success("Successfully added ${res.coinsAwarded} Coins!")
                            },
                            onFailure = { err ->
                                _uiState.value = EconomyUiState.Error(err.localizedMessage ?: "Purchase verification failed")
                            }
                        )
                    }
                    is PurchaseResult.Error -> {
                        _uiState.value = EconomyUiState.Error(result.message)
                    }
                    PurchaseResult.UserCancelled -> {
                        _uiState.value = EconomyUiState.Idle
                    }
                }
            }
        }
    }

    fun purchaseCoinPack(activity: Activity, productId: String) {
        billingManager.launchPurchase(activity, productId)
    }

    fun sendGift(receiverId: String, giftId: String, roomId: String? = null) {
        viewModelScope.launch {
            _uiState.value = EconomyUiState.Loading
            val result = economyRepository.sendGift(userId, receiverId, giftId, roomId)
            result.fold(
                onSuccess = {
                    _uiState.value = EconomyUiState.Success("Gift sent successfully! 🎉")
                },
                onFailure = { err ->
                    _uiState.value = EconomyUiState.Error(err.localizedMessage ?: "Failed to send gift")
                }
            )
        }
    }

    fun requestWithdrawal(amountDiamonds: Long, paymentMethod: String, paymentDetails: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.value = EconomyUiState.Loading
            val result = economyRepository.requestWithdrawal(userId, amountDiamonds, paymentMethod, paymentDetails)
            result.fold(
                onSuccess = {
                    _uiState.value = EconomyUiState.Success("Withdrawal request submitted for review!")
                },
                onFailure = { err ->
                    _uiState.value = EconomyUiState.Error(err.localizedMessage ?: "Failed to submit withdrawal")
                }
            )
        }
    }

    fun clearUiState() {
        _uiState.value = EconomyUiState.Idle
    }

    class Factory(
        private val userId: String,
        private val economyRepository: EconomyRepository,
        private val billingManager: BillingManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(userId, economyRepository, billingManager) as T
        }
    }
}
