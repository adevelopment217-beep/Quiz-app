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

class OpenAICompatibleProvider(
    private val httpClient: OkHttpClient,
    override val name: String = "OPENAI_COMPATIBLE"
) : AiProvider {

    override suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String,
        config: AiProviderConfig,
        jsonOutput: Boolean
    ): AiProviderResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.trim()
        val model = config.model.ifBlank { "gpt-4o-mini" }
        val endpoint = normalizeEndpoint(config.baseUrl)

        val host = try {
            URI(endpoint).host ?: "api.openai.com"
        } catch (e: Exception) {
            "api.openai.com"
        }

        val requestJson = JSONObject()
        requestJson.put("model", model)

        val messagesArray = JSONArray()

        if (systemPrompt.isNotBlank()) {
            val sysObj = JSONObject()
            sysObj.put("role", "system")
            sysObj.put("content", systemPrompt)
            messagesArray.put(sysObj)
        }

        val userObj = JSONObject()
        userObj.put("role", "user")
        userObj.put("content", prompt)
        messagesArray.put(userObj)

        requestJson.put("messages", messagesArray)
        requestJson.put("temperature", config.temperature)
        requestJson.put("max_tokens", if (config.maxTokens > 0) config.maxTokens else 2048)

        if (jsonOutput) {
            val respFormat = JSONObject()
            respFormat.put("type", "json_object")
            requestJson.put("response_format", respFormat)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body)
            .addHeader("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        // Helpful metadata headers for OpenRouter / HuggingFace
        requestBuilder.addHeader("HTTP-Referer", "https://medhaquiz.app")
        requestBuilder.addHeader("X-Title", "MedhaQuiz")

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
            val choices = respJson.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""

            if (content.isBlank()) {
                return@withContext AiProviderResult(
                    success = false,
                    httpStatus = statusCode,
                    latencyMs = latency,
                    errorMessage = "Provider returned empty choice content in response.",
                    providerName = name,
                    modelName = model,
                    endpointHost = host
                )
            }

            return@withContext AiProviderResult(
                success = true,
                content = content,
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
                errorMessage = "Connection error to $host: ${e.localizedMessage ?: "Unknown network failure"}",
                providerName = name,
                modelName = model,
                endpointHost = host
            )
        }
    }

    private fun normalizeEndpoint(rawUrl: String): String {
        var url = rawUrl.trim().trimEnd('/')
        if (url.isBlank()) {
            return "https://api.openai.com/v1/chat/completions"
        }

        // Avoid duplicate paths like /chat/completions/chat/completions
        if (url.endsWith("/chat/completions")) {
            return url
        }

        // If URL already ends with /v1
        if (url.endsWith("/v1")) {
            return "$url/chat/completions"
        }

        // If URL has version path in it like .../v1 or .../v1/something
        return if (url.contains("/v1/")) {
            "$url/chat/completions"
        } else {
            // Standard base url like https://api.groq.com/openai or https://router.huggingface.co
            // Check if /v1 should be prefixed
            if (url.endsWith("/v1") || url.endsWith("/v1beta")) {
                "$url/chat/completions"
            } else {
                "$url/chat/completions"
            }
        }
    }

    private fun sanitizeError(rawBody: String, statusCode: Int, apiKey: String): String {
        var msg = try {
            val obj = JSONObject(rawBody)
            val errObj = obj.optJSONObject("error")
            if (errObj != null) {
                errObj.optString("message", errObj.toString())
            } else {
                obj.optString("message", rawBody.take(200))
            }
        } catch (e: Exception) {
            rawBody.take(200)
        }
        if (apiKey.isNotBlank()) {
            msg = msg.replace(apiKey, "••••••••")
        }
        return "HTTP $statusCode: $msg"
    }
}
