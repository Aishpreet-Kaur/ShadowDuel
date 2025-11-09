package com.example.shadowduel.data.database.dao

import androidx.room.*
import com.example.shadowduel.data.database.entities.DetectedPatternEntity

@Dao
interface PatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: DetectedPatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: DetectedPatternEntity)

    @Query("SELECT * FROM detected_patterns WHERE confidenceScore > :minConfidence ORDER BY confidenceScore DESC")
    suspend fun getHighConfidencePatterns(minConfidence: Float = 0.6f): List<DetectedPatternEntity>

    @Query("SELECT * FROM detected_patterns WHERE patternName = :name LIMIT 1")
    suspend fun getPatternByName(name: String): DetectedPatternEntity?

    @Query("SELECT * FROM detected_patterns ORDER BY confidenceScore DESC")
    suspend fun getAllPatterns(): List<DetectedPatternEntity>

    @Query("DELETE FROM detected_patterns")
    suspend fun deleteAllPatterns()
}