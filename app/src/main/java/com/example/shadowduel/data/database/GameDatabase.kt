package com.example.shadowduel.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.shadowduel.data.database.dao.GameSessionDao
import com.example.shadowduel.data.database.dao.PatternDao
import com.example.shadowduel.data.database.dao.PlayerMoveDao
import com.example.shadowduel.data.database.entities.DetectedPatternEntity
import com.example.shadowduel.data.database.entities.GameSessionEntity
import com.example.shadowduel.data.database.entities.PlayerMoveEntity

@Database(
    entities = [
        PlayerMoveEntity::class,
        DetectedPatternEntity::class,
        GameSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {

    abstract fun playerMoveDao(): PlayerMoveDao
    abstract fun patternDao(): PatternDao
    abstract fun gameSessionDao(): GameSessionDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "shadow_duel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}