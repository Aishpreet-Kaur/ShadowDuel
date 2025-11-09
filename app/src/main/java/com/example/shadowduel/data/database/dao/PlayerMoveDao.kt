//package com.example.shadowduel.data.database.dao
//
//import androidx.room.*
//import com.example.shadowduel.data.database.entities.PlayerMoveEntity
//
//@Dao
//interface PlayerMoveDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMove(move: PlayerMoveEntity): Long
//
//    @Query("SELECT * FROM player_moves WHERE gameSessionId = :sessionId ORDER BY timestamp DESC")
//    suspend fun getMovesForSession(sessionId: String): List<PlayerMoveEntity>
//
//    @Query("SELECT * FROM player_moves ORDER BY timestamp DESC LIMIT :limit")
//    suspend fun getLastNMoves(limit: Int): List<PlayerMoveEntity>
//
//    @Query("""
//        SELECT * FROM player_moves
//        WHERE playerHealthRange = :healthRange
//        AND previousAiMove = :prevAiMove
//        ORDER BY timestamp DESC
//        LIMIT :limit
//    """)
//    suspend fun getMovesInSituation(
//        healthRange: String,
//        prevAiMove: String,
//        limit: Int = 20
//    ): List<PlayerMoveEntity>
//
//    @Query("""
//        SELECT moveType, COUNT(*) as count
//        FROM player_moves
//        WHERE playerHealthRange = :healthRange
//        GROUP BY moveType
//        ORDER BY count DESC
//        LIMIT 1
//    """)
//    suspend fun getMostFrequentMoveInHealthRange(healthRange: String): String?
//
//    @Query("DELETE FROM player_moves WHERE gameSessionId = :sessionId")
//    suspend fun deleteSessionMoves(sessionId: String)
//
//    @Query("DELETE FROM player_moves")
//    suspend fun deleteAllMoves()
//}

package com.example.shadowduel.data.database.dao

import androidx.room.*
import com.example.shadowduel.data.database.entities.PlayerMoveEntity

@Dao
interface PlayerMoveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(move: PlayerMoveEntity): Long

    @Query("SELECT * FROM player_moves WHERE gameSessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getMovesForSession(sessionId: String): List<PlayerMoveEntity>

    @Query("SELECT * FROM player_moves ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastNMoves(limit: Int): List<PlayerMoveEntity>

    @Query("""
        SELECT * FROM player_moves 
        WHERE playerHealthRange = :healthRange 
        AND previousAiMove = :prevAiMove
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getMovesInSituation(
        healthRange: String,
        prevAiMove: String,
        limit: Int = 20
    ): List<PlayerMoveEntity>

    // FIX: Changed the query to select only the 'moveType' column
    @Query("""
        SELECT moveType 
        FROM player_moves 
        WHERE playerHealthRange = :healthRange
        GROUP BY moveType 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostFrequentMoveInHealthRange(healthRange: String): String?

    @Query("DELETE FROM player_moves WHERE gameSessionId = :sessionId")
    suspend fun deleteSessionMoves(sessionId: String)

    @Query("DELETE FROM player_moves")
    suspend fun deleteAllMoves()
}
