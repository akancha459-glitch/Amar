package com.example.data

object ArduinoCode {
    const val sketch = """#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <Adafruit_NeoPixel.h>

// --- Configuration ---
#define LED_PIN     4   // D2 on NodeMCU. Connect DI (Data Input) of WS2812B here.
#define NUM_LEDS    8   // 8-bit POV Fan display (8 LEDs along the blade)
#define MAX_COLS    128 // Maximum column patterns we can store in memory

// Hall Effect Sensor pin (Optional: connect magnetic sensor to trigger sync)
#define HALL_PIN    12  // D6 on NodeMCU. Grounded when magnet is detected.
bool useHallSensor = false; // Will set to true if trigger detected

// Initialize Adafruit NeoPixel library
Adafruit_NeoPixel strip(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

// Set up Access Point (AP) Credentials
const char* ssid = "POV_Fan_LED";
const char* password = "password123"; // Must be at least 8 chars

ESP8266WebServer server(80);

// Global state variables
uint8_t globalBrightness = 150; // 0 to 255
int numColumns = 32;            // Current loaded columns
uint32_t patternMemory[MAX_COLS][NUM_LEDS]; // Memory buffer for pattern
int columnDelayUs = 800;        // Delay between columns in microseconds (speed control)

// Default "Heart" pattern preloaded
void loadDefaultPattern() {
  numColumns = 32;
  for (int c = 0; c < 32; c++) {
    for (int r = 0; r < 8; r++) {
      patternMemory[c][r] = 0; // Turn off
    }
  }
  // Heart 1 centered around col 8
  patternMemory[7][1] = strip.Color(255, 0, 80);
  patternMemory[9][1] = strip.Color(255, 0, 80);
  for (int c = 6; c <= 10; c++) {
    patternMemory[c][2] = strip.Color(255, 0, 80);
    patternMemory[c][3] = strip.Color(255, 0, 80);
  }
  for (int c = 7; c <= 9; c++) patternMemory[c][4] = strip.Color(255, 0, 80);
  patternMemory[8][5] = strip.Color(255, 0, 80);

  // Heart 2 centered around col 24
  patternMemory[23][1] = strip.Color(255, 0, 80);
  patternMemory[25][1] = strip.Color(255, 0, 80);
  for (int c = 22; c <= 26; c++) {
    patternMemory[c][2] = strip.Color(255, 0, 80);
    patternMemory[c][3] = strip.Color(255, 0, 80);
  }
  for (int c = 23; c <= 25; c++) patternMemory[c][4] = strip.Color(255, 0, 80);
  patternMemory[24][5] = strip.Color(255, 0, 80);
}

// Convert "RRGGBB" hex string to uint32_t color
uint32_t parseHexColor(String hex) {
  if (hex.length() < 6) return 0;
  long rgb = strtol(hex.c_str(), NULL, 16);
  byte r = (rgb >> 16) & 0xFF;
  byte g = (rgb >> 8) & 0xFF;
  byte b = rgb & 0xFF;
  return strip.Color(r, g, b);
}

// REST Endpoint: Root index
void handleRoot() {
  String response = "{\"status\":\"running\",\"device\":\"WS2812B POV Fan\",\"brightness\":" + String(globalBrightness) + ",\"columns\":" + String(numColumns) + ",\"delay_us\":" + String(columnDelayUs) + "}";
  server.send(200, "application/json", response);
}

// REST Endpoint: Update Brightness
void handleBrightness() {
  if (server.hasArg("val")) {
    int b = server.arg("val").toInt();
    if (b >= 0 && b <= 255) {
      globalBrightness = b;
      strip.setBrightness(globalBrightness);
      strip.show();
      server.send(200, "application/json", "{\"status\":\"success\",\"brightness\":" + String(globalBrightness) + "}");
      return;
    }
  }
  server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Missing/invalid 'val' param\"}");
}

// REST Endpoint: Update Speed / Timing
void handleDelay() {
  if (server.hasArg("val")) {
    int val = server.arg("val").toInt();
    if (val >= 50 && val <= 10000) {
      columnDelayUs = val;
      server.send(200, "application/json", "{\"status\":\"success\",\"delay_us\":" + String(columnDelayUs) + "}");
      return;
    }
  }
  server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Missing/invalid 'val' param\"}");
}

// REST Endpoint: Upload Custom Pattern
// Body should contain comma-separated 6-char hex colors (e.g., "FF0000,00FF00,...")
void handleUploadPattern() {
  if (!server.hasArg("cols")) {
    server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Missing 'cols' parameter\"}");
    return;
  }

  int cols = server.arg("cols").toInt();
  if (cols <= 0 || cols > MAX_COLS) {
    server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"cols must be between 1 and " + String(MAX_COLS) + "\"}");
    return;
  }

  String body = server.arg("plain"); // Raw post body
  if (body.length() == 0) {
    server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Empty body\"}");
    return;
  }

  int expectedPixels = cols * NUM_LEDS;
  int parsedCount = 0;
  
  // Parse comma-separated hex values
  int strPos = 0;
  for (int c = 0; c < cols; c++) {
    for (int r = 0; r < NUM_LEDS; r++) {
      if (strPos >= body.length()) break;
      
      int nextComma = body.indexOf(',', strPos);
      String hexVal;
      if (nextComma == -1) {
        hexVal = body.substring(strPos);
        strPos = body.length();
      } else {
        hexVal = body.substring(strPos, nextComma);
        strPos = nextComma + 1;
      }
      
      hexVal.trim();
      // Remove leading '#' if present
      if (hexVal.startsWith("#")) {
        hexVal = hexVal.substring(1);
      }
      
      patternMemory[c][r] = parseHexColor(hexVal);
      parsedCount++;
    }
  }

  numColumns = cols;
  server.send(200, "application/json", "{\"status\":\"success\",\"loaded_cols\":" + String(numColumns) + ",\"parsed_pixels\":" + String(parsedCount) + "}");
}

void setup() {
  Serial.begin(115200);
  
  // Setup NeoPixel Strip
  strip.begin();
  strip.setBrightness(globalBrightness);
  strip.show(); // Initialize all pixels to 'off'
  
  // Load Default Heart Pattern
  loadDefaultPattern();
  
  // Setup Hall Effect sensor pin
  pinMode(HALL_PIN, INPUT_PULLUP);
  
  // Start ESP8266 Access Point
  WiFi.softAP(ssid, password);
  Serial.println("");
  Serial.print("Access Point SSID: ");
  Serial.println(ssid);
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());
  
  // Register REST API endpoints
  server.on("/", HTTP_GET, handleRoot);
  server.on("/status", HTTP_GET, handleRoot);
  server.on("/brightness", HTTP_POST, handleBrightness);
  server.on("/brightness", HTTP_GET, handleBrightness); // support GET for testing
  server.on("/delay", HTTP_POST, handleDelay);
  server.on("/delay", HTTP_GET, handleDelay); // support GET for testing
  server.on("/pattern", HTTP_POST, handleUploadPattern);
  
  server.begin();
  Serial.println("HTTP Server started.");
}

void loop() {
  // Handle incoming HTTP requests
  server.handleClient();
  
  // Render the POV Display
  // If a Hall sensor is present, synchronize with it:
  if (digitalRead(HALL_PIN) == LOW) {
    useHallSensor = true;
  }

  if (useHallSensor) {
    // Wait for the magnet to trigger start of a rotation
    while (digitalRead(HALL_PIN) == HIGH) {
      server.handleClient(); // keep server responsive while waiting
    }
    // Render one full rotation sweep of columns
    for (int c = 0; c < numColumns; c++) {
      for (int r = 0; r < NUM_LEDS; r++) {
        strip.setPixelColor(r, patternMemory[c][r]);
      }
      strip.show();
      delayMicroseconds(columnDelayUs);
    }
  } else {
    // Sensorless mode: Continuous rotation sweep
    for (int c = 0; c < numColumns; c++) {
      for (int r = 0; r < NUM_LEDS; r++) {
        strip.setPixelColor(r, patternMemory[c][r]);
      }
      strip.show();
      delayMicroseconds(columnDelayUs);
    }
  }
}
"""

    const val instructions = """1. **Requirements**:
   - ESP8266 Board (e.g., NodeMCU or Wemos D1 Mini)
   - 8x WS2812B NeoPixel LED Strip or Blade module
   - (Optional) Hall Effect Sensor (e.g., US1881) for perfect rotation synchronization.

2. **Wiring Connection**:
   - **WS2812B VCC** -> NodeMCU **5V (VU)** or external 5V power supply
   - **WS2812B GND** -> NodeMCU **GND**
   - **WS2812B DIN (Data)** -> NodeMCU Pin **D2 (GPIO 4)**
   - **Hall Sensor Out** -> NodeMCU Pin **D6 (GPIO 12)** (Optional)

3. **Software Configuration**:
   - Open Arduino IDE.
   - Install **ESP8266 Board Support** from Boards Manager.
   - Install the **Adafruit NeoPixel** library from the Library Manager.
   - Copy the code above, paste it in Arduino IDE, and upload it to your ESP8266!

4. **Connecting to the App**:
   - Power on your POV Fan device.
   - On your Android Phone, connect to the Wi-Fi network: **"POV_Fan_LED"** with password **"password123"**.
   - Open this App and configure, draw, and upload custom patterns!
"""
}
