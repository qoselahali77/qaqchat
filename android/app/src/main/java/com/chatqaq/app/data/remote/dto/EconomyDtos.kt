package com.chatqaq.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("coins_balance")
    val coinsBalance: String,
    @SerializedName("earnings_balance")
    val earningsBalance: String
)

data class GiftDto(
    val id: String,
    val name: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("animation_url")
    val animationUrl: String?,
    @SerializedName("coin_price")
    val coinPrice: String,
    @SerializedName("diamond_reward")
    val diamondReward: String,
    val position: Int = 0
)

data class SendGiftRequest(
    @SerializedName("receiver_id")
    val receiverId: String,
    @SerializedName("gift_id")
    val giftId: String,
    @SerializedName("room_id")
    val roomId: String? = null,
    @SerializedName("idempotency_key")
    val idempotencyKey: String? = null
)

data class SendGiftResponse(
    val success: Boolean,
    @SerializedName("gift_transaction_id")
    val giftTransactionId: String,
    @SerializedName("sender_coins_balance")
    val senderCoinsBalance: String
)

data class VerifyPurchaseRequest(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("purchase_token")
    val purchaseToken: String
)

data class VerifyPurchaseResponse(
    val success: Boolean,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("coins_awarded")
    val coinsAwarded: Long,
    @SerializedName("new_coins_balance")
    val newCoinsBalance: String
)

data class CreateWithdrawalRequest(
    @SerializedName("amount_diamonds")
    val amountDiamonds: Long,
    @SerializedName("payment_method")
    val paymentMethod: String,
    @SerializedName("payment_details")
    val paymentDetails: Map<String, Any>
)

data class CreateWithdrawalResponse(
    val success: Boolean,
    @SerializedName("withdrawal_id")
    val withdrawalId: String,
    val status: String,
    @SerializedName("remaining_earnings")
    val remainingEarnings: String
)
