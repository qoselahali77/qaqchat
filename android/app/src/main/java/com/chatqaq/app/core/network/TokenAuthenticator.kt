package com.chatqaq.app.core.network

import com.chatqaq.app.data.remote.dto.RefreshTokenRequest
import com.chatqaq.app.data.remote.dto.RefreshResponse
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops if refresh endpoint itself returned 401
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) {
            runBlocking { tokenManager.clearTokens() }
            return null
        }

        // If we already attempted retry with the current token and it still failed, abort
        val currentToken = runBlocking { tokenManager.getAccessToken() }
        val headerToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (headerToken != null && headerToken != currentToken && currentToken != null) {
            // Another thread already refreshed the token; retry with current token
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return null

        // Perform synchronous refresh request using isolated client to avoid recursion
        val refreshRequestBody = gson.toJson(RefreshTokenRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/auth/refresh")
            .post(refreshRequestBody)
            .build()

        val refreshClient = OkHttpClient()

        try {
            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string()
                if (bodyString != null) {
                    val result = gson.fromJson(bodyString, RefreshResponse::class.java)
                    val newAccessToken = result.tokens.accessToken
                    val newRefreshToken = result.tokens.refreshToken

                    runBlocking {
                        tokenManager.saveTokens(newAccessToken, newRefreshToken)
                    }

                    // Retry the failed request with the new access token
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                }
            } else {
                // Refresh token is invalid/expired -> Clear session
                runBlocking { tokenManager.clearTokens() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
