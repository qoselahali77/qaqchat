package com.chatqaq.app.data.remote

import com.chatqaq.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/google")
    suspend fun googleAuth(
        @Body request: GoogleAuthRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<RefreshResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest? = null
    ): Response<GenericSuccessResponse>

    @GET("api/v1/users/me")
    suspend fun getProfile(): Response<UserDto>

    @PATCH("api/v1/users/me")
    suspend fun updateProfile(
        @Body request: UpdateUserRequest
    ): Response<UserDto>
}
