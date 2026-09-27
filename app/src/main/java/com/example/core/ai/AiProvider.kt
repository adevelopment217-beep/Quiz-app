package com.example.core.ai

import com.example.core.model.AiProviderConfig

data class AiProviderResult(
    val success: Boolean,
    val content: String = "",
    val httpStatus: Int = 0,
    val latencyMs: Long = 0L,
    val errorMessage: String? = null,
    val providerName: String = "",
    val modelName: String = "",
    val endpointHost: String = ""
)

interface AiProvider {
    val name: String

    suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String,
        config: AiProviderConfig,
        jsonOutput: Boolean = false
    ): AiProviderResult
}
