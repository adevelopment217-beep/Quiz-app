package com.example.core.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.core.model.AiProviderConfig

class AiConfigRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("medha_ai_config_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): AiProviderConfig {
        val defaultApiKey = try {
            BuildConfig.GEMINI_API_KEY.ifBlank { "" }
        } catch (e: Exception) {
            ""
        }

        val savedApiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val effectiveApiKey = savedApiKey.ifBlank { defaultApiKey }

        return AiProviderConfig(
            provider = prefs.getString(KEY_PROVIDER, "gemini") ?: "gemini",
            model = prefs.getString(KEY_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash",
            baseUrl = prefs.getString(KEY_BASE_URL, "https://generativelanguage.googleapis.com") ?: "https://generativelanguage.googleapis.com",
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 2048),
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "You are MedhaQuiz AI, an empathetic, highly knowledgeable educational tutor specializing in Bangladesh Classes 1-10 curriculum.") ?: "",
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, true),
            isAssistantEnabled = prefs.getBoolean(KEY_IS_ASSISTANT_ENABLED, true),
            isSemanticEvalEnabled = prefs.getBoolean(KEY_IS_SEMANTIC_ENABLED, true),
            isQualityCheckEnabled = prefs.getBoolean(KEY_IS_QUALITY_ENABLED, true),
            apiKey = effectiveApiKey
        )
    }

    fun saveConfig(config: AiProviderConfig) {
        prefs.edit()
            .putString(KEY_PROVIDER, config.provider)
            .putString(KEY_MODEL, config.model)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_BASE_URL, config.baseUrl)
            .putFloat(KEY_TEMPERATURE, config.temperature)
            .putInt(KEY_MAX_TOKENS, config.maxTokens)
            .putString(KEY_SYSTEM_PROMPT, config.systemPrompt)
            .putBoolean(KEY_IS_ENABLED, config.isEnabled)
            .putBoolean(KEY_IS_ASSISTANT_ENABLED, config.isAssistantEnabled)
            .putBoolean(KEY_IS_SEMANTIC_ENABLED, config.isSemanticEvalEnabled)
            .putBoolean(KEY_IS_QUALITY_ENABLED, config.isQualityCheckEnabled)
            .apply()

        AiManager.instance.updateConfig(config)
    }

    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_TEMPERATURE = "ai_temperature"
        private const val KEY_MAX_TOKENS = "ai_max_tokens"
        private const val KEY_SYSTEM_PROMPT = "ai_system_prompt"
        private const val KEY_IS_ENABLED = "ai_is_enabled"
        private const val KEY_IS_ASSISTANT_ENABLED = "ai_is_assistant_enabled"
        private const val KEY_IS_SEMANTIC_ENABLED = "ai_is_semantic_enabled"
        private const val KEY_IS_QUALITY_ENABLED = "ai_is_quality_enabled"

        @Volatile
        private var INSTANCE: AiConfigRepository? = null

        fun getInstance(context: Context): AiConfigRepository {
            return INSTANCE ?: synchronized(this) {
                val inst = AiConfigRepository(context.applicationContext)
                INSTANCE = inst
                // Immediately apply to AiManager
                AiManager.instance.updateConfig(inst.loadConfig())
                inst
            }
        }
    }
}
