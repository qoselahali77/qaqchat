package com.chatqaq.app.domain.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val status: String,
    val bio: String?,
    val isBanned: Boolean = false,
    val createdAt: String? = null
)
