//package com.example.shadowduel.presentation.game
//
//import android.os.Bundle
//import android.view.View
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import com.example.shadowduel.databinding.ActivityGameBinding
//import com.example.shadowduel.domain.model.Move
//
//class GameActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityGameBinding
//    private val viewModel: GameViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityGameBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupObservers()
//        setupButtons()
//    }
//
//    private fun setupObservers() {
//        // Observe game state
//        viewModel.gameState.observe(this) { gameState ->
//            // Update game view
//            binding.gameView.updateFighters(gameState.player, gameState.opponent)
//
//            // Update round info
//            val phase = gameState.getAIDifficultyPhase()
//            binding.tvRoundInfo.text = "Round ${gameState.roundNumber} - ${phase.description}"
//
//            // Update score
//            binding.tvScore.text = "Player: ${gameState.playerScore}  |  AI: ${gameState.opponentScore}"
//        }
//
//        // Observe battle log
//        viewModel.battleLog.observe(this) { log ->
//            binding.tvBattleLog.text = log
//        }
//
//        // Observe AI confidence
//        viewModel.aiConfidence.observe(this) { confidence ->
//            val percentage = (confidence * 100).toInt()
//            binding.tvAiConfidence.text = "AI Prediction Confidence: $percentage%"
//            binding.progressAiConfidence.progress = percentage
//
//            // Change color based on confidence
//            val color = when {
//                percentage > 70 -> android.graphics.Color.parseColor("#ff0055")
//                percentage > 40 -> android.graphics.Color.parseColor("#ffcc00")
//                else -> android.graphics.Color.parseColor("#00ff88")
//            }
//            binding.progressAiConfidence.progressTintList = android.content.res.ColorStateList.valueOf(color)
//        }
//
//        // Observe processing state
//        viewModel.isProcessingMove.observe(this) { isProcessing ->
//            setButtonsEnabled(!isProcessing)
//            binding.loadingOverlay.visibility = if (isProcessing) View.VISIBLE else View.GONE
//        }
//
//        // Observe AI thinking
//        viewModel.aiThinking.observe(this) { isThinking ->
//            binding.tvAiThinking.visibility = if (isThinking) View.VISIBLE else View.GONE
//        }
//
//        // Observe round winner
//        viewModel.roundWinner.observe(this) { winner ->
//            winner?.let {
//                binding.tvRoundWinner.visibility = View.VISIBLE
//                binding.tvRoundWinner.text = when (it) {
//                    "Player" -> "🏆 YOU WON THE ROUND! 🏆"
//                    "AI" -> "💀 AI WON THE ROUND 💀"
//                    "Draw" -> "⚖️ DRAW ⚖️"
//                    else -> ""
//                }
//
//                // Hide after a delay
//                binding.tvRoundWinner.postDelayed({
//                    binding.tvRoundWinner.visibility = View.GONE
//                }, 2500)
//            }
//        }
//
//        // Observe battle animation
//        viewModel.battleAnimation.observe(this) { animationData ->
//            animationData?.let {
//                binding.gameView.playBattleAnimation(
//                    playerMove = it.playerMove,
//                    opponentMove = it.opponentMove,
//                    result = it.result,
//                    onComplete = {
//                        // Animation completed
//                    }
//                )
//            }
//        }
//    }
//
//    private fun setupButtons() {
//        binding.btnAttackHigh.setOnClickListener {
//            viewModel.playerMove(Move.ATTACK_HIGH)
//        }
//
//        binding.btnAttackLow.setOnClickListener {
//            viewModel.playerMove(Move.ATTACK_LOW)
//        }
//
//        binding.btnBlockHigh.setOnClickListener {
//            viewModel.playerMove(Move.BLOCK_HIGH)
//        }
//
//        binding.btnBlockLow.setOnClickListener {
//            viewModel.playerMove(Move.BLOCK_LOW)
//        }
//
//        binding.btnDodge.setOnClickListener {
//            viewModel.playerMove(Move.DODGE)
//        }
//
//        binding.btnSpecial.setOnClickListener {
//            viewModel.playerMove(Move.SPECIAL)
//        }
//
//        binding.btnRestart.setOnClickListener {
//            viewModel.startNewGame()
//        }
//    }
//
//    private fun setButtonsEnabled(enabled: Boolean) {
//        binding.btnAttackHigh.isEnabled = enabled
//        binding.btnAttackLow.isEnabled = enabled
//        binding.btnBlockHigh.isEnabled = enabled
//        binding.btnBlockLow.isEnabled = enabled
//        binding.btnDodge.isEnabled = enabled
//        binding.btnSpecial.isEnabled = enabled
//    }
//}

