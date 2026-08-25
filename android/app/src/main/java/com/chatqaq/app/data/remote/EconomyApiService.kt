package com.chatqaq.app.data.remote

import com.chatqaq.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface EconomyApiService {

    @GET("api/v1/economy/wallet")
    suspend fun getWallet(): Response<WalletResponse>

    @GET("api/v1/economy/gifts")
    suspend fun getGifts(): Response<List<GiftDto>>

    @POST("api/v1/economy/gifts/send")
    suspend fun sendGift(
        @Body request: SendGiftRequest
    ): Response<SendGiftResponse>

    @POST("api/v1/economy/billing/google-play/verify")
    suspend fun verifyGooglePlayPurchase(
        @Body request: VerifyPurchaseRequest
    ): Response<VerifyPurchaseResponse>

    @POST("api/v1/economy/withdrawals")
    suspend fun requestWithdrawal(
        @Body request: CreateWithdrawalRequest
    ): Response<CreateWithdrawalResponse>
}
