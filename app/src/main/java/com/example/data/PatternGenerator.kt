package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

object PatternGenerator {
    
    // Generates an 8xN pixel grid from text/emoji
    fun generateFromText(text: String, textColorHex: String, columnsLimit: Int = 64): Pair<Int, List<String>> {
        if (text.isEmpty()) {
            return Pair(32, List(8 * 32) { "#000000" })
        }
        
        // Setup paint with specific text color
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(textColorHex)
            textSize = 8.5f // Height of text is approx 8 pixels
            style = Paint.Style.FILL
        }
        
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        // Pad the text width so it has some breathing room
        val textWidth = Math.max(bounds.width() + 6, 32)
        val finalWidth = Math.min(textWidth, columnsLimit)
        
        // Create 8px tall bitmap
        val bitmap = Bitmap.createBitmap(finalWidth, 8, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK) // Black background means LED off
        
        // Align text vertically in the 8px height
        // Baseline should be around y = 7 (since height is 8)
        val x = 2f
        val y = 7f
        canvas.drawText(text, x, y, paint)
        
        val pixelsList = ArrayList<String>(finalWidth * 8)
        for (col in 0 until finalWidth) {
            for (row in 0 until 8) {
                val pixel = bitmap.getPixel(col, row)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // If it's near black, turn it off completely
                if (r < 25 && g < 25 && b < 25) {
                    pixelsList.add("#000000")
                } else {
                    // Format color as RGB hex
                    val hex = String.format("#%02X%02X%02X", r, g, b)
                    pixelsList.add(hex)
                }
            }
        }
        
        bitmap.recycle()
        return Pair(finalWidth, pixelsList)
    }

    // Default presets
    fun getHeartPreset(): List<String> {
        val grid = Array(8) { Array(32) { "#000000" } }
        // Let's draw a few hearts around the circle (e.g. at col 4-10, col 20-26)
        val heartCols = listOf(6, 7, 8, 22, 23, 24)
        
        // Heart 1 around col 8
        grid[1][7] = "#FF0055"; grid[1][9] = "#FF0055"
        grid[2][6] = "#FF0055"; grid[2][7] = "#FF0055"; grid[2][8] = "#FF0055"; grid[2][9] = "#FF0055"; grid[2][10] = "#FF0055"
        grid[3][6] = "#FF0055"; grid[3][7] = "#FF0055"; grid[3][8] = "#FF0055"; grid[3][9] = "#FF0055"; grid[3][10] = "#FF0055"
        grid[4][7] = "#FF0055"; grid[4][8] = "#FF0055"; grid[4][9] = "#FF0055"
        grid[5][8] = "#FF0055"
        
        // Heart 2 around col 24
        grid[1][23] = "#FF0055"; grid[1][25] = "#FF0055"
        grid[2][22] = "#FF0055"; grid[2][23] = "#FF0055"; grid[2][24] = "#FF0055"; grid[2][25] = "#FF0055"; grid[2][26] = "#FF0055"
        grid[3][22] = "#FF0055"; grid[3][23] = "#FF0055"; grid[3][24] = "#FF0055"; grid[3][25] = "#FF0055"; grid[3][26] = "#FF0055"
        grid[4][23] = "#FF0055"; grid[4][24] = "#FF0055"; grid[4][25] = "#FF0055"
        grid[5][24] = "#FF0055"

        // Flatten to columns major order: Col 0 (row 0..7), Col 1 (row 0..7)...
        val flatList = ArrayList<String>(256)
        for (col in 0 until 32) {
            for (row in 0 until 8) {
                flatList.add(grid[row][col])
            }
        }
        return flatList
    }

    fun getSmileyPreset(): List<String> {
        val grid = Array(8) { Array(32) { "#000000" } }
        // Face 1 centered at col 8
        grid[1][7] = "#FFFF00"; grid[1][9] = "#FFFF00" // Eyes
        grid[4][6] = "#FFFF00"; grid[4][10] = "#FFFF00" // Smile edges
        grid[5][7] = "#FFFF00"; grid[5][8] = "#FFFF00"; grid[5][9] = "#FFFF00" // Smile bottom
        
        // Face 2 centered at col 24
        grid[1][23] = "#FFFF00"; grid[1][25] = "#FFFF00"
        grid[4][22] = "#FFFF00"; grid[4][26] = "#FFFF00"
        grid[5][23] = "#FFFF00"; grid[5][24] = "#FFFF00"; grid[5][25] = "#FFFF00"

        val flatList = ArrayList<String>(256)
        for (col in 0 until 32) {
            for (row in 0 until 8) {
                flatList.add(grid[row][col])
            }
        }
        return flatList
    }

    fun getRainbowWavePreset(): List<String> {
        val flatList = ArrayList<String>(256)
        val colors = listOf(
            "#FF0000", // Red
            "#FF7F00", // Orange
            "#FFFF00", // Yellow
            "#00FF00", // Green
            "#0000FF", // Blue
            "#4B0082", // Indigo
            "#9400D3", // Violet
            "#FF00FF"  // Magenta
        )
        for (col in 0 until 32) {
            for (row in 0 until 8) {
                // Wave offset
                val colorIndex = (row + col) % colors.size
                flatList.add(colors[colorIndex])
            }
        }
        return flatList
    }

    fun getSpiralPreset(): List<String> {
        val flatList = ArrayList<String>(256)
        for (col in 0 until 32) {
            for (row in 0 until 8) {
                val activeRow = (col / 4) % 8
                if (row == activeRow) {
                    flatList.add("#00FFFF") // Cyan spiral
                } else {
                    flatList.add("#000000")
                }
            }
        }
        return flatList
    }
}
