package com.chatqaq.app.data.repository

import com.chatqaq.app.core.network.TokenManager
import com.chatqaq.app.data.local.dao.UserDao
import com.chatqaq.app.data.local.entity.UserEntity
import com.chatqaq.app.data.remote.AuthApiService
import com.chatqaq.app.data.remote.dto.*
import com.chatqaq.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val apiService: AuthApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) {

    fun observeCurrentUser(userId: String): Flow<User?> {
        return userDao.observeUserById(userId).map { it?.toDomain() }
    }

    suspend fun getCachedUser(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ): Result<User> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    email = email,
                    password = password,
                    displayName = "$firstName $lastName".trim()
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val user = authBody.user.toDomain()

                tokenManager.saveTokens(
                    accessToken = authBody.tokens.accessToken,
                    refreshToken = authBody.tokens.refreshToken,
                    userId = user.id
                )

                userDao.insertOrUpdate(UserEntity.fromDomain(user))
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(login: String, password: String): Result<User> {
        return try {
            val response = apiService.login(
                LoginRequest(
                    login = login,
                    password = password
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val user = authBody.user.toDomain()

                tokenManager.saveTokens(
                    accessToken = authBody.tokens.accessToken,
                    refreshToken = authBody.tokens.refreshToken,
                    userId = user.id
                )

                userDao.insertOrUpdate(UserEntity.fromDomain(user))
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val response = apiService.googleAuth(
                GoogleAuthRequest(idToken = idToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val user = authBody.user.toDomain()

                tokenManager.saveTokens(
                    accessToken = authBody.tokens.accessToken,
                    refreshToken = authBody.tokens.refreshToken,
                    userId = user.id
                )

                userDao.insertOrUpdate(UserEntity.fromDomain(user))
                Result.success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Google Sign-In failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.toDomain()
                userDao.insertOrUpdate(UserEntity.fromDomain(user))
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String? = null,
        avatarUrl: String? = null,
        status: String? = null,
        bio: String? = null
    ): Result<User> {
        return try {
            val response = apiService.updateProfile(
                UpdateUserRequest(
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    status = status,
                    bio = bio
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val updatedUser = response.body()!!.toDomain()
                userDao.insertOrUpdate(UserEntity.fromDomain(updatedUser))
                Result.success(updatedUser)
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                apiService.logout(RefreshTokenRequest(refreshToken))
            }
            tokenManager.clearTokens()
            userDao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            userDao.clearAll()
            Result.success(Unit)
        }
    }
}
