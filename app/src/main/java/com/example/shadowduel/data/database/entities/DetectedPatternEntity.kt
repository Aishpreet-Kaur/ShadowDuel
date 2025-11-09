package com.example.shadowduel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_patterns")
data class DetectedPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patternName: String,
    val condition: String, // JSON string describing the condition
    val predictedMove: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)