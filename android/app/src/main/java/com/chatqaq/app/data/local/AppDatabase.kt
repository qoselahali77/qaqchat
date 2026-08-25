package com.chatqaq.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chatqaq.app.data.local.dao.UserDao
import com.chatqaq.app.data.local.dao.WalletDao
import com.chatqaq.app.data.local.entity.GiftEntity
import com.chatqaq.app.data.local.entity.UserEntity
import com.chatqaq.app.data.local.entity.WalletEntity

@Database(
    entities = [
        UserEntity::class,
        WalletEntity::class,
        GiftEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao

    companion object {
        private const val DATABASE_NAME = "chatqaq_local.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * STRICT RULE: Additive Migration only.
         * Creates user_wallets and gifts tables safely for existing users without deleting their user profile.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_wallets` (
                        `user_id` TEXT NOT NULL,
                        `coins_balance` INTEGER NOT NULL DEFAULT 0,
                        `earnings_balance` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`user_id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gifts` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon_url` TEXT NOT NULL,
                        `animation_url` TEXT,
                        `coin_price` INTEGER NOT NULL,
                        `diamond_reward` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
