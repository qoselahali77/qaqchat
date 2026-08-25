package com.chatqaq.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatqaq.app.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    val status: String,
    val bio: String?,
    @ColumnInfo(name = "is_banned", defaultValue = "0")
    val isBanned: Boolean = false,
    @ColumnInfo(name = "created_at")
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

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            status = user.status,
            bio = user.bio,
            isBanned = user.isBanned,
            createdAt = user.createdAt
        )
    }
}
