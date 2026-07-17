package com.example

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ArduinoCode
import com.example.data.SavedPattern
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CyberBg
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: FanViewModel = viewModel()
    
    // States from ViewModel
    val ip by viewModel.ipAddress.collectAsStateWithLifecycle()
    val connStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val brightness by viewModel.fanBrightness.collectAsStateWithLifecycle()
    val delayUs by viewModel.fanDelayUs.collectAsStateWithLifecycle()
    
    val activeTab = remember { mutableStateOf(0) } // 0: Control, 1: Designer, 2: Library, 3: ESP8266 Code
    
    Column(modifier = modifier) {
        // App Header with Glowing Cyber Logo & Status Indicator
        HeaderSection(
            connStatus = connStatus,
            statusMsg = statusMsg,
            onConnectClick = { viewModel.connectToFan() },
            ip = ip,
            onIpChange = { viewModel.ipAddress.value = it }
        )

        // Navigation Tabs using Cyber styled buttons
        TabsRowSection(activeTab = activeTab)

        // Tab Content with animation transitions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (activeTab.value) {
                0 -> ControlPanelTab(
                    brightness = brightness,
                    delayUs = delayUs,
                    onBrightnessChange = { viewModel.updateBrightness(it) },
                    onDelayChange = { viewModel.updateDelayUs(it) },
                    connStatus = connStatus,
                    statusMsg = statusMsg
                )
                1 -> DesignerTab(viewModel = viewModel)
                2 -> LibraryTab(viewModel = viewModel)
                3 -> ArduinoCodeTab(context = context)
            }
        }
    }
}

