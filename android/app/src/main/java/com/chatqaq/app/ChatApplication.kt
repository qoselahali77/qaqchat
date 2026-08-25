package com.chatqaq.app

import android.app.Application
import com.chatqaq.app.core.network.ApiClient
import com.chatqaq.app.core.network.TokenManager
import com.chatqaq.app.data.local.AppDatabase
import com.chatqaq.app.data.repository.AuthRepository

class ChatApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var economyRepository: com.chatqaq.app.data.repository.EconomyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        tokenManager = TokenManager(this)

        val apiService = ApiClient.getAuthApiService(this)
        authRepository = AuthRepository(
            apiService = apiService,
            userDao = database.userDao(),
            tokenManager = tokenManager
        )

        val economyApiService = ApiClient.getEconomyApiService(this)
        economyRepository = com.chatqaq.app.data.repository.EconomyRepository(
            apiService = economyApiService,
            walletDao = database.walletDao()
        )
    }

    companion object {
        lateinit var instance: ChatApplication
            private set
    }
}
