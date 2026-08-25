package com.chatqaq.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gifts")
data class GiftEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "animation_url")
    val animationUrl: String?,
    @ColumnInfo(name = "coin_price")
    val coinPrice: Long,
    @ColumnInfo(name = "diamond_reward")
    val diamondReward: Long,
    @ColumnInfo(name = "position", defaultValue = "0")
    val position: Int = 0
)
