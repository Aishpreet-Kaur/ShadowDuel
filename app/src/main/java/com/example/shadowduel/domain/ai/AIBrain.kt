package com.example.shadowduel.domain.ai

import com.example.shadowduel.data.repository.GameRepository
import com.example.shadowduel.domain.model.AIPhase
import com.example.shadowduel.domain.model.GameState
import com.example.shadowduel.domain.model.Move
import kotlin.random.Random

class AIBrain(private val repository: GameRepository) {

    private val patternDetector = PatternDetector(repository)

    // Available moves for AI
    private val allMoves = listOf(
        Move.ATTACK_HIGH,
        Move.ATTACK_LOW,
        Move.BLOCK_HIGH,
        Move.BLOCK_LOW,
        Move.DODGE,
        Move.SPECIAL
    )

    /**
     * Main decision-making function
     * Returns the AI's next move based on current game state and learning phase
     */
    suspend fun decideNextMove(gameState: GameState): Move {
        val aiPhase = gameState.getAIDifficultyPhase()

        return when (aiPhase) {
            AIPhase.BEGINNER -> decideBeginnerMove(gameState)
            AIPhase.LEARNING -> decideLearningMove(gameState)
            AIPhase.ADAPTIVE -> decideAdaptiveMove(gameState)
            AIPhase.MASTER -> decideMasterMove(gameState)
        }
    }

    /**
     * PHASE 1: Beginner (Rounds 1-3)
     * - Plays mostly defensively
     * - 60% chance defensive moves
     * - Lets player win to build confidence
     */
    private fun decideBeginnerMove(gameState: GameState): Move {
        val random = Random.nextFloat()

        return when {
            random < 0.6f -> {
                // Play defensive
                listOf(Move.BLOCK_HIGH, Move.BLOCK_LOW, Move.DODGE).random()
            }
            random < 0.8f -> {
                // Basic attack
                listOf(Move.ATTACK_HIGH, Move.ATTACK_LOW).random()
            }
            else -> {
                // Rarely use special
                if (gameState.opponent.health > 50) Move.SPECIAL else Move.ATTACK_HIGH
            }
        }
    }

    /**
     * PHASE 2: Learning (Rounds 4-7)
     * - Starts detecting patterns
     * - 50/50 mix of random and pattern-based moves
     * - Begins to counter player's tendencies
     */
    private suspend fun decideLearningMove(gameState: GameState): Move {
        val patterns = patternDetector.detectPatterns(gameState)

        return if (patterns.isNotEmpty() && Random.nextFloat() < 0.5f) {
            // 50% chance to use pattern prediction
            val predictedPlayerMove = patterns.maxByOrNull { it.value }?.key
            if (predictedPlayerMove != null) {
                counterMove(predictedPlayerMove)
            } else {
                smartRandomMove(gameState)
            }
        } else {
            // 50% chance to play smart random
            smartRandomMove(gameState)
        }
    }

    /**
     * PHASE 3: Adaptive (Rounds 8-12)
     * - Actively exploits detected patterns
     * - 75% pattern-based decisions
     * - Uses counter-strategies
     */
    private suspend fun decideAdaptiveMove(gameState: GameState): Move {
        val patterns = patternDetector.detectPatterns(gameState)

        return if (patterns.isNotEmpty() && Random.nextFloat() < 0.75f) {
            // 75% chance to exploit patterns
            val predictedPlayerMove = patterns.maxByOrNull { it.value }?.key

            if (predictedPlayerMove != null) {
                val confidence = patterns[predictedPlayerMove] ?: 0f

                if (confidence > 0.7f) {
                    // High confidence - directly counter
                    counterMove(predictedPlayerMove)
                } else {
                    // Medium confidence - mix counter with aggression
                    if (Random.nextFloat() < 0.7f) {
                        counterMove(predictedPlayerMove)
                    } else {
                        aggressiveMove(gameState)
                    }
                }
            } else {
                aggressiveMove(gameState)
            }
        } else {
            smartRandomMove(gameState)
        }
    }