@Composable
fun HeaderSection(
    connStatus: ConnectionStatus,
    statusMsg: String,
    onConnectClick: () -> Unit,
    ip: String,
    onIpChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(CyberPrimary, CyberSecondary)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "POV FAN LED",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Smart Wi-Fi Controller",
                        fontSize = 12.sp,
                        color = CyberTextMuted
                    )
                }

                // Status LED badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = when (connStatus) {
                                ConnectionStatus.CONNECTED -> Color(0xFF1B5E20)
                                ConnectionStatus.CONNECTING -> Color(0xFFE65100)
                                ConnectionStatus.DISCONNECTED -> Color(0xFF3E2723)
                                ConnectionStatus.ERROR -> Color(0xFFB71C1C)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (connStatus) {
                                    ConnectionStatus.CONNECTED -> Color.Green
                                    ConnectionStatus.CONNECTING -> Color.Yellow
                                    ConnectionStatus.DISCONNECTED -> Color.Gray
                                    ConnectionStatus.ERROR -> Color.Red
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (connStatus) {
                            ConnectionStatus.CONNECTED -> "CONNECTED"
                            ConnectionStatus.CONNECTING -> "CONNECTING"
                            ConnectionStatus.DISCONNECTED -> "OFFLINE"
                            ConnectionStatus.ERROR -> "ERROR"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // IP Connection input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = onIpChange,
                    label = { Text("ESP8266 IP (आईपी)", color = CyberTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = CyberTextMuted,
                        focusedTextColor = CyberText,
                        unfocusedTextColor = CyberText
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        contentColor = CyberBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CONNECT", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Status description text
            Text(
                text = statusMsg,
                fontSize = 12.sp,
                color = when (connStatus) {
                    ConnectionStatus.CONNECTED -> CyberPrimary
                    ConnectionStatus.ERROR -> CyberSecondary
                    else -> CyberTextMuted
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TabsRowSection(activeTab: MutableState<Int>) {
    ScrollableTabRow(
        selectedTabIndex = activeTab.value,
        containerColor = Color.Transparent,
        contentColor = CyberPrimary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.value]),
                color = CyberSecondary
            )
        },
        divider = {}
    ) {
        val tabs = listOf(
            Pair(Icons.Default.Tune, "CONTROL"),
            Pair(Icons.Default.Palette, "DESIGNER"),
            Pair(Icons.Default.Folder, "LIBRARY"),
            Pair(Icons.Default.Code, "ESP8266 CODE")
        )
        tabs.forEachIndexed { index, pair ->
            Tab(
                selected = activeTab.value == index,
                onClick = { activeTab.value = index },
                text = { 
                    Text(
                        pair.second, 
                        fontWeight = if (activeTab.value == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    ) 
                },
                icon = { 
                    Icon(
                        pair.first, 
                        contentDescription = pair.second,
                        tint = if (activeTab.value == index) CyberSecondary else CyberTextMuted
                    ) 
                },
                selectedContentColor = CyberSecondary,
                unselectedContentColor = CyberTextMuted
            )
        }
    }
}

@Composable
fun ControlPanelTab(
    brightness: Float,
    delayUs: Int,
    onBrightnessChange: (Float) -> Unit,
    onDelayChange: (Int) -> Unit,
    connStatus: ConnectionStatus,
    statusMsg: String
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF232335))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = "Brightness", tint = CyberPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "BRIGHTNESS (चमक)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberText
                            )
                        }
                        Text(
                            "${(brightness / 2.55).toInt()}% (${brightness.toInt()})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberPrimary,
                            activeTrackColor = CyberPrimary,
                            inactiveTrackColor = Color(0xFF1E1E2D)
                        )
                    )
                    
                    Text(
                        text = "Adjust the LED brightness to save power or increase visibility.",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF232335))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = CyberSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SPEED SYNC DELAY (स्पीड सिंक्रोनाइज़)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberText
                            )
                        }
                        Text(
                            "${delayUs} µs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = delayUs.toFloat(),
                        onValueChange = { onDelayChange(it.toInt()) },
                        valueRange = 100f..3000f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberSecondary,
                            activeTrackColor = CyberSecondary,
                            inactiveTrackColor = Color(0xFF1E1E2D)
                        )
                    )
                    
                    Text(
                        text = "Decrease delay if text appears stretched; increase delay if text looks compressed. Match it perfectly with your fan's RPM!",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )
                }
            }
        }

        // Quick Troubleshooting Advice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120B16)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberTertiary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to Sync POV Light (सिंक करने की विधि):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Connect your phone to ESP8266 AP Wifi Network: **POV_Fan_LED**\n" +
                        "• Password is: **password123**\n" +
                        "• Keep the fan spinning at a constant speed.\n" +
                        "• Adjust the 'SPEED SYNC DELAY' above in micro-seconds to match rotation speed so the image displays correctly.",
                        fontSize = 12.sp,
                        color = CyberText,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignerTab(viewModel: FanViewModel) {
    val context = LocalContext.current
    val colsCount by viewModel.colsCount.collectAsStateWithLifecycle()
    val pixelGrid by viewModel.pixelGrid.collectAsStateWithLifecycle()
    val drawColor by viewModel.selectedDrawColor.collectAsStateWithLifecycle()
    val activeMode by viewModel.activePatternType.collectAsStateWithLifecycle()
    val scrollText by viewModel.scrollText.collectAsStateWithLifecycle()
    val textColor by viewModel.textColor.collectAsStateWithLifecycle()
    val isUploading by viewModel.isNetworkLoading.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf("") }

    val colorsPalette = listOf(
        "#FF0000", // Red
        "#FF5500", // Orange
        "#FFFF00", // Yellow
        "#00FF00", // Green
        "#00FFFF", // Cyan
        "#0055FF", // Blue
        "#FF00FF", // Magenta
        "#AA00FF", // Purple
        "#FFFFFF", // White
        "#000000"  // OFF / Black
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Mode Selectors
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Pair(PatternType.DRAWING, "DRAW (ड्रॉ)"),
                    Pair(PatternType.TEXT, "TEXT/EMOJI")
                ).forEach { (mode, title) ->
                    Button(
                        onClick = { viewModel.activePatternType.value = mode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == mode) CyberSecondary else CyberCard,
                            contentColor = if (activeMode == mode) Color.White else CyberTextMuted
                        ),
                        border = BorderStroke(1.dp, if (activeMode == mode) CyberSecondary else Color(0xFF232335)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (activeMode == PatternType.DRAWING) {
            // Palette Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Select Color (रंग चुनें):", fontSize = 12.sp, color = CyberTextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorsPalette.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = drawColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) CyberPrimary else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.selectedDrawColor.value = hex }
                                ) {
                                    if (hex == "#000000") {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = "Off",
                                            tint = Color.Red,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive 8x32 Grid Canvas
            item {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            "8-Bit LED Matrix Grid (8x32):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimary
                        )
                        Text(
                            "Scroll Right →",
                            fontSize = 10.sp,
                            color = CyberSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .background(CyberGridBg, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2C2C42), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        for (col in 0 until colsCount) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 3.dp)
                            ) {
                                for (row in 0 until 8) {
                                    val index = col * 8 + row
                                    val pixelColorHex = pixelGrid.getOrNull(index) ?: "#000000"
                                    val color = Color(android.graphics.Color.parseColor(pixelColorHex))
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(color, shape = RoundedCornerShape(4.dp))
                                            .border(
                                                1.dp, 
                                                Color(0xFF33334F).copy(alpha = 0.6f), 
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                viewModel.setPixelColor(row, col, drawColor)
                                            }
                                    )
                                }
                                Text(
                                    text = "${col + 1}",
                                    fontSize = 8.sp,
                                    color = CyberTextMuted,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }

            // Utility Clear / Fill Canvas Buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearGrid() },
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLEAR (साफ करें)", color = Color.Red)
                    }

                    OutlinedButton(
                        onClick = {
                            for (c in 0 until colsCount) {
                                for (r in 0 until 8) {
                                    viewModel.setPixelColor(r, c, drawColor)
                                }
                            }
                        },
                        border = BorderStroke(1.dp, CyberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.FormatColorFill, contentDescription = "Fill", tint = CyberPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FILL (पूरा भरें)", color = CyberPrimary)
                    }
                }
            }
        }

        // TEXT / EMOJI Mode fields
        if (activeMode == PatternType.TEXT) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF232335))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Type Custom Text or Emojis:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimary
                        )

                        OutlinedTextField(
                            value = scrollText,
                            onValueChange = { viewModel.scrollText.value = it },
                            label = { Text("Text or Emoji (उदा. HELLO ❤️)", color = CyberTextMuted) },
                            placeholder = { Text("E.g. INDIA 🌟", color = CyberTextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CyberText,
                                unfocusedTextColor = CyberText,
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = Color(0xFF232335)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Text Color palette inside Card
                        Text("Select Text Color (अक्षर का रंग चुनें):", fontSize = 12.sp, color = CyberTextMuted)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            colorsPalette.filter { it != "#000000" }.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = textColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) CyberPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.textColor.value = hex }
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.generateTextPattern() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = CyberBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Generate")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GENERATE PATTERN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // PRESET DESIGN SHORTCUTS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF232335))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Quick Presets (जल्दी सेटिंग्स):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        mapOf(
                            "HEART" to "❤️ Heart (दिल)",
                            "SMILEY" to "😊 Smiley (स्माइली)",
                            "RAINBOW" to "🌈 Rainbow (इंद्रधनुष)",
                            "SPIRAL" to "🌀 Spiral (कुंडली)"
                        ).forEach { (presetKey, label) ->
                            AssistChip(
                                onClick = { 
                                    viewModel.loadPresetPattern(presetKey)
                                    Toast.makeText(context, "$presetKey Preset Loaded", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(label, color = CyberText) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF1B1B29),
                                    leadingIconContentColor = CyberSecondary
                                ),
                                border = BorderStroke(1.dp, Color(0xFF2C2C42))
                            )
                        }
                    }
                }
            }
        }

        // Send & Save Actions Block
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = { showSaveDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTertiary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE TO LIBRARY", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.uploadPatternToFan() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSecondary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Upload")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UPLOAD TO FAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Save Pattern Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Pattern", color = CyberPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a name for your design:", color = CyberText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveNameInput,
                        onValueChange = { saveNameInput = it },
                        singleLine = true,
                        placeholder = { Text("My Design 1") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberText,
                            unfocusedTextColor = CyberText,
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberTextMuted
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.savePatternToLocal(saveNameInput)
                        saveNameInput = ""
                        showSaveDialog = false
                        Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("SAVE", color = CyberPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = CyberTextMuted)
                }
            },
            containerColor = CyberCard
        )
    }
}

