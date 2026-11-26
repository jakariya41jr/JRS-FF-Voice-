package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class CartesiaTtsClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class SynthesisResult {
        data class Success(val audioFile: File) : SynthesisResult()
        data class Failure(val reason: String) : SynthesisResult()
    }

    suspend fun synthesizeToWav(
        apiKey: String,
        text: String,
        voiceId: String,
        languageCode: String,
        speed: Float,
        outputFile: File
    ): SynthesisResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext SynthesisResult.Failure("Missing Cartesia API Key")
        }
        if (voiceId.isBlank()) {
            return@withContext SynthesisResult.Failure("Missing Voice ID")
        }

        try {
            val payload = JSONObject().apply {
                put("model_id", "sonic-2")
                put("transcript", text)
                put("voice", JSONObject().apply {
                    put("mode", "id")
                    put("id", voiceId)
                })
                put("output_format", JSONObject().apply {
                    put("container", "wav")
                    put("encoding", "pcm_s16le")
                    put("sample_rate", 44100)
                })
                put("language", languageCode.ifBlank { "en" })
                val speedTag = when {
                    speed <= 0.78f -> "slowest"
                    speed <= 0.92f -> "slow"
                    speed >= 1.25f -> "fastest"
                    speed >= 1.10f -> "fast"
                    else -> "normal"
                }
                put("generation_config", JSONObject().apply {
                    put("speed", speed.toDouble().coerceIn(0.6, 1.5))
                })
                put("__experimental_controls", JSONObject().apply {
                    put("speed", speedTag)
                })
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.cartesia.ai/tts/bytes")
                .addHeader("X-API-Key", apiKey)
                .addHeader("Cartesia-Version", "2024-11-13")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()?.take(180).orEmpty()
                    return@withContext SynthesisResult.Failure("HTTP ${response.code}: $errBody")
                }
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.size < 44) {
                    return@withContext SynthesisResult.Failure("Empty audio response")
                }
                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(bytes)
                SynthesisResult.Success(outputFile)
            }
        } catch (t: Throwable) {
            SynthesisResult.Failure(t.message ?: "Network error")
        }
    }
}
