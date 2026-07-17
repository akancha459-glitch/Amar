package com.example.data

import kotlinx.coroutines.flow.Flow

class PatternRepository(private val patternDao: PatternDao) {
    val allPatterns: Flow<List<SavedPattern>> = patternDao.getAllPatterns()

    suspend fun insert(pattern: SavedPattern) {
        patternDao.insertPattern(pattern)
    }

    suspend fun deleteById(id: Int) {
        patternDao.deletePatternById(id)
    }

    suspend fun delete(pattern: SavedPattern) {
        patternDao.deletePattern(pattern)
    }
}
