package com.example.shadowduel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long = System.currentTimeMillis(),
    val totalRounds: Int = 0,
    val playerWins: Int = 0,
    val aiWins: Int = 0,
    val aiDifficultyLevel: String = "BEGINNER"
)