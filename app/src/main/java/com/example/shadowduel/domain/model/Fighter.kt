package com.example.shadowduel.domain.model

data class Fighter(
    val name: String,
    var health: Int = 100,
    var currentMove: Move = Move.IDLE,
    var isPlayer: Boolean = false
) {
    val healthPercentage: Int
        get() = (health * 100) / 100

    val healthRange: String
        get() = when {
            health >= 75 -> "75-100"
            health >= 50 -> "50-75"
            health >= 25 -> "25-50"
            else -> "0-25"
        }

    fun takeDamage(damage: Int) {
        health = (health - damage).coalesce(0, 100)
    }

    fun isAlive(): Boolean = health > 0

    fun reset() {
        health = 100
        currentMove = Move.IDLE
    }
}

// Extension function to clamp value
fun Int.coalesce(min: Int, max: Int): Int {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}