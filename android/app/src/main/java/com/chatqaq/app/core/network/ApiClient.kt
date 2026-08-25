package com.chatqaq.app.core.network

import android.content.Context
import com.chatqaq.app.data.remote.AuthApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Production Server Base URL
    private const val BASE_URL = "https://qaq-tool.fun/qaqchat/"

    @Volatile
    private var authApiService: AuthApiService? = null

    fun getAuthApiService(context: Context): AuthApiService {
        return authApiService ?: synchronized(this) {
            val tokenManager = TokenManager(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(tokenManager))
                .authenticator(TokenAuthenticator(tokenManager, BASE_URL))
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(AuthApiService::class.java)
            authApiService = service
            service
        }
    }

    @Volatile
    private var economyApiService: com.chatqaq.app.data.remote.EconomyApiService? = null

    fun getEconomyApiService(context: Context): com.chatqaq.app.data.remote.EconomyApiService {
        return economyApiService ?: synchronized(this) {
            val tokenManager = TokenManager(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(tokenManager))
                .authenticator(TokenAuthenticator(tokenManager, BASE_URL))
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(com.chatqaq.app.data.remote.EconomyApiService::class.java)
            economyApiService = service
            service
        }
    }
}
