package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Query("SELECT * FROM saved_patterns ORDER BY timestamp DESC")
    fun getAllPatterns(): Flow<List<SavedPattern>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: SavedPattern)

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun deletePatternById(id: Int)

    @Delete
    suspend fun deletePattern(pattern: SavedPattern)
}