package com.example.shadowduel.presentation.game

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.shadowduel.databinding.ActivityGameBinding
import com.example.shadowduel.domain.model.Move
import com.example.shadowduel.presentation.GameOverActivity

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    companion object {
        const val ROUNDS_TO_WIN = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupButtons()
    }

    private fun setupObservers() {
        // Observe game state
        viewModel.gameState.observe(this) { gameState ->
            // Update game view
            binding.gameView.updateFighters(gameState.player, gameState.opponent)

            // Update round info
            val phase = gameState.getAIDifficultyPhase()
            binding.tvRoundInfo.text = "Round ${gameState.roundNumber} - ${phase.description}"

            // Update score
            binding.tvScore.text = "Player: ${gameState.playerScore}  |  AI: ${gameState.opponentScore}"

            // Check if game is over
            if (gameState.playerScore >= ROUNDS_TO_WIN || gameState.opponentScore >= ROUNDS_TO_WIN) {
                finishGame(gameState.playerScore, gameState.opponentScore)
            }
        }

        // Observe battle log
        viewModel.battleLog.observe(this) { log ->
            binding.tvBattleLog.text = log
        }

        // Observe AI confidence
        viewModel.aiConfidence.observe(this) { confidence ->
            val percentage = (confidence * 100).toInt()
            binding.tvAiConfidence.text = "AI Prediction Confidence: $percentage%"
            binding.progressAiConfidence.progress = percentage

            // Change color based on confidence
            val color = when {
                percentage > 70 -> android.graphics.Color.parseColor("#ff0055")
                percentage > 40 -> android.graphics.Color.parseColor("#ffcc00")
                else -> android.graphics.Color.parseColor("#00ff88")
            }
            binding.progressAiConfidence.progressTintList = android.content.res.ColorStateList.valueOf(color)
        }

        // Observe processing state
        viewModel.isProcessingMove.observe(this) { isProcessing ->
            setButtonsEnabled(!isProcessing)
            binding.loadingOverlay.visibility = if (isProcessing) View.VISIBLE else View.GONE
        }

        // Observe round winner
        viewModel.roundWinner.observe(this) { winner ->
            winner?.let {
                binding.tvRoundWinner.visibility = View.VISIBLE
                binding.tvRoundWinner.text = when (it) {
                    "Player" -> "🏆 YOU WON THE ROUND! 🏆"
                    "AI" -> "💀 AI WON THE ROUND 💀"
                    "Draw" -> "⚖️ DRAW ⚖️"
                    else -> ""
                }

                // Hide after a delay
                binding.tvRoundWinner.postDelayed({
                    binding.tvRoundWinner.visibility = View.GONE
                }, 1500)
            }
        }

        // Observe battle animation
        viewModel.battleAnimation.observe(this) { animationData ->
            animationData?.let {
                binding.gameView.playBattleAnimation(
                    playerMove = it.playerMove,
                    opponentMove = it.opponentMove,
                    result = it.result,
                    onComplete = {}
                )
            }
        }
    }

    private fun setupButtons() {
        binding.btnAttackHigh.setOnClickListener {
            viewModel.playerMove(Move.ATTACK_HIGH)
        }

        binding.btnAttackLow.setOnClickListener {
            viewModel.playerMove(Move.ATTACK_LOW)
        }

        binding.btnBlockHigh.setOnClickListener {
            viewModel.playerMove(Move.BLOCK_HIGH)
        }

        binding.btnBlockLow.setOnClickListener {
            viewModel.playerMove(Move.BLOCK_LOW)
        }

        binding.btnDodge.setOnClickListener {
            viewModel.playerMove(Move.DODGE)
        }

        binding.btnSpecial.setOnClickListener {
            viewModel.playerMove(Move.SPECIAL)
        }

        binding.btnQuit.setOnClickListener {
            finish()
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnAttackHigh.isEnabled = enabled
        binding.btnAttackLow.isEnabled = enabled
        binding.btnBlockHigh.isEnabled = enabled
        binding.btnBlockLow.isEnabled = enabled
        binding.btnDodge.isEnabled = enabled
        binding.btnSpecial.isEnabled = enabled
    }

    private fun finishGame(playerScore: Int, aiScore: Int) {
        // Navigate to Game Over screen
        val intent = Intent(this, GameOverActivity::class.java).apply {
            putExtra("PLAYER_SCORE", playerScore)
            putExtra("AI_SCORE", aiScore)
            putExtra("TOTAL_ROUNDS", viewModel.gameState.value?.roundNumber ?: 0)
            putExtra("AI_CONFIDENCE", (viewModel.aiConfidence.value ?: 0f) * 100)
        }
        startActivity(intent)
        finish()
    }
}