package com.chatqaq.app

import android.app.Application
import com.chatqaq.app.core.network.ApiClient
import com.chatqaq.app.core.network.TokenManager
import com.chatqaq.app.data.local.AppDatabase
import com.chatqaq.app.data.repository.AuthRepository
import com.chatqaq.app.data.repository.EconomyRepository

class ChatApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    val tokenManager: TokenManager by lazy {
        TokenManager(applicationContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            apiService = ApiClient.getAuthApiService(applicationContext),
            userDao = database.userDao(),
            tokenManager = tokenManager
        )
    }

    val economyRepository: EconomyRepository by lazy {
        EconomyRepository(
            apiService = ApiClient.getEconomyApiService(applicationContext),
            walletDao = database.walletDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ChatApplication
            private set
    }
}
