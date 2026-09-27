package com.example.core.ai

import okhttp3.OkHttpClient

class AiProviderFactory(private val httpClient: OkHttpClient) {

    private val geminiProvider = GeminiProvider(httpClient)
    private val anthropicProvider = AnthropicProvider(httpClient)

    fun getProvider(providerName: String): AiProvider {
        return when (providerName.trim().lowercase()) {
            "gemini" -> geminiProvider
            "anthropic" -> anthropicProvider
            "openrouter" -> OpenAICompatibleProvider(httpClient, name = "OPENROUTER")
            "openai" -> OpenAICompatibleProvider(httpClient, name = "OPENAI")
            "custom" -> OpenAICompatibleProvider(httpClient, name = "CUSTOM")
            else -> OpenAICompatibleProvider(httpClient, name = providerName.uppercase())
        }
    }
}
