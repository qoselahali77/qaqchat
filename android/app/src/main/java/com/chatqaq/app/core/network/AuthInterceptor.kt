package com.chatqaq.app.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip adding token for public auth endpoints
        val url = request.url.encodedPath
        if (url.endsWith("/auth/login") || url.endsWith("/auth/register") || url.endsWith("/auth/refresh")) {
            return chain.proceed(request)
        }

        val token = runBlocking { tokenManager.getAccessToken() }
        val newRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}
