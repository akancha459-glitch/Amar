package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

enum class PatternType {
    DRAWING, TEXT, PRESET
}

class FanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PatternRepository(db.patternDao())

    // 1. Connection states
    val ipAddress = MutableStateFlow("192.168.4.1")
    val connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val statusMessage = MutableStateFlow("Ready to connect")

    // 2. Hardware controls
    val fanBrightness = MutableStateFlow(150f)
    val fanDelayUs = MutableStateFlow(800)

    // 3. Grid / Editor State (8 rows, 32 columns standard)
    val colsCount = MutableStateFlow(32)
    val pixelGrid = MutableStateFlow<List<String>>(List(8 * 32) { "#000000" })
    val selectedDrawColor = MutableStateFlow("#FF0000")

    // 4. Mode States
    val activePatternType = MutableStateFlow(PatternType.DRAWING)
    
    // 5. Scroll Text States
    val scrollText = MutableStateFlow("HELLO")
    val textColor = MutableStateFlow("#FF00FF")

    // 6. Local database patterns list
    val savedPatterns: StateFlow<List<SavedPattern>> = repository.allPatterns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isNetworkLoading = MutableStateFlow(false)

    init {
        // Load default heart pattern initially
        loadPresetPattern("HEART")
    }

    // Connect and query status
    fun connectToFan() {
        viewModelScope.launch {
            connectionStatus.value = ConnectionStatus.CONNECTING
            statusMessage.value = "Connecting to http://${ipAddress.value}..."
            val result = FanClient.checkStatus(ipAddress.value)
            result.onSuccess { status ->
                connectionStatus.value = ConnectionStatus.CONNECTED
                fanBrightness.value = status.brightness.toFloat()
                fanDelayUs.value = status.delayUs
                statusMessage.value = "Connected! POV Fan detected."
            }.onFailure { err ->
                connectionStatus.value = ConnectionStatus.ERROR
                statusMessage.value = "Connection failed: ${err.message ?: "Unknown error"}. Make sure you are connected to the 'POV_Fan_LED' Wi-Fi."
            }
        }
    }

    // Set brightness
    fun updateBrightness(value: Float) {
        fanBrightness.value = value
        viewModelScope.launch {
            FanClient.sendBrightness(ipAddress.value, value.toInt())
        }
    }

    // Set Column delay (microsecond speed tuner)
    fun updateDelayUs(value: Int) {
        fanDelayUs.value = value
        viewModelScope.launch {
            FanClient.sendDelay(ipAddress.value, value)
        }
    }

    // Set individual pixel color in the grid
    fun setPixelColor(row: Int, col: Int, color: String) {
        val currentList = pixelGrid.value.toMutableList()
        val index = col * 8 + row
        if (index in currentList.indices) {
            currentList[index] = color
            pixelGrid.value = currentList
        }
    }

    // Clear grid
    fun clearGrid() {
        pixelGrid.value = List(8 * colsCount.value) { "#000000" }
    }

    // Generate pattern from Text state
    fun generateTextPattern() {
        viewModelScope.launch {
            val result = PatternGenerator.generateFromText(scrollText.value, textColor.value, columnsLimit = 64)
            colsCount.value = result.first
            pixelGrid.value = result.second
            statusMessage.value = "Generated text pattern (${result.first} columns)"
        }
    }

    // Load preset pattern
    fun loadPresetPattern(presetName: String) {
        when (presetName) {
            "HEART" -> {
                colsCount.value = 32
                pixelGrid.value = PatternGenerator.getHeartPreset()
                activePatternType.value = PatternType.PRESET
            }
            "SMILEY" -> {
                colsCount.value = 32
                pixelGrid.value = PatternGenerator.getSmileyPreset()
                activePatternType.value = PatternType.PRESET
            }
            "RAINBOW" -> {
                colsCount.value = 32
                pixelGrid.value = PatternGenerator.getRainbowWavePreset()
                activePatternType.value = PatternType.PRESET
            }
            "SPIRAL" -> {
                colsCount.value = 32
                pixelGrid.value = PatternGenerator.getSpiralPreset()
                activePatternType.value = PatternType.PRESET
            }
        }
    }

    // Upload current pattern to ESP8266
    fun uploadPatternToFan() {
        viewModelScope.launch {
            isNetworkLoading.value = true
            statusMessage.value = "Uploading pattern to http://${ipAddress.value}..."
            
            // If in TEXT mode, generate text pattern before upload
            if (activePatternType.value == PatternType.TEXT) {
                generateTextPattern()
            }

            val result = FanClient.sendPattern(
                ip = ipAddress.value,
                cols = colsCount.value,
                patternColors = pixelGrid.value
            )

            result.onSuccess { resp ->
                isNetworkLoading.value = false
                statusMessage.value = "Pattern Uploaded Successfully! 🚀"
            }.onFailure { err ->
                isNetworkLoading.value = false
                statusMessage.value = "Upload Failed: ${err.message}. Ensure you are on the same Wi-Fi."
            }
        }
    }

    // Local library operations
    fun savePatternToLocal(name: String) {
        viewModelScope.launch {
            val dataString = pixelGrid.value.joinToString(",")
            val pattern = SavedPattern(
                name = name.ifEmpty { "My Pattern ${System.currentTimeMillis() % 1000}" },
                type = activePatternType.value.name,
                textValue = if (activePatternType.value == PatternType.TEXT) scrollText.value else "",
                colorHex = if (activePatternType.value == PatternType.TEXT) textColor.value else selectedDrawColor.value,
                pixelData = dataString,
                columnsCount = colsCount.value
            )
            repository.insert(pattern)
            statusMessage.value = "Pattern saved to local library!"
        }
    }

    fun deletePatternFromLocal(pattern: SavedPattern) {
        viewModelScope.launch {
            repository.delete(pattern)
            statusMessage.value = "Pattern deleted from library"
        }
    }

    fun loadPattern(pattern: SavedPattern) {
        colsCount.value = pattern.columnsCount
        pixelGrid.value = pattern.pixelData.split(",")
        activePatternType.value = PatternType.valueOf(pattern.type)
        if (pattern.type == "TEXT") {
            scrollText.value = pattern.textValue
            textColor.value = pattern.colorHex
        } else {
            selectedDrawColor.value = pattern.colorHex
        }
        statusMessage.value = "Loaded pattern: ${pattern.name}"
    }
}