    /**
     * PHASE 4: Master (Rounds 13+)
     * - Nearly unbeatable
     * - 90% pattern exploitation
     * - Perfect counters and baiting
     */
    private suspend fun decideMasterMove(gameState: GameState): Move {
        val patterns = patternDetector.detectPatterns(gameState)

        if (patterns.isEmpty() || Random.nextFloat() > 0.9f) {
            return aggressiveMove(gameState)
        }

        // Get highest confidence prediction
        val (predictedMove, confidence) = patterns.maxByOrNull { it.value }
            ?: return aggressiveMove(gameState)

        return when {
            confidence > 0.8f -> {
                // Very high confidence - perfect counter
                counterMove(predictedMove)
            }
            confidence > 0.6f -> {
                // High confidence - counter or bait
                if (Random.nextFloat() < 0.8f) {
                    counterMove(predictedMove)
                } else {
                    // Bait: Do unexpected move to create new pattern
                    baitMove(predictedMove)
                }
            }
            else -> {
                // Medium confidence - aggressive play
                aggressiveMove(gameState)
            }
        }
    }

    /**
     * Returns a move that counters the predicted player move
     */
    private fun counterMove(predictedPlayerMove: Move): Move {
        return when (predictedPlayerMove) {
            Move.ATTACK_HIGH -> Move.BLOCK_HIGH
            Move.ATTACK_LOW -> Move.BLOCK_LOW
            Move.BLOCK_HIGH -> Move.ATTACK_LOW
            Move.BLOCK_LOW -> Move.ATTACK_HIGH
            Move.DODGE -> Move.SPECIAL // Special is hard to dodge
            Move.SPECIAL -> Move.DODGE // Dodge the slow special
            Move.IDLE -> Move.ATTACK_HIGH
        }
    }

    /**
     * Smart random move based on game state
     */
    private fun smartRandomMove(gameState: GameState): Move {
        return when {
            gameState.opponent.health < 30 -> {
                // AI is low health - play defensive
                listOf(Move.BLOCK_HIGH, Move.BLOCK_LOW, Move.DODGE).random()
            }
            gameState.player.health < 30 -> {
                // Player is low health - finish them
                listOf(Move.ATTACK_HIGH, Move.ATTACK_LOW, Move.SPECIAL).random()
            }
            else -> {
                // Balanced play
                allMoves.random()
            }
        }
    }

    /**
     * Aggressive move selection
     */
    private fun aggressiveMove(gameState: GameState): Move {
        return when {
            gameState.player.health < 40 && gameState.opponent.health > 40 -> {
                // Go for the kill
                if (Random.nextFloat() < 0.4f) Move.SPECIAL else
                    listOf(Move.ATTACK_HIGH, Move.ATTACK_LOW).random()
            }
            else -> {
                listOf(Move.ATTACK_HIGH, Move.ATTACK_LOW, Move.SPECIAL).random()
            }
        }
    }

    /**
     * Bait move - do something unexpected to manipulate player behavior
     */
    private fun baitMove(predictedPlayerMove: Move): Move {
        // Do opposite of what would counter them
        return when (predictedPlayerMove) {
            Move.ATTACK_HIGH -> Move.ATTACK_LOW // Attack instead of blocking
            Move.ATTACK_LOW -> Move.ATTACK_HIGH
            Move.BLOCK_HIGH -> Move.BLOCK_LOW // Block different area
            Move.BLOCK_LOW -> Move.BLOCK_HIGH
            else -> allMoves.random()
        }
    }

    /**
     * Get AI's confidence in its prediction (for UI display)
     */
    suspend fun getConfidenceLevel(gameState: GameState): Float {
        val patterns = patternDetector.detectPatterns(gameState)
        return patterns.values.maxOrNull() ?: 0f
    }

    /**
     * Get detected patterns for stats display
     */
    suspend fun getDetectedPatterns() = patternDetector.getHighConfidencePatterns()
}