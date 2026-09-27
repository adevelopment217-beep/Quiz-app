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

class AnthropicProvider(
    private val httpClient: OkHttpClient
) : AiProvider {

    override val name: String = "ANTHROPIC"

    override suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String,
        config: AiProviderConfig,
        jsonOutput: Boolean
    ): AiProviderResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.trim()
        val model = config.model.ifBlank { "claude-3-5-sonnet-20241022" }

        var baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com/v1"
        }
        val endpoint = if (baseUrl.endsWith("/messages")) baseUrl else "$baseUrl/messages"

        val host = try {
            URI(endpoint).host ?: "api.anthropic.com"
        } catch (e: Exception) {
            "api.anthropic.com"
        }

        val requestJson = JSONObject()
        requestJson.put("model", model)
        requestJson.put("max_tokens", if (config.maxTokens > 0) config.maxTokens else 2048)
        requestJson.put("temperature", config.temperature)

        if (systemPrompt.isNotBlank()) {
            requestJson.put("system", systemPrompt)
        }

        val messagesArray = JSONArray()
        val userObj = JSONObject()
        userObj.put("role", "user")
        userObj.put("content", prompt)
        messagesArray.put(userObj)
        requestJson.put("messages", messagesArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("x-api-key", apiKey)
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
            val contentArray = respJson.optJSONArray("content")
            val firstBlock = contentArray?.optJSONObject(0)
            val text = firstBlock?.optString("text", "") ?: ""

            if (text.isBlank()) {
                return@withContext AiProviderResult(
                    success = false,
                    httpStatus = statusCode,
                    latencyMs = latency,
                    errorMessage = "Anthropic returned empty text.",
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
                errorMessage = "Connection error to $host: ${e.localizedMessage ?: "Unknown failure"}",
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
