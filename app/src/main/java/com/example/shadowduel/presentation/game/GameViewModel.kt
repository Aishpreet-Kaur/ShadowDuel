package com.example.shadowduel.presentation.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.shadowduel.data.database.GameDatabase
import com.example.shadowduel.data.repository.GameRepository
import com.example.shadowduel.domain.ai.AIBrain
import com.example.shadowduel.domain.model.Fighter
import com.example.shadowduel.domain.model.GameState
import com.example.shadowduel.domain.model.Move
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val repository = GameRepository(database)
    private val aiBrain = AIBrain(repository)

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _battleLog = MutableLiveData<String>()
    val battleLog: LiveData<String> = _battleLog

    private val _aiConfidence = MutableLiveData<Float>()
    val aiConfidence: LiveData<Float> = _aiConfidence

    private val _isProcessingMove = MutableLiveData<Boolean>()
    val isProcessingMove: LiveData<Boolean> = _isProcessingMove

    private val _roundWinner = MutableLiveData<String?>()
    val roundWinner: LiveData<String?> = _roundWinner

    private val _battleAnimation = MutableLiveData<BattleAnimationData?>()
    val battleAnimation: LiveData<BattleAnimationData?> = _battleAnimation

    init {
        startNewGame()
    }

    fun startNewGame() {
        val player = Fighter("Player", isPlayer = true)
        val opponent = Fighter("Shadow AI", isPlayer = false)
        val sessionId = System.currentTimeMillis().toString()

        _gameState.value = GameState(
            player = player,
            opponent = opponent,
            roundNumber = 1,
            playerScore = 0,
            opponentScore = 0,
            gameSessionId = sessionId
        )

        viewModelScope.launch {
            repository.createSession(sessionId)
        }

        _battleLog.value = "⚔️ Round 1 - FIGHT!"
        _roundWinner.value = null
        _isProcessingMove.value = false
    }

    fun playerMove(move: Move) {
        if (_isProcessingMove.value == true) return

        val currentState = _gameState.value ?: return
        if (!currentState.player.isAlive() || !currentState.opponent.isAlive()) return

        _isProcessingMove.value = true

        viewModelScope.launch {
            try {
                // Get AI's move INSTANTLY (no delay)
                val aiMove = aiBrain.decideNextMove(currentState)

                // Get AI confidence
                val confidence = aiBrain.getConfidenceLevel(currentState)
                _aiConfidence.postValue(confidence)

                // Show what's happening
                _battleLog.postValue("You: ${move.displayName} vs AI: ${aiMove.displayName}")

                // Determine battle result
                val battleResult = calculateBattleResult(move, aiMove)

                // Trigger battle animation (this runs for 2 seconds)
                _battleAnimation.postValue(
                    BattleAnimationData(
                        playerMove = move,
                        opponentMove = aiMove,
                        result = battleResult
                    )
                )

                // Apply damage immediately (during animation)
                applyBattleDamage(move, aiMove, battleResult)

                // Save player move to database (async, doesn't block)
                val outcome = when (battleResult) {
                    GameView.BattleResult.PLAYER_HIT -> "HIT"
                    GameView.BattleResult.OPPONENT_HIT -> "BLOCKED"
                    else -> "MISSED"
                }
                launch { repository.savePlayerMove(currentState, move, outcome) }

                // Update game state
                updateGameState(move, aiMove)

                // Wait ONLY for animation to complete (2 seconds)
                delay(2000)

                // Show result instantly
                showBattleResult(battleResult)

                // Check for round end immediately
                checkRoundEnd()

            } finally {
                _isProcessingMove.postValue(false)
                _battleAnimation.postValue(null)
            }
        }
    }

    private fun calculateBattleResult(playerMove: Move, aiMove: Move): GameView.BattleResult {
        val playerHits = playerMove.isAttack() && !aiMove.counters(playerMove)
        val aiHits = aiMove.isAttack() && !playerMove.counters(aiMove)

        return when {
            playerHits && aiHits -> GameView.BattleResult.BOTH_HIT
            playerHits -> GameView.BattleResult.PLAYER_HIT
            aiHits -> GameView.BattleResult.OPPONENT_HIT
            !playerMove.isAttack() && !aiMove.isAttack() -> GameView.BattleResult.BOTH_BLOCKED
            else -> GameView.BattleResult.NONE
        }
    }

    private fun applyBattleDamage(playerMove: Move, aiMove: Move, result: GameView.BattleResult) {
        val currentState = _gameState.value ?: return

        when (result) {
            GameView.BattleResult.PLAYER_HIT -> {
                currentState.opponent.takeDamage(playerMove.damage)
            }
            GameView.BattleResult.OPPONENT_HIT -> {
                currentState.player.takeDamage(aiMove.damage)
            }
            GameView.BattleResult.BOTH_HIT -> {
                currentState.player.takeDamage(aiMove.damage)
                currentState.opponent.takeDamage(playerMove.damage)
            }
            else -> {
                // No damage
            }
        }

        _gameState.postValue(currentState)
    }

    private fun showBattleResult(result: GameView.BattleResult) {
        val message = when (result) {
            GameView.BattleResult.PLAYER_HIT -> "💥 Direct hit! You damaged the AI!"
            GameView.BattleResult.OPPONENT_HIT -> "🛡️ AI blocked and countered!"
            GameView.BattleResult.BOTH_HIT -> "⚔️ Both fighters hit!"
            GameView.BattleResult.BOTH_BLOCKED -> "🤝 Both defended!"
            GameView.BattleResult.NONE -> "💨 Both missed!"
        }
        _battleLog.postValue(message)
    }

    private fun updateGameState(playerMove: Move, aiMove: Move) {
        val currentState = _gameState.value ?: return

        _gameState.postValue(
            currentState.copy(
                player = currentState.player,
                opponent = currentState.opponent,
                lastPlayerMove = playerMove,
                lastOpponentMove = aiMove
            )
        )
    }

    private fun checkRoundEnd() {
        val currentState = _gameState.value ?: return
        val player = currentState.player
        val opponent = currentState.opponent

        when {
            !player.isAlive() && !opponent.isAlive() -> {
                _roundWinner.postValue("Draw")
                _battleLog.postValue("💀 Double knockout!")
                scheduleNextRound()
            }
            !player.isAlive() -> {
                _roundWinner.postValue("AI")
                _battleLog.postValue("❌ Shadow AI wins!")
                val newState = currentState.copy(
                    opponentScore = currentState.opponentScore + 1
                )
                _gameState.postValue(newState)
                scheduleNextRound()
            }
            !opponent.isAlive() -> {
                _roundWinner.postValue("Player")
                _battleLog.postValue("🎉 You win!")
                val newState = currentState.copy(
                    playerScore = currentState.playerScore + 1
                )
                _gameState.postValue(newState)
                scheduleNextRound()
            }
        }
    }

    private fun scheduleNextRound() {
        viewModelScope.launch {
            // Only wait 1.5 seconds before next round
            delay(1500)
            startNextRound()
        }
    }

    private fun startNextRound() {
        val currentState = _gameState.value ?: return

        // Reset fighters
        currentState.player.reset()
        currentState.opponent.reset()

        val newState = currentState.copy(
            roundNumber = currentState.roundNumber + 1
        )

        _gameState.postValue(newState)
        _roundWinner.postValue(null)

        // Update AI phase message
        val phase = newState.getAIDifficultyPhase()
        _battleLog.postValue("⚔️ Round ${newState.roundNumber} - ${phase.description}!")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            val currentState = _gameState.value
            if (currentState != null) {
                val session = repository.getSession(currentState.gameSessionId)
                session?.let {
                    repository.updateSession(
                        it.copy(
                            totalRounds = currentState.roundNumber,
                            playerWins = currentState.playerScore,
                            aiWins = currentState.opponentScore,
                            aiDifficultyLevel = currentState.getAIDifficultyPhase().name
                        )
                    )
                }
            }
        }
    }
}

data class BattleAnimationData(
    val playerMove: Move,
    val opponentMove: Move,
    val result: GameView.BattleResult
)