@Composable
fun LibraryTab(viewModel: FanViewModel) {
    val context = LocalContext.current
    val savedList by viewModel.savedPatterns.collectAsStateWithLifecycle()
    
    if (savedList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Empty",
                    tint = CyberTextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "No Saved Patterns yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberText
                )
                Text(
                    "Designs you draw or generate from text\nwill be stored here locally.",
                    fontSize = 12.sp,
                    color = CyberTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Saved Patterns Library (सुरक्षित किए पैटर्न):",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyberPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedList) { pattern ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        border = BorderStroke(1.dp, Color(0xFF2C2C42)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadPattern(pattern)
                                Toast
                                    .makeText(
                                        context,
                                        "Loaded '${pattern.name}' in Designer tab!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Tiny preview circle matrix
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black, shape = RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw simple multi-colored dots inside box as mock preview
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dots = pattern.pixelData.split(",").take(3)
                                    dots.forEach { dHex ->
                                        val c = try { Color(android.graphics.Color.parseColor(dHex)) } catch (e: Exception) { CyberSecondary }
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(c))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pattern.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberText
                                )
                                Text(
                                    "Type: ${pattern.type} • Cols: ${pattern.columnsCount}",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.deletePatternFromLocal(pattern) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArduinoCodeTab(context: Context) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C42))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ESP8266 Arduino Instructions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ArduinoCode.instructions,
                        fontSize = 12.sp,
                        color = CyberText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        "Arduino Source Code:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary
                    )

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Arduino POV Fan Code", ArduinoCode.sketch)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Arduino Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Code scroll view box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color(0xFF06060A), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E1E2F), shape = RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = ArduinoCode.sketch,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF00FFCC),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
