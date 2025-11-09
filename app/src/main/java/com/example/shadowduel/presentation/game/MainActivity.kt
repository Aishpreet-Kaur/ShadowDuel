package com.example.shadowduel.presentation.game

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shadowduel.databinding.ActivityMainBinding
import com.example.shadowduel.presentation.game.GameActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        // New Game Button
        binding.btnNewGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // How to Play Button
        binding.btnHowToPlay.setOnClickListener {
            showHowToPlayDialog()
        }

        // View Stats Button
        binding.btnViewStats.setOnClickListener {
            showStatsDialog()
        }

        // Exit Button
        binding.btnExit.setOnClickListener {
            showExitConfirmation()
        }
    }

    private fun showHowToPlayDialog() {
        AlertDialog.Builder(this)
            .setTitle("📖 How to Play")
            .setMessage(
                """
                🎮 OBJECTIVE:
                Defeat the Shadow AI in combat!
                
                ⚔️ MOVES:
                • Attack High/Low: Deal damage
                • Block High/Low: Counter attacks
                • Dodge: Evade all attacks
                • Special: High damage (30 HP)
                
                🧠 AI LEARNING:
                The AI learns your patterns!
                • Rounds 1-3: Easy (AI learning)
                • Rounds 4-7: Medium (detecting patterns)
                • Rounds 8+: Hard (exploiting patterns)
                
                🏆 VICTORY:
                First to win 3 rounds wins the game!
                
                💡 TIP: Change your strategy to confuse the AI!
                """.trimIndent()
            )
            .setPositiveButton("GOT IT!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showStatsDialog() {
        AlertDialog.Builder(this)
            .setTitle("📊 AI Learning System")
            .setMessage(
                """
                🤖 ADAPTIVE AI FEATURES:
                
                ✅ Pattern Detection
                The AI tracks your move history and finds patterns.
                
                ✅ Health-Based Prediction
                AI learns what you do at low/high health.
                
                ✅ Sequence Learning
                AI detects move combinations.
                
                ✅ Confidence Meter
                Higher confidence = AI is more certain about predicting your next move.
                
                ✅ Database Storage
                All your moves are stored and analyzed in real-time!
                
                🎓 This demonstrates:
                • Machine Learning concepts
                • Pattern recognition
                • Adaptive AI behavior
                • Real-time data processing
                """.trimIndent()
            )
            .setPositiveButton("COOL!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Game?")
            .setMessage("Are you sure you want to exit Shadow Duel?")
            .setPositiveButton("YES") { _, _ -> finish() }
            .setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}