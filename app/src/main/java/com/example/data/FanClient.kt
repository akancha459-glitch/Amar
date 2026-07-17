package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FanClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Base URL is typically http://192.168.4.1
    private fun formatIp(ip: String): String {
        var cleanIp = ip.trim()
        if (!cleanIp.startsWith("http://") && !cleanIp.startsWith("https://")) {
            cleanIp = "http://$cleanIp"
        }
        if (cleanIp.endsWith("/")) {
            cleanIp = cleanIp.substring(0, cleanIp.length - 1)
        }
        return cleanIp
    }

    suspend fun checkStatus(ip: String): Result<FanStatus> = withContext(Dispatchers.IO) {
        try {
            val url = "${formatIp(ip)}/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val brightness = json.optInt("brightness", 150)
                val columns = json.optInt("columns", 32)
                val delayUs = json.optInt("delay_us", 800)
                Result.success(FanStatus(brightness, columns, delayUs))
            }
        } catch (e: Exception) {
            Log.e("FanClient", "Status check failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendBrightness(ip: String, brightness: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = "${formatIp(ip)}/brightness?val=$brightness"
            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val actualBrightness = json.optInt("brightness", brightness)
                Result.success(actualBrightness)
            }
        } catch (e: Exception) {
            Log.e("FanClient", "Brightness post failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendDelay(ip: String, delayUs: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = "${formatIp(ip)}/delay?val=$delayUs"
            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val actualDelay = json.optInt("delay_us", delayUs)
                Result.success(actualDelay)
            }
        } catch (e: Exception) {
            Log.e("FanClient", "Delay post failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPattern(ip: String, cols: Int, patternColors: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${formatIp(ip)}/pattern?cols=$cols"
            
            // Format colors as comma separated hex list. We omit '#' to save bandwidth
            val cleanedColors = patternColors.map { 
                if (it.startsWith("#")) it.substring(1) else it 
            }.joinToString(",")

            val mediaType = "text/plain".toMediaType()
            val requestBody = cleanedColors.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                Result.success(body)
            }
        } catch (e: Exception) {
            Log.e("FanClient", "Pattern upload failed", e)
            Result.failure(e)
        }
    }
}

data class FanStatus(
    val brightness: Int,
    val columns: Int,
    val delayUs: Int
)
