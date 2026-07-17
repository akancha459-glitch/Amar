package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_patterns")
data class SavedPattern(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "TEXT", "DRAWING", "EMOJI", "PRESET"
    val textValue: String = "",
    val colorHex: String = "#FF0000",
    val pixelData: String, // Comma-separated hex values (e.g., "#000000,#FF0000,...") for 8xN matrix
    val columnsCount: Int = 32,
    val timestamp: Long = System.currentTimeMillis()
)
