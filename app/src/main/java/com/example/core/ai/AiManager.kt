package com.example.core.ai

import com.example.core.model.AiCorrectionReport
import com.example.core.model.AiEvaluationResult
import com.example.core.model.AiProviderConfig
import com.example.core.model.AiRequestLog
import com.example.core.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiManager private constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val providerFactory = AiProviderFactory(httpClient)

    var config: AiProviderConfig = AiProviderConfig()
        private set

    private val _requestLogs = MutableStateFlow<List<AiRequestLog>>(emptyList())
    val requestLogs: StateFlow<List<AiRequestLog>> = _requestLogs.asStateFlow()

    fun updateConfig(newConfig: AiProviderConfig) {
        config = newConfig
    }

    /**
     * Unified pipeline for ALL AI requests (Tutor, Evaluation, Quality Check, and Test Connection).
     * No feature makes direct or separate hard-coded API calls.
     */
    suspend fun executeRequest(
        feature: String,
        prompt: String,
        systemPrompt: String = config.systemPrompt,
        jsonOutput: Boolean = false
    ): AiProviderResult = withContext(Dispatchers.IO) {
        if (!config.isEnabled) {
            return@withContext AiProviderResult(
                success = false,
                errorMessage = "এআই মাস্টার ইঞ্জিন বর্তমানে বন্ধ রয়েছে (Master AI Engine Disabled)",
                providerName = config.provider,
                modelName = config.model
            )
        }

        val provider = providerFactory.getProvider(config.provider)
        val result = provider.generateCompletion(
            prompt = prompt,
            systemPrompt = systemPrompt,
            config = config,
            jsonOutput = jsonOutput
        )

        // Record diagnostic entry (Safe, never contains API key or user private credentials)
        recordLog(
            feature = feature,
            result = result
        )

        return@withContext result
    }

    /**
     * Real connection test using the exact same provider/model pipeline.
     */
    suspend fun testConnection(): AiProviderResult = withContext(Dispatchers.IO) {
        val testPrompt = "Hello! Please reply in one short sentence confirming that the AI connection is successfully active."
        return@withContext executeRequest(
            feature = "TEST_CONNECTION",
            prompt = testPrompt,
            systemPrompt = "You are a connectivity tester. Respond briefly and affirmatively in Bengali or English.",
            jsonOutput = false
        )
    }

    /**
     * Multi-Level Answer Evaluation:
     * Levels 1, 2, 3 -> DeterministicEvaluator
     * Level 4 -> Semantic AI (via active provider)
     * Level 5 -> Uncertain Fallback (does not crash)
     */
    suspend fun evaluateAnswer(
        question: Question,
        userAnswer: String
    ): AiEvaluationResult = withContext(Dispatchers.IO) {
        // Levels 1 to 3: Deterministic exact, normalized, accepted match
        val deterministic = DeterministicEvaluator.evaluate(question, userAnswer)
        if (deterministic != null) {
            return@withContext deterministic
        }

        // If AI or semantic eval is disabled or answer empty
        if (!config.isEnabled || !config.isSemanticEvalEnabled || userAnswer.isBlank()) {
            return@withContext AiEvaluationResult(
                status = "INCORRECT",
                confidence = 0.8,
                reason = "Answer does not match official key.",
                normalizedAnswer = userAnswer.trim(),
                suggestedCorrectAnswer = question.answer,
                explanation = question.explanation
            )
        }

        // Level 4: Semantic AI Evaluation
        try {
            val prompt = """
                You are an educational quiz answer evaluator for Bangladeshi school curriculum.
                Question: "${question.questionText}"
                Question Type: "${question.type}"
                Official Correct Answer: "${question.answer}"
                Alternative Accepted Answers: ${question.acceptedAnswers.joinToString(", ")}
                User's Submitted Answer: "$userAnswer"
                Official Explanation: "${question.explanation}"

                Task: Determine whether the user's answer is semantically and educationally correct.
                Accept spelling variations, translated names (e.g. Dhaka vs ঢাকা), full sentences expressing the core answer, or standard synonyms.

                Respond ONLY with valid JSON in this exact structure:
                {
                  "status": "CORRECT" or "INCORRECT" or "UNCERTAIN",
                  "confidence": 0.95,
                  "reason": "Brief rationale in Bengali",
                  "normalizedAnswer": "Cleaned up user answer",
                  "suggestedCorrectAnswer": "Official correct answer",
                  "explanation": "Helpful educational explanation"
                }
            """.trimIndent()

            val result = executeRequest(
                feature = "SEMANTIC_EVALUATION",
                prompt = prompt,
                systemPrompt = "You are a strict JSON-only educational evaluator.",
                jsonOutput = true
            )

            if (result.success && result.content.isNotBlank()) {
                val cleaned = cleanJsonString(result.content)
                val json = JSONObject(cleaned)
                return@withContext AiEvaluationResult(
                    status = json.optString("status", "UNCERTAIN").uppercase(),
                    confidence = json.optDouble("confidence", 0.8),
                    reason = json.optString("reason", "এআই উত্তর মূল্যায়ন করেছে।"),
                    normalizedAnswer = json.optString("normalizedAnswer", userAnswer),
                    suggestedCorrectAnswer = json.optString("suggestedCorrectAnswer", question.answer),
                    explanation = json.optString("explanation", question.explanation)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Level 5: Safe Uncertain fallback
        return@withContext AiEvaluationResult(
            status = "UNCERTAIN",
            confidence = 0.5,
            reason = "সেমেন্টিক মূল্যায়ন এই মুহূর্তে অনুপলব্ধ। শিক্ষক বা উত্তরমালা পর্যালোচনা করুন।",
            normalizedAnswer = userAnswer.trim(),
            suggestedCorrectAnswer = question.answer,
            explanation = question.explanation
        )
    }

    /**
     * Context-aware conversational AI Tutor response
     */
    suspend fun getTutorResponse(
        contextPrompt: String,
        userQuery: String
    ): String = withContext(Dispatchers.IO) {
        if (!config.isEnabled || !config.isAssistantEnabled) {
            return@withContext "এআই গৃহশিক্ষক বর্তমানে নিষ্ক্রিয় রয়েছে। অ্যাডমিন সেটিংস থেকে চালু করুন।"
        }

        val fullSystemPrompt = """
            ${config.systemPrompt}
            
            Current Educational Context:
            $contextPrompt
        """.trimIndent()

        val result = executeRequest(
            feature = "AI_TUTOR",
            prompt = userQuery,
            systemPrompt = fullSystemPrompt,
            jsonOutput = false
        )

        if (result.success && result.content.isNotBlank()) {
            return@withContext result.content
        } else {
            val err = result.errorMessage ?: "অজানা সমস্যা"
            return@withContext "দুঃখিত, এআই গৃহশিক্ষকের উত্তর পাওয়া যায়নি।\n($err)"
        }
    }

    /**
     * AI Quiz Quality Control & Defect Detection
     */
    suspend fun inspectQuizQuality(
        quizTitle: String,
        questions: List<Question>
    ): List<AiCorrectionReport> = withContext(Dispatchers.IO) {
        if (!config.isEnabled || !config.isQualityCheckEnabled || questions.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            val qArray = JSONArray()
            for (q in questions) {
                val obj = JSONObject()
                obj.put("id", q.id)
                obj.put("type", q.type)
                obj.put("question", q.questionText)
                obj.put("options", JSONArray(q.options))
                obj.put("answer", q.answer)
                obj.put("explanation", q.explanation)
                qArray.put(obj)
            }

            val prompt = """
                You are a senior curriculum auditor. Analyze the following quiz questions for defects:
                Quiz: "$quizTitle"
                Questions JSON:
                ${qArray.toString()}
                
                Inspect for:
                1. ANSWER_KEY_ERROR: Official answer is factually incorrect.
                2. AMBIGUOUS: Question has multiple correct options or confusing wording.
                3. DUPLICATE_OPTION: Multiple choices are identical or synonymous.
                4. MALFORMED: Missing options for MCQ, answer not in options, etc.
                5. TYPO: Obvious spelling or grammar mistake affecting clarity.
                
                Respond ONLY with a JSON array of reports:
                [
                  {
                    "questionId": "id_here",
                    "issueType": "ANSWER_KEY_ERROR",
                    "currentValue": "...",
                    "proposedValue": "...",
                    "reason": "Clear explanation of error",
                    "confidence": 0.95
                  }
                ]
                If no defects are found, return empty array [].
            """.trimIndent()

            val result = executeRequest(
                feature = "QUALITY_CHECK",
                prompt = prompt,
                systemPrompt = "You are a JSON-only curriculum auditor.",
                jsonOutput = true
            )

            if (result.success && result.content.isNotBlank()) {
                val cleaned = cleanJsonString(result.content)
                val array = JSONArray(cleaned)
                val reports = mutableListOf<AiCorrectionReport>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val qId = item.optString("questionId", "")
                    val targetQ = questions.find { it.id == qId }
                    reports.add(
                        AiCorrectionReport(
                            id = "cr_${System.currentTimeMillis()}_$i",
                            quizId = targetQ?.quizId ?: "",
                            questionId = qId,
                            quizTitle = quizTitle,
                            questionText = targetQ?.questionText ?: "",
                            issueType = item.optString("issueType", "ANSWER_KEY_ERROR"),
                            currentValue = item.optString("currentValue", targetQ?.answer ?: ""),
                            proposedValue = item.optString("proposedValue", ""),
                            reason = item.optString("reason", "এআই কোয়ালিটি কন্ট্রোল দ্বারা চিহ্নিত।"),
                            confidence = item.optDouble("confidence", 0.9),
                            aiModel = config.model
                        )
                    )
                }
                return@withContext reports
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext emptyList()
    }

    private fun recordLog(feature: String, result: AiProviderResult) {
        val entry = AiRequestLog(
            id = "log_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            feature = feature,
            provider = result.providerName,
            model = result.modelName,
            endpointHost = result.endpointHost,
            status = if (result.success) "SUCCESS" else "FAILED",
            httpStatus = result.httpStatus,
            latencyMs = result.latencyMs,
            sanitizedError = result.errorMessage
        )
        val current = _requestLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 30) {
            _requestLogs.value = current.take(30)
        } else {
            _requestLogs.value = current
        }
    }

    private fun cleanJsonString(input: String): String {
        var s = input.trim()
        if (s.startsWith("```json")) {
            s = s.removePrefix("```json").trim()
        } else if (s.startsWith("```")) {
            s = s.removePrefix("```").trim()
        }
        if (s.endsWith("```")) {
            s = s.removeSuffix("```").trim()
        }
        return s.trim()
    }

    companion object {
        val instance: AiManager by lazy { AiManager() }
    }
}
