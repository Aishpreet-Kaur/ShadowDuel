package com.example.shadowduel.data.database.dao

import androidx.room.*
import com.example.shadowduel.data.database.entities.GameSessionEntity

@Dao
interface GameSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GameSessionEntity): Long

    @Update
    suspend fun updateSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<GameSessionEntity>

    @Query("DELETE FROM game_sessions")
    suspend fun deleteAllSessions()
}