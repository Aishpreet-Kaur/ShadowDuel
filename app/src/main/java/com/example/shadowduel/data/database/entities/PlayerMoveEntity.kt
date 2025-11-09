package com.example.shadowduel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_moves")
data class PlayerMoveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val moveType: String,
    val playerHealthRange: String,
    val opponentHealthRange: String,
    val previousAiMove: String,
    val previousPlayerMove: String,
    val outcome: String, // "HIT", "BLOCKED", "MISSED"
    val roundNumber: Int,
    val gameSessionId: String
)