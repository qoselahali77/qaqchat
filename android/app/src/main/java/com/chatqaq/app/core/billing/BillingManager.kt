package com.chatqaq.app.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface PurchaseResult {
    data class Success(val orderId: String, val productId: String, val purchaseToken: String) : PurchaseResult
    data class Error(val message: String) : PurchaseResult
    data object UserCancelled : PurchaseResult
}

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>()
    val purchaseEvents: SharedFlow<PurchaseResult> = _purchaseEvents.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var isConnected = false

    fun startConnection(onReady: (() -> Unit)? = null) {
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        onReady?.invoke()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
        } catch (e: Exception) {
            isConnected = false
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        if (!isConnected) {
            startConnection { launchPurchase(activity, productId) }
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                coroutineScope.launch {
                    _purchaseEvents.emit(PurchaseResult.Error("Product not found: ${billingResult.debugMessage}"))
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val orderId = purchase.orderId ?: purchase.purchaseToken.take(16)
                        val productId = purchase.products.firstOrNull() ?: "unknown"
                        val purchaseToken = purchase.purchaseToken

                        coroutineScope.launch {
                            // Forward purchase details to backend for server-side verification
                            _purchaseEvents.emit(
                                PurchaseResult.Success(
                                    orderId = orderId,
                                    productId = productId,
                                    purchaseToken = purchaseToken
                                )
                            )
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                coroutineScope.launch {
                    _purchaseEvents.emit(PurchaseResult.UserCancelled)
                }
            }
            else -> {
                coroutineScope.launch {
                    _purchaseEvents.emit(PurchaseResult.Error("Purchase failed: ${billingResult.debugMessage}"))
                }
            }
        }
    }
}
