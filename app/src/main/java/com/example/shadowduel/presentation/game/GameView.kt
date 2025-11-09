package com.example.shadowduel.presentation.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.shadowduel.domain.model.Fighter
import com.example.shadowduel.domain.model.Move

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var playerFighter: Fighter? = null
    private var opponentFighter: Fighter? = null

    // Battle animation state
    private var isBattleAnimating = false
    private var battleProgress = 0f
    private var playerBattleMove: Move = Move.IDLE
    private var opponentBattleMove: Move = Move.IDLE
    private var battleResult: BattleResult = BattleResult.NONE

    // Animation offsets
    private var playerOffsetX = 0f
    private var playerOffsetY = 0f
    private var opponentOffsetX = 0f
    private var opponentOffsetY = 0f
    private var cameraZoom = 1f
    private var screenShake = 0f
    private var flashAlpha = 0
    private var breathingOffset = 0f

    // Particles
    private val particles = mutableListOf<Particle>()
    private val impactLines = mutableListOf<ImpactLine>()

    // Paint objects
    private val backgroundPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, 2000f,
            intArrayOf(
                Color.parseColor("#0f0c29"),
                Color.parseColor("#302b63"),
                Color.parseColor("#24243e")
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    private val playerPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#00ff88"))
    }

    private val opponentPaint = Paint().apply {
        color = Color.parseColor("#ff0055")
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#ff0055"))
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        startBreathingAnimation()
        startParticleLoop()
    }

    private fun startBreathingAnimation() {
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                breathingOffset = sin(Math.toRadians((animation.animatedValue as Float).toDouble())).toFloat() * 5f
                if (!isBattleAnimating) invalidate()
            }
            start()
        }
    }

    private fun startParticleLoop() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                particles.removeAll { it.isDead() }
                particles.forEach { it.update() }
                impactLines.removeAll { it.isDead() }
                impactLines.forEach { it.update() }
                if (!isBattleAnimating) invalidate()
            }
            start()
        }
    }

    fun updateFighters(player: Fighter, opponent: Fighter) {
        this.playerFighter = player
        this.opponentFighter = opponent
        if (!isBattleAnimating) invalidate()
    }

    fun playBattleAnimation(
        playerMove: Move,
        opponentMove: Move,
        result: BattleResult,
        onComplete: () -> Unit
    ) {
        if (isBattleAnimating) return

        isBattleAnimating = true
        playerBattleMove = playerMove
        opponentBattleMove = opponentMove
        battleResult = result

        // Reset offsets
        playerOffsetX = 0f
        playerOffsetY = 0f
        opponentOffsetX = 0f
        opponentOffsetY = 0f
        cameraZoom = 1f

        // Multi-phase animation
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                battleProgress = animation.animatedValue as Float
                updateBattlePositions()
                invalidate()
            }

            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isBattleAnimating = false
                    playerBattleMove = Move.IDLE
                    opponentBattleMove = Move.IDLE
                    battleProgress = 0f
                    playerOffsetX = 0f
                    playerOffsetY = 0f
                    opponentOffsetX = 0f
                    opponentOffsetY = 0f
                    cameraZoom = 1f
                    onComplete()
                    invalidate()
                }
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
        }
        animator.start()
    }

    private fun updateBattlePositions() {
        when {
            // Phase 1: Charge forward (0.0 - 0.3)
            battleProgress < 0.3f -> {
                val progress = battleProgress / 0.3f
                playerOffsetX = progress * 150f
                opponentOffsetX = -progress * 150f
                cameraZoom = 1f + (progress * 0.2f)
            }

            // Phase 2: Attack execution (0.3 - 0.6)
            battleProgress < 0.6f -> {
                val progress = (battleProgress - 0.3f) / 0.3f
                playerOffsetX = 150f
                opponentOffsetX = -150f
                cameraZoom = 1.2f

                // Execute moves
                executeMoveAnimation(progress)
            }

            // Phase 3: Impact & recoil (0.6 - 0.8)
            battleProgress < 0.8f -> {
                val progress = (battleProgress - 0.6f) / 0.2f

                when (battleResult) {
                    BattleResult.PLAYER_HIT -> {
                        opponentOffsetX = -150f + (progress * 80f) // Knocked back
                        screenShake = 15f * (1f - progress)
                        if (progress < 0.3f) createImpactEffect(false)
                    }
                    BattleResult.OPPONENT_HIT -> {
                        playerOffsetX = 150f - (progress * 80f)
                        screenShake = 15f * (1f - progress)
                        if (progress < 0.3f) createImpactEffect(true)
                    }
                    BattleResult.BOTH_HIT -> {
                        playerOffsetX = 150f - (progress * 60f)
                        opponentOffsetX = -150f + (progress * 60f)
                        screenShake = 20f * (1f - progress)
                        if (progress < 0.3f) {
                            createImpactEffect(true)
                            createImpactEffect(false)
                        }
                    }
                    BattleResult.BOTH_BLOCKED, BattleResult.NONE -> {
                        // No knockback
                    }
                }

                flashAlpha = (200 * (1f - progress)).toInt()
            }

            // Phase 4: Return to position (0.8 - 1.0)
            else -> {
                val progress = (battleProgress - 0.8f) / 0.2f
                playerOffsetX = 150f - (progress * 150f)
                opponentOffsetX = -150f + (progress * 150f)
                cameraZoom = 1.2f - (progress * 0.2f)
                screenShake = 0f
                flashAlpha = 0
            }
        }
    }

    private fun executeMoveAnimation(progress: Float) {
        // Create energy trails and effects
        if (progress < 0.5f && particles.size < 50) {
            if (playerBattleMove.isAttack()) {
                repeat(2) {
                    particles.add(Particle(
                        width * 0.25f + playerOffsetX + 50f,
                        height * 0.5f,
                        Color.parseColor("#00ff88")
                    ))
                }
            }
            if (opponentBattleMove.isAttack()) {
                repeat(2) {
                    particles.add(Particle(
                        width * 0.75f + opponentOffsetX - 50f,
                        height * 0.5f,
                        Color.parseColor("#ff0055")
                    ))
                }
            }
        }
    }

    private fun createImpactEffect(isOpponent: Boolean) {
        val x = if (isOpponent) width * 0.75f + opponentOffsetX else width * 0.25f + playerOffsetX
        val y = height * 0.5f
        val color = if (isOpponent) Color.parseColor("#ff0055") else Color.parseColor("#00ff88")

        // Explosion particles
        repeat(20) {
            particles.add(Particle(x, y, Color.YELLOW, speed = 8f))
        }

        // Impact lines
        repeat(8) { i ->
            impactLines.add(ImpactLine(x, y, i * 45f, color))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        // Apply shake
        canvas.translate(
            (Math.random() * screenShake - screenShake / 2).toFloat(),
            (Math.random() * screenShake - screenShake / 2).toFloat()
        )

        // Apply zoom
        val centerX = width / 2f
        val centerY = height / 2f
        canvas.scale(cameraZoom, cameraZoom, centerX, centerY)

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Ground
        val groundY = height * 0.75f
        drawGround(canvas, groundY)

        // Particles behind fighters
        particles.filter { !it.foreground }.forEach { it.draw(canvas) }

        // Draw fighters
        playerFighter?.let { player ->
            drawFighter(
                canvas,
                player,
                width * 0.25f + playerOffsetX,
                groundY + playerOffsetY,
                true,
                if (isBattleAnimating) playerBattleMove else Move.IDLE
            )
        }

        opponentFighter?.let { opponent ->
            drawFighter(
                canvas,
                opponent,
                width * 0.75f + opponentOffsetX,
                groundY + opponentOffsetY,
                false,
                if (isBattleAnimating) opponentBattleMove else Move.IDLE
            )
        }

        // Impact lines
        impactLines.forEach { it.draw(canvas) }

        // Particles in front
        particles.filter { it.foreground }.forEach { it.draw(canvas) }

        // Battle text
        if (isBattleAnimating && battleProgress > 0.3f && battleProgress < 0.7f) {
            val alpha = if (battleProgress < 0.5f) {
                ((battleProgress - 0.3f) / 0.2f * 255).toInt()
            } else {
                ((0.7f - battleProgress) / 0.2f * 255).toInt()
            }

            textPaint.alpha = alpha
            val resultText = when (battleResult) {
                BattleResult.PLAYER_HIT -> "HIT!"
                BattleResult.OPPONENT_HIT -> "BLOCKED!"
                BattleResult.BOTH_HIT -> "CLASH!"
                BattleResult.BOTH_BLOCKED -> "MISS!"
                BattleResult.NONE -> ""
            }
            canvas.drawText(resultText, width / 2f, height / 2f, textPaint)
        }

        // Flash overlay
        if (flashAlpha > 0) {
            val flashPaint = Paint().apply {
                color = when (battleResult) {
                    BattleResult.PLAYER_HIT -> Color.RED
                    BattleResult.OPPONENT_HIT -> Color.GREEN
                    else -> Color.WHITE
                }
                alpha = flashAlpha
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
        }

        canvas.restore()

        // Health bars (not affected by zoom/shake)
        playerFighter?.let { drawHealthBar(canvas, it, 50f, 50f, true) }
        opponentFighter?.let { drawHealthBar(canvas, it, width - 350f, 50f, false) }
    }

    private fun drawGround(canvas: Canvas, groundY: Float) {
        val groundPaint = Paint().apply {
            color = Color.parseColor("#444444")
            strokeWidth = 4f
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#222222"))
        }
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, groundPaint)
    }

    private fun drawFighter(
        canvas: Canvas,
        fighter: Fighter,
        x: Float,
        groundY: Float,
        isPlayer: Boolean,
        currentMove: Move
    ) {
        val paint = if (isPlayer) playerPaint else opponentPaint
        val breath = if (!isBattleAnimating) breathingOffset else 0f

        val bodyWidth = 70f
        val bodyHeight = 140f
        val bodyTop = groundY - bodyHeight - 80f + breath

        // Glow
        val glowPaint = Paint().apply {
            color = paint.color
            alpha = 80
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, bodyTop + bodyHeight / 2, 100f, glowPaint)

        // Body
        canvas.drawRoundRect(
            x - bodyWidth / 2, bodyTop,
            x + bodyWidth / 2, groundY - 80f + breath,
            20f, 20f, paint
        )

        // Head
        canvas.drawCircle(x, bodyTop - 40f, 40f, paint)

        // Eyes
        val eyePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x - 15f, bodyTop - 45f, 6f, eyePaint)
        canvas.drawCircle(x + 15f, bodyTop - 45f, 6f, eyePaint)

        // Pupils
        val pupilPaint = Paint().apply { color = Color.BLACK }
        canvas.drawCircle(x - 15f, bodyTop - 45f, 3f, pupilPaint)
        canvas.drawCircle(x + 15f, bodyTop - 45f, 3f, pupilPaint)

        // Legs
        canvas.drawRoundRect(x - bodyWidth / 2, groundY - 80f + breath, x - 15f, groundY, 15f, 15f, paint)
        canvas.drawRoundRect(x + 15f, groundY - 80f + breath, x + bodyWidth / 2, groundY, 15f, 15f, paint)

        // Arms based on move
        drawArmsForMove(canvas, currentMove, x, bodyTop + 50f + breath, isPlayer, paint)

        // Move label
        if (isBattleAnimating && currentMove != Move.IDLE && battleProgress > 0.2f && battleProgress < 0.8f) {
            val movePaint = Paint().apply {
                color = Color.YELLOW
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(5f, 0f, 0f, Color.BLACK)
            }
            canvas.drawText(currentMove.displayName, x, bodyTop - 100f, movePaint)
        }
    }

    private fun drawArmsForMove(canvas: Canvas, move: Move, x: Float, y: Float, isPlayer: Boolean, paint: Paint) {
        val armPaint = Paint(paint).apply {
            strokeWidth = 18f
            strokeCap = Paint.Cap.ROUND
        }
        val dir = if (isPlayer) 1f else -1f

        when (move) {
            Move.ATTACK_HIGH, Move.ATTACK_LOW -> {
                val targetY = if (move == Move.ATTACK_HIGH) y - 50f else y + 50f
                canvas.drawLine(x, y, x + (70f * dir), targetY, armPaint)
            }
            Move.BLOCK_HIGH, Move.BLOCK_LOW -> {
                val targetY = if (move == Move.BLOCK_HIGH) y - 40f else y + 40f
                canvas.drawLine(x - 30f, y, x - 30f, targetY, armPaint)
                canvas.drawLine(x + 30f, y, x + 30f, targetY, armPaint)
            }
            Move.DODGE -> {
                canvas.drawLine(x, y, x - (50f * dir), y + 30f, armPaint)
            }
            Move.SPECIAL -> {
                canvas.drawLine(x, y, x + (80f * dir), y, armPaint)
                val energyPaint = Paint().apply {
                    color = Color.YELLOW
                    setShadowLayer(20f, 0f, 0f, Color.YELLOW)
                }
                canvas.drawCircle(x + (80f * dir), y, 30f, energyPaint)
            }
            else -> {
                canvas.drawLine(x - 30f, y, x - 30f, y + 50f, armPaint.apply { strokeWidth = 15f })
                canvas.drawLine(x + 30f, y, x + 30f, y + 50f, armPaint)
            }
        }
    }

    private fun drawHealthBar(canvas: Canvas, fighter: Fighter, x: Float, y: Float, isPlayer: Boolean) {
        val barWidth = 300f
        val barHeight = 40f

        val bgPaint = Paint().apply {
            color = Color.parseColor("#1a1a1a")
            setShadowLayer(5f, 0f, 2f, Color.BLACK)
        }
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 10f, 10f, bgPaint)

        val healthWidth = (fighter.health / 100f) * barWidth
        val healthPaint = Paint().apply {
            shader = LinearGradient(
                x, y, x + healthWidth, y,
                when {
                    fighter.health > 60 -> Color.parseColor("#00ff88")
                    fighter.health > 30 -> Color.parseColor("#ffcc00")
                    else -> Color.parseColor("#ff0055")
                },
                Color.parseColor("#004422"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(x, y, x + healthWidth, y + barHeight, 10f, 10f, healthPaint)

        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 10f, 10f, borderPaint)

        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
        val label = if (isPlayer) "PLAYER" else "SHADOW AI"
        canvas.drawText("$label: ${fighter.health}", x + 10f, y + 28f, labelPaint)
    }

    enum class BattleResult {
        NONE, PLAYER_HIT, OPPONENT_HIT, BOTH_HIT, BOTH_BLOCKED
    }

    private inner class Particle(
        private var x: Float,
        private var y: Float,
        private val color: Int,
        private val speed: Float = 4f,
        val foreground: Boolean = Math.random() > 0.5
    ) {
        private val vx = (Math.random() * speed * 2 - speed).toFloat()
        private val vy = (Math.random() * speed * 2 - speed).toFloat()
        private var alpha = 255
        private val paint = Paint().apply { this.color = this@Particle.color }

        fun update() {
            x += vx
            y += vy
            alpha = (alpha - 6).coerceAtLeast(0)
        }

        fun draw(canvas: Canvas) {
            paint.alpha = alpha
            canvas.drawCircle(x, y, 6f, paint)
        }

        fun isDead() = alpha <= 0
    }

    private inner class ImpactLine(
        private val x: Float,
        private val y: Float,
        private val angle: Float,
        private val color: Int
    ) {
        private var length = 0f
        private var alpha = 255
        private val paint = Paint().apply {
            this.color = this@ImpactLine.color
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }

        fun update() {
            length += 8f
            alpha = (alpha - 15).coerceAtLeast(0)
        }

        fun draw(canvas: Canvas) {
            paint.alpha = alpha
            val rad = Math.toRadians(angle.toDouble())
            canvas.drawLine(
                x, y,
                x + (length * Math.cos(rad)).toFloat(),
                y + (length * Math.sin(rad)).toFloat(),
                paint
            )
        }

        fun isDead() = alpha <= 0
    }
}