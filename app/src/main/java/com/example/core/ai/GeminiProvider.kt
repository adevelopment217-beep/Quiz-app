package com.example.core.ai

import com.example.core.model.AiProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class GeminiProvider(private val httpClient: OkHttpClient) : AiProvider {

    override val name: String = "GEMINI"

    override suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String,
        config: AiProviderConfig,
        jsonOutput: Boolean
    ): AiProviderResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.trim()
        val model = config.model.ifBlank { "gemini-2.5-flash" }

        var baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com"
        }

        val endpoint = when {
            baseUrl.contains("/models/") && baseUrl.contains(":generateContent") -> baseUrl
            baseUrl.contains("/v1beta") || baseUrl.contains("/v1") -> "$baseUrl/models/$model:generateContent"
            else -> "$baseUrl/v1beta/models/$model:generateContent"
        }

        val host = try {
            URI(endpoint).host ?: "generativelanguage.googleapis.com"
        } catch (e: Exception) {
            "generativelanguage.googleapis.com"
        }

        val requestJson = JSONObject()

        // System Instruction
        if (systemPrompt.isNotBlank()) {
            val sysInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            sysInstruction.put("parts", sysParts)
            requestJson.put("systemInstruction", sysInstruction)
        }

        // Contents
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        contentObj.put("role", "user")
        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", config.temperature)
        genConfig.put("maxOutputTokens", if (config.maxTokens > 0) config.maxTokens else 2048)
        if (jsonOutput) {
            genConfig.put("responseMimeType", "application/json")
        }
        requestJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(if (apiKey.isNotBlank()) "$endpoint?key=$apiKey" else endpoint)
            .post(body)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("x-goog-api-key", apiKey)
        }

        try {
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val statusCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val sanitizedError = sanitizeError(responseBody, statusCode, apiKey)
                return@withContext AiProviderResult(
                    success = false,
                    httpStatus = statusCode,
                    latencyMs = latency,
                    errorMessage = sanitizedError,
                    providerName = name,
                    modelName = model,
                    endpointHost = host
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            if (text.isBlank()) {
                val finishReason = firstCandidate?.optString("finishReason", "EMPTY")
                return@withContext AiProviderResult(
                    success = false,
                    httpStatus = statusCode,
                    latencyMs = latency,
                    errorMessage = "Gemini returned empty text (FinishReason: $finishReason)",
                    providerName = name,
                    modelName = model,
                    endpointHost = host
                )
            }

            return@withContext AiProviderResult(
                success = true,
                content = text,
                httpStatus = statusCode,
                latencyMs = latency,
                providerName = name,
                modelName = model,
                endpointHost = host
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            return@withContext AiProviderResult(
                success = false,
                httpStatus = 0,
                latencyMs = latency,
                errorMessage = "Network or connection error: ${e.localizedMessage ?: "Unknown error"}",
                providerName = name,
                modelName = model,
                endpointHost = host
            )
        }
    }

    private fun sanitizeError(rawBody: String, statusCode: Int, apiKey: String): String {
        var msg = try {
            val obj = JSONObject(rawBody)
            val errObj = obj.optJSONObject("error")
            errObj?.optString("message") ?: obj.optString("message", rawBody.take(200))
        } catch (e: Exception) {
            rawBody.take(200)
        }
        if (apiKey.isNotBlank()) {
            msg = msg.replace(apiKey, "••••••••")
        }
        return "HTTP $statusCode: $msg"
    }
}
