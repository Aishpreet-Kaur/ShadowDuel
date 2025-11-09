package com.example.shadowduel.domain.model

enum class Move(
    val displayName: String,
    val damage: Int,
    val cooldownMs: Long,
    val animationDurationMs: Long
) {
    ATTACK_HIGH("Attack High", 15, 800, 300),
    ATTACK_LOW("Attack Low", 15, 800, 300),
    BLOCK_HIGH("Block High", 0, 600, 200),
    BLOCK_LOW("Block Low", 0, 600, 200),
    DODGE("Dodge", 0, 1200, 400),
    SPECIAL("Special Attack", 30, 2000, 500),
    IDLE("Idle", 0, 0, 0);

    fun counters(opponentMove: Move): Boolean {
        return when (this) {
            BLOCK_HIGH -> opponentMove == ATTACK_HIGH
            BLOCK_LOW -> opponentMove == ATTACK_LOW
            DODGE -> opponentMove in listOf(ATTACK_HIGH, ATTACK_LOW, SPECIAL)
            else -> false
        }
    }

    fun isAttack(): Boolean = this in listOf(ATTACK_HIGH, ATTACK_LOW, SPECIAL)
    fun isDefense(): Boolean = this in listOf(BLOCK_HIGH, BLOCK_LOW, DODGE)
}