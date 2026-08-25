package com.chatqaq.app.data.repository

import com.chatqaq.app.data.local.dao.WalletDao
import com.chatqaq.app.data.local.entity.GiftEntity
import com.chatqaq.app.data.local.entity.WalletEntity
import com.chatqaq.app.data.remote.EconomyApiService
import com.chatqaq.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EconomyRepository(
    private val apiService: EconomyApiService,
    private val walletDao: WalletDao
) {

    fun observeWallet(userId: String): Flow<WalletEntity?> {
        return walletDao.observeWallet(userId)
    }

    fun observeGifts(): Flow<List<GiftEntity>> {
        return walletDao.observeGifts()
    }

    suspend fun fetchWallet(userId: String): Result<WalletEntity> {
        return try {
            val response = apiService.getWallet()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val entity = WalletEntity(
                    userId = body.userId,
                    coinsBalance = body.coinsBalance.toLongOrNull() ?: 0L,
                    earningsBalance = body.earningsBalance.toLongOrNull() ?: 0L
                )
                walletDao.insertOrUpdateWallet(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception("Failed to fetch wallet: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchGifts(): Result<List<GiftEntity>> {
        return try {
            val response = apiService.getGifts()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.map {
                    GiftEntity(
                        id = it.id,
                        name = it.name,
                        iconUrl = it.iconUrl,
                        animationUrl = it.animationUrl,
                        coinPrice = it.coinPrice.toLongOrNull() ?: 0L,
                        diamondReward = it.diamondReward.toLongOrNull() ?: 0L,
                        position = it.position
                    )
                }
                walletDao.insertGifts(list)
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch gifts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendGift(
        userId: String,
        receiverId: String,
        giftId: String,
        roomId: String? = null
    ): Result<SendGiftResponse> {
        return try {
            val idempotencyKey = UUID.randomUUID().toString()
            val response = apiService.sendGift(
                SendGiftRequest(
                    receiverId = receiverId,
                    giftId = giftId,
                    roomId = roomId,
                    idempotencyKey = idempotencyKey
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Update local Room balance
                val current = walletDao.getWallet(userId)
                if (current != null) {
                    walletDao.insertOrUpdateWallet(
                        current.copy(coinsBalance = body.senderCoinsBalance.toLongOrNull() ?: current.coinsBalance)
                    )
                }
                Result.success(body)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to send gift"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyGooglePlayPurchase(
        userId: String,
        orderId: String,
        productId: String,
        purchaseToken: String
    ): Result<VerifyPurchaseResponse> {
        return try {
            val response = apiService.verifyGooglePlayPurchase(
                VerifyPurchaseRequest(
                    orderId = orderId,
                    productId = productId,
                    purchaseToken = purchaseToken
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val current = walletDao.getWallet(userId)
                if (current != null) {
                    walletDao.insertOrUpdateWallet(
                        current.copy(coinsBalance = body.newCoinsBalance.toLongOrNull() ?: current.coinsBalance)
                    )
                }
                Result.success(body)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Server verification failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        userId: String,
        amountDiamonds: Long,
        paymentMethod: String,
        paymentDetails: Map<String, Any>
    ): Result<CreateWithdrawalResponse> {
        return try {
            val response = apiService.requestWithdrawal(
                CreateWithdrawalRequest(
                    amountDiamonds = amountDiamonds,
                    paymentMethod = paymentMethod,
                    paymentDetails = paymentDetails
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val current = walletDao.getWallet(userId)
                if (current != null) {
                    walletDao.insertOrUpdateWallet(
                        current.copy(earningsBalance = body.remainingEarnings.toLongOrNull() ?: current.earningsBalance)
                    )
                }
                Result.success(body)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Withdrawal request failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
