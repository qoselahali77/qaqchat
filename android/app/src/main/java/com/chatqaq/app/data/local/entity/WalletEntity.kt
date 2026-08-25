package com.chatqaq.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallets")
data class WalletEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "coins_balance", defaultValue = "0")
    val coinsBalance: Long = 0,

    @ColumnInfo(name = "earnings_balance", defaultValue = "0")
    val earningsBalance: Long = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
