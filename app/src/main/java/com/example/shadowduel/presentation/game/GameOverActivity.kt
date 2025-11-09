package com.example.shadowduel.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shadowduel.databinding.ActivityGameOverBinding
import com.example.shadowduel.presentation.game.GameActivity
import com.example.shadowduel.presentation.game.MainActivity

class GameOverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameOverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameOverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayResults()
        setupButtons()
    }

    private fun displayResults() {
        // Get data from intent
        val playerScore = intent.getIntExtra("PLAYER_SCORE", 0)
        val aiScore = intent.getIntExtra("AI_SCORE", 0)
        val totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 0)
        val aiConfidence = intent.getFloatExtra("AI_CONFIDENCE", 0f)

        // Determine winner
        val playerWon = playerScore > aiScore

        // Set title and emoji
        if (playerWon) {
            binding.tvResultTitle.text = "VICTORY!"
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_green_light))
            binding.tvResultEmoji.text = "🏆"
        } else {
            binding.tvResultTitle.text = "DEFEAT"
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_red_light))
            binding.tvResultEmoji.text = "💀"
        }

        // Set score
        binding.tvFinalScore.text = "PLAYER $playerScore - $aiScore AI"

        // Set total rounds
        binding.tvTotalRounds.text = totalRounds.toString()

        // Set AI confidence
        binding.tvAiConfidence.text = "${aiConfidence.toInt()}%"

        // Set learning status message
        val learningMessage = when {
            aiConfidence > 70 -> "🧠 AI mastered your combat style!"
            aiConfidence > 40 -> "🤖 AI learned several of your patterns!"
            else -> "💭 AI is still learning your moves!"
        }
        binding.tvAiLearningStatus.text = learningMessage

        // Set result message
        val resultMessage = when {
            playerWon && aiConfidence > 70 -> "Incredible! You beat a highly adaptive AI!"
            playerWon && aiConfidence > 40 -> "Great job! You outsmarted the learning AI!"
            playerWon -> "Victory! But the AI is learning..."
            !playerWon && aiConfidence > 70 -> "The AI predicted your every move!"
            !playerWon && aiConfidence > 40 -> "The AI exploited your patterns!"
            else -> "The AI got lucky this time!"
        }
        binding.tvResultMessage.text = resultMessage
    }

    private fun setupButtons() {
        // Play Again Button
        binding.btnPlayAgain.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Main Menu Button
        binding.btnMainMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        // Prevent going back to game
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}