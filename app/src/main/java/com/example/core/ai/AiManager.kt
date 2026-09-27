package com.example.core.ai

import com.example.BuildConfig
import com.example.core.model.AiCorrectionReport
import com.example.core.model.AiEvaluationResult
import com.example.core.model.AiProviderConfig
import com.example.core.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiManager private constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var config: AiProviderConfig = AiProviderConfig()
        private set

    fun updateConfig(newConfig: AiProviderConfig) {
        config = newConfig
    }

    private fun getEffectiveApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.ifBlank { "" }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Multi-Level Evaluation:
     * Levels 1, 2, 3 -> DeterministicEvaluator
     * Level 4 -> Semantic AI (Gemini / Provider)
     * Level 5 -> Uncertain Fallback
     */
    suspend fun evaluateAnswer(
        question: Question,
        userAnswer: String
    ): AiEvaluationResult = withContext(Dispatchers.IO) {
        // Levels 1 to 3
        val deterministic = DeterministicEvaluator.evaluate(question, userAnswer)
        if (deterministic != null) {
            return@withContext deterministic
        }

        // If AI is disabled or empty answer
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
                You are an educational quiz answer evaluator.
                Question: "${question.questionText}"
                Question Type: "${question.type}"
                Official Correct Answer: "${question.answer}"
                Alternative Accepted Answers: ${question.acceptedAnswers.joinToString(", ")}
                User's Submitted Answer: "$userAnswer"
                Official Explanation: "${question.explanation}"

                Task: Determine whether the user's answer is semantically and educationally correct.
                For example, synonyms, translated names (e.g. Dhaka vs ঢাকা), full sentences containing the core answer, or spelling variations should be recognized as CORRECT if meaning matches.
                
                Respond ONLY with valid JSON in this exact structure:
                {
                  "status": "CORRECT" or "INCORRECT" or "UNCERTAIN",
                  "confidence": 0.95,
                  "reason": "Brief rationale in Bengali or English",
                  "normalizedAnswer": "Cleaned up user answer",
                  "suggestedCorrectAnswer": "Official correct answer",
                  "explanation": "Helpful educational explanation"
                }
            """.trimIndent()

            val aiResponse = callGeminiApi(prompt, jsonOutput = true)
            if (aiResponse.isNotBlank()) {
                val cleaned = cleanJsonString(aiResponse)
                val json = JSONObject(cleaned)
                return@withContext AiEvaluationResult(
                    status = json.optString("status", "UNCERTAIN"),
                    confidence = json.optDouble("confidence", 0.5),
                    reason = json.optString("reason", "AI evaluated answer."),
                    normalizedAnswer = json.optString("normalizedAnswer", userAnswer),
                    suggestedCorrectAnswer = json.optString("suggestedCorrectAnswer", question.answer),
                    explanation = json.optString("explanation", question.explanation)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Level 5: Uncertain / Safe fallback
        return@withContext AiEvaluationResult(
            status = "UNCERTAIN",
            confidence = 0.5,
            reason = "Semantic evaluation unavailable. Please review with teacher or teacher's key.",
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
            return@withContext "AI সহকারী বর্তমানে নিষ্ক্রিয় রয়েছে বা ইন্টারনেট সংযোগ নেই।"
        }

        val fullPrompt = """
            ${config.systemPrompt}
            
            Current Educational Context:
            $contextPrompt
            
            Student Question/Request:
            $userQuery
            
            Instructions:
            - Respond in clear, encouraging, friendly tone.
            - If student asks in Bengali ("এই প্রশ্নটা বুঝিয়ে দাও", "আমি কেন ভুল করেছি?", "এই অধ্যায় শেখাও"), answer in clear Bengali.
            - Provide clear step-by-step logic, concepts, and memory tips.
            - Format nicely with markdown bullet points if helpful.
        """.trimIndent()

        val response = callGeminiApi(fullPrompt, jsonOutput = false)
        if (response.isBlank()) {
            return@withContext "দুঃখিত, এই মুহূর্তে AI উত্তর তৈরি করা সম্ভব হয়নি। অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ বা API Key পরীক্ষা করুন।"
        }
        return@withContext response
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

            val response = callGeminiApi(prompt, jsonOutput = true)
            if (response.isNotBlank()) {
                val cleaned = cleanJsonString(response)
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
                            reason = item.optString("reason", "Detected by AI quality control."),
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

    /**
     * Direct Gemini REST API call with fallback
     */
    private suspend fun callGeminiApi(prompt: String, jsonOutput: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext ""
        }

        val model = if (config.model.isNotBlank()) config.model else "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("temperature", config.temperature)
        genConfig.put("maxOutputTokens", config.maxTokens)
        if (jsonOutput) {
            genConfig.put("responseMimeType", "application/json")
        }
        requestJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext ""
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            return@withContext text
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext ""
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
