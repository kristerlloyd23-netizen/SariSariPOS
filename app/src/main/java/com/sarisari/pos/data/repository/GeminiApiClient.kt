package com.sarisari.pos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal REST client for the Gemini Developer API; no API key is included in the APK. */
object GeminiApiClient {
    const val MODEL = "gemini-3.8-flash"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun generate(apiKey: String, systemInstruction: String, groundedContext: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", groundedContext)))
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.25)
                        put("maxOutputTokens", 650)
                    })
                }
                val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 25_000
                    instanceFollowRedirects = false
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("x-goog-api-key", apiKey)
                }
                try {
                    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) return@runCatching null
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val parts = JSONObject(response).optJSONArray("candidates")
                        ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        ?: return@runCatching null
                    buildString {
                        for (i in 0 until parts.length()) {
                            val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                            if (text.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(text)
                            }
                        }
                    }.trim().takeIf(String::isNotBlank)?.take(3000)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
}
