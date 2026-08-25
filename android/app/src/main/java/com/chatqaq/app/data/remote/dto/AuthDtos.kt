package com.chatqaq.app.data.remote.dto

import com.chatqaq.app.domain.model.User
import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("display_name")
    val displayName: String? = null
)

data class LoginRequest(
    val login: String,
    val password: String,
    @SerializedName("device_info")
    val deviceInfo: String? = "Android Native Client"
)

data class GoogleAuthRequest(
    @SerializedName("id_token")
    val idToken: String,
    @SerializedName("device_info")
    val deviceInfo: String? = "Android Google Sign-In"
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class UpdateUserRequest(
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    val status: String? = null,
    val bio: String? = null
)

data class TokensDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    val status: String,
    val bio: String?,
    @SerializedName("is_banned")
    val isBanned: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        username = username,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        status = status,
        bio = bio,
        isBanned = isBanned,
        createdAt = createdAt
    )
}

data class AuthResponse(
    val user: UserDto,
    val tokens: TokensDto
)

data class RefreshResponse(
    val tokens: TokensDto
)

data class GenericSuccessResponse(
    val success: Boolean,
    val message: String
)
