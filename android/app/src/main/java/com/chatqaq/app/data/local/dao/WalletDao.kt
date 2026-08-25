package com.chatqaq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatqaq.app.data.local.entity.GiftEntity
import com.chatqaq.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM user_wallets WHERE user_id = :userId LIMIT 1")
    fun observeWallet(userId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM user_wallets WHERE user_id = :userId LIMIT 1")
    suspend fun getWallet(userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: WalletEntity)

    @Query("SELECT * FROM gifts ORDER BY position ASC")
    fun observeGifts(): Flow<List<GiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGifts(gifts: List<GiftEntity>)

    @Query("DELETE FROM user_wallets")
    suspend fun clearWallet()
}
