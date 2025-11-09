package com.example.shadowduel.data.repository

import com.example.shadowduel.data.database.GameDatabase
import com.example.shadowduel.data.database.entities.DetectedPatternEntity
import com.example.shadowduel.data.database.entities.GameSessionEntity
import com.example.shadowduel.data.database.entities.PlayerMoveEntity
import com.example.shadowduel.domain.model.GameState
import com.example.shadowduel.domain.model.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val database: GameDatabase) {

    // Player Moves
    suspend fun savePlayerMove(
        gameState: GameState,
        playerMove: Move,
        outcome: String
    ) = withContext(Dispatchers.IO) {
        val moveEntity = PlayerMoveEntity(
            moveType = playerMove.name,
            playerHealthRange = gameState.player.healthRange,
            opponentHealthRange = gameState.opponent.healthRange,
            previousAiMove = gameState.lastOpponentMove.name,
            previousPlayerMove = gameState.lastPlayerMove.name,
            outcome = outcome,
            roundNumber = gameState.roundNumber,
            gameSessionId = gameState.gameSessionId
        )
        database.playerMoveDao().insertMove(moveEntity)
    }

    suspend fun getLastNPlayerMoves(n: Int): List<PlayerMoveEntity> =
        withContext(Dispatchers.IO) {
            database.playerMoveDao().getLastNMoves(n)
        }

    suspend fun getMovesInSituation(
        healthRange: String,
        prevAiMove: String,
        limit: Int = 20
    ): List<PlayerMoveEntity> = withContext(Dispatchers.IO) {
        database.playerMoveDao().getMovesInSituation(healthRange, prevAiMove, limit)
    }

    suspend fun getMostFrequentMoveInHealthRange(healthRange: String): String? =
        withContext(Dispatchers.IO) {
            database.playerMoveDao().getMostFrequentMoveInHealthRange(healthRange)
        }

    // Patterns
    suspend fun savePattern(pattern: DetectedPatternEntity) = withContext(Dispatchers.IO) {
        val existing = database.patternDao().getPatternByName(pattern.patternName)
        if (existing != null) {
            database.patternDao().updatePattern(
                existing.copy(
                    confidenceScore = pattern.confidenceScore,
                    successCount = pattern.successCount,
                    failureCount = pattern.failureCount,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            database.patternDao().insertPattern(pattern)
        }
    }

    suspend fun getHighConfidencePatterns(minConfidence: Float = 0.6f): List<DetectedPatternEntity> =
        withContext(Dispatchers.IO) {
            database.patternDao().getHighConfidencePatterns(minConfidence)
        }

    suspend fun getAllPatterns(): List<DetectedPatternEntity> =
        withContext(Dispatchers.IO) {
            database.patternDao().getAllPatterns()
        }

    // Game Sessions
    suspend fun createSession(sessionId: String) = withContext(Dispatchers.IO) {
        val session = GameSessionEntity(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        database.gameSessionDao().insertSession(session)
    }

    suspend fun updateSession(session: GameSessionEntity) = withContext(Dispatchers.IO) {
        database.gameSessionDao().updateSession(session)
    }

    suspend fun getSession(sessionId: String): GameSessionEntity? =
        withContext(Dispatchers.IO) {
            database.gameSessionDao().getSession(sessionId)
        }

    // Clear data
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.playerMoveDao().deleteAllMoves()
        database.patternDao().deleteAllPatterns()
        database.gameSessionDao().deleteAllSessions()
    }
}