package com.example.shadowduel.domain.model

data class GameState(
    val player: Fighter,
    val opponent: Fighter,
    val roundNumber: Int = 1,
    val playerScore: Int = 0,
    val opponentScore: Int = 0,
    val lastPlayerMove: Move = Move.IDLE,
    val lastOpponentMove: Move = Move.IDLE,
    val gameSessionId: String = System.currentTimeMillis().toString()
) {
    fun getAIDifficultyPhase(): AIPhase {
        return when {
            roundNumber <= 3 -> AIPhase.BEGINNER
            roundNumber <= 7 -> AIPhase.LEARNING
            roundNumber <= 12 -> AIPhase.ADAPTIVE
            else -> AIPhase.MASTER
        }
    }

    fun copy(
        player: Fighter = this.player,
        opponent: Fighter = this.opponent,
        roundNumber: Int = this.roundNumber,
        playerScore: Int = this.playerScore,
        opponentScore: Int = this.opponentScore,
        lastPlayerMove: Move = this.lastPlayerMove,
        lastOpponentMove: Move = this.lastOpponentMove
    ) = GameState(
        player = player,
        opponent = opponent,
        roundNumber = roundNumber,
        playerScore = playerScore,
        opponentScore = opponentScore,
        lastPlayerMove = lastPlayerMove,
        lastOpponentMove = lastOpponentMove,
        gameSessionId = this.gameSessionId
    )
}

enum class AIPhase(val description: String, val winRateTarget: Float) {
    BEGINNER("Learning Your Style", 0.3f),
    LEARNING("Detecting Patterns", 0.5f),
    ADAPTIVE("Exploiting Weaknesses", 0.75f),
    MASTER("Unbeatable", 0.9f)
}