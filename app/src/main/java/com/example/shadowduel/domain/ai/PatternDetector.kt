package com.example.shadowduel.domain.ai

import com.example.shadowduel.data.database.entities.DetectedPatternEntity
import com.example.shadowduel.data.database.entities.PlayerMoveEntity
import com.example.shadowduel.data.repository.GameRepository
import com.example.shadowduel.domain.model.GameState
import com.example.shadowduel.domain.model.Move

class PatternDetector(private val repository: GameRepository) {

    /**
     * Analyzes player behavior and detects patterns
     * Returns a confidence score (0.0 to 1.0) for predicted moves
     */
    suspend fun detectPatterns(gameState: GameState): Map<Move, Float> {
        val predictions = mutableMapOf<Move, Float>()

        // Pattern 1: Health-based behavior
        val healthPattern = detectHealthBasedPattern(gameState.player.healthRange)
        if (healthPattern != null) {
            predictions[healthPattern.first] = healthPattern.second
        }

        // Pattern 2: After taking damage
        val damagePattern = detectAfterDamagePattern(gameState)
        if (damagePattern != null) {
            predictions[damagePattern.first] =
                predictions.getOrDefault(damagePattern.first, 0f) + damagePattern.second
        }

        // Pattern 3: Move sequences (combo patterns)
        val sequencePattern = detectMoveSequencePattern()
        if (sequencePattern != null) {
            predictions[sequencePattern.first] =
                predictions.getOrDefault(sequencePattern.first, 0f) + sequencePattern.second
        }

        // Pattern 4: Reaction to specific AI moves
        val reactionPattern = detectReactionPattern(gameState.lastOpponentMove)
        if (reactionPattern != null) {
            predictions[reactionPattern.first] =
                predictions.getOrDefault(reactionPattern.first, 0f) + reactionPattern.second
        }

        // Normalize scores to 0-1 range
        val maxScore = predictions.values.maxOrNull() ?: 1f
        return predictions.mapValues { (it.value / maxScore).coerceIn(0f, 1f) }
    }

    /**
     * Pattern 1: What does player do at specific health ranges?
     * Example: "Player blocks high 80% of the time when health < 30%"
     */
    private suspend fun detectHealthBasedPattern(healthRange: String): Pair<Move, Float>? {
        val moveName = repository.getMostFrequentMoveInHealthRange(healthRange) ?: return null

        val moves = repository.getMovesInSituation(healthRange, "", limit = 30)
        if (moves.size < 5) return null // Need at least 5 samples

        val targetMoveCount = moves.count { it.moveType == moveName }
        val confidence = targetMoveCount.toFloat() / moves.size

        // Save pattern to database
        if (confidence > 0.6f) {
            val pattern = DetectedPatternEntity(
                patternName = "health_${healthRange}_${moveName}",
                condition = "health_range:$healthRange",
                predictedMove = moveName,
                confidenceScore = confidence,
                successCount = targetMoveCount,
                failureCount = moves.size - targetMoveCount
            )
            repository.savePattern(pattern)
        }

        return try {
            Pair(Move.valueOf(moveName), confidence)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pattern 2: What does player do right after taking damage?
     */
    private suspend fun detectAfterDamagePattern(gameState: GameState): Pair<Move, Float>? {
        val recentMoves = repository.getLastNPlayerMoves(20)

        // Find moves where player took damage (outcome = "HIT")
        val movesAfterDamage = recentMoves
            .zipWithNext()
            .filter { (prev, _) -> prev.outcome == "HIT" }
            .map { (_, next) -> next.moveType }

        if (movesAfterDamage.size < 3) return null

        // Find most common move after taking damage
        val moveFrequency = movesAfterDamage.groupingBy { it }.eachCount()
        val mostCommon = moveFrequency.maxByOrNull { it.value } ?: return null

        val confidence = mostCommon.value.toFloat() / movesAfterDamage.size

        if (confidence > 0.5f) {
            val pattern = DetectedPatternEntity(
                patternName = "after_damage_${mostCommon.key}",
                condition = "after_taking_damage",
                predictedMove = mostCommon.key,
                confidenceScore = confidence,
                successCount = mostCommon.value,
                failureCount = movesAfterDamage.size - mostCommon.value
            )
            repository.savePattern(pattern)
        }

        return try {
            Pair(Move.valueOf(mostCommon.key), confidence)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pattern 3: Move sequences (player tends to do X after Y)
     */
    private suspend fun detectMoveSequencePattern(): Pair<Move, Float>? {
        val recentMoves = repository.getLastNPlayerMoves(15)
        if (recentMoves.size < 5) return null

        // Look for A -> B -> ? patterns
        val sequences = recentMoves
            .windowed(3, 1)
            .map { Triple(it[0].moveType, it[1].moveType, it[2].moveType) }

        if (sequences.isEmpty()) return null

        // Find most common 2-move sequence and its follow-up
        val sequenceMap = sequences.groupingBy { it }.eachCount()
        val mostCommon = sequenceMap.maxByOrNull { it.value } ?: return null

        val confidence = mostCommon.value.toFloat() / sequences.size

        return try {
            if (confidence > 0.4f) {
                Pair(Move.valueOf(mostCommon.key.third), confidence)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pattern 4: How does player react to specific AI moves?
     */
    private suspend fun detectReactionPattern(lastAiMove: Move): Pair<Move, Float>? {
        if (lastAiMove == Move.IDLE) return null

        val reactions = repository.getMovesInSituation("", lastAiMove.name, limit = 20)
        if (reactions.size < 3) return null

        val moveFrequency = reactions.groupingBy { it.moveType }.eachCount()
        val mostCommon = moveFrequency.maxByOrNull { it.value } ?: return null

        val confidence = mostCommon.value.toFloat() / reactions.size

        if (confidence > 0.5f) {
            val pattern = DetectedPatternEntity(
                patternName = "reaction_to_${lastAiMove.name}",
                condition = "ai_move:${lastAiMove.name}",
                predictedMove = mostCommon.key,
                confidenceScore = confidence,
                successCount = mostCommon.value,
                failureCount = reactions.size - mostCommon.value
            )
            repository.savePattern(pattern)
        }

        return try {
            Pair(Move.valueOf(mostCommon.key), confidence)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all detected patterns with high confidence
     */
    suspend fun getHighConfidencePatterns(): List<DetectedPatternEntity> {
        return repository.getHighConfidencePatterns(minConfidence = 0.6f)
    }
}