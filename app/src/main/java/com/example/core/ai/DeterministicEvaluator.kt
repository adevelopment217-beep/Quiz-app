package com.example.core.ai

import com.example.core.model.AiEvaluationResult
import com.example.core.model.Question
import java.util.Locale

object DeterministicEvaluator {

    // Bengali numerals to English and vice versa
    private val bengaliToEnglishDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val wordToNumber = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
        "শূন্য" to "0", "এক" to "1", "দুই" to "2", "তিন" to "3", "চার" to "4",
        "পাঁচ" to "5", "ছয়" to "6", "ছয়" to "6", "সাত" to "7", "আট" to "8", "নয়" to "9", "নয়" to "9", "দশ" to "10"
    )

    fun normalizeText(input: String): String {
        var text = input.trim().lowercase(Locale.ROOT)
        // Convert Bengali digits to English digits
        val sb = java.lang.StringBuilder()
        for (ch in text) {
            sb.append(bengaliToEnglishDigits[ch] ?: ch)
        }
        text = sb.toString()
        // Remove trailing and leading punctuation (periods, commas, exclamation, quotes)
        text = text.replace(Regex("[.,!?;:'\"`।]+"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()

        // Check if word maps directly to number
        wordToNumber[text]?.let { return it }

        return text
    }

    /**
     * Evaluates user answer through Levels 1, 2, and 3
     */
    fun evaluate(question: Question, rawUserAnswer: String): AiEvaluationResult? {
        val userAnswer = rawUserAnswer.trim()
        val officialAnswer = question.answer.trim()

        // Level 1: Exact Match
        if (userAnswer.equals(officialAnswer, ignoreCase = false)) {
            return AiEvaluationResult(
                status = "CORRECT",
                confidence = 1.0,
                reason = "Exact match with official answer.",
                normalizedAnswer = userAnswer,
                suggestedCorrectAnswer = officialAnswer,
                explanation = question.explanation
            )
        }

        // Level 2: Normalized Match
        val normUser = normalizeText(userAnswer)
        val normOfficial = normalizeText(officialAnswer)
        if (normUser == normOfficial) {
            return AiEvaluationResult(
                status = "CORRECT",
                confidence = 0.99,
                reason = "Matches official answer after text & numeral normalization.",
                normalizedAnswer = normUser,
                suggestedCorrectAnswer = officialAnswer,
                explanation = question.explanation
            )
        }

        // Level 3: Accepted Answers
        for (accepted in question.acceptedAnswers) {
            val normAccepted = normalizeText(accepted)
            if (normUser == normAccepted || userAnswer.equals(accepted.trim(), ignoreCase = true)) {
                return AiEvaluationResult(
                    status = "CORRECT",
                    confidence = 0.98,
                    reason = "Matches alternative accepted answer: '$accepted'.",
                    normalizedAnswer = normUser,
                    suggestedCorrectAnswer = officialAnswer,
                    explanation = question.explanation
                )
            }
        }

        // For MCQ, if user selected an option that doesn't match official or accepted, it's INCORRECT
        if (question.type.equals("mcq", ignoreCase = true)) {
            return AiEvaluationResult(
                status = "INCORRECT",
                confidence = 1.0,
                reason = "Selected MCQ option does not match correct answer.",
                normalizedAnswer = normUser,
                suggestedCorrectAnswer = officialAnswer,
                explanation = question.explanation
            )
        }

        // For fill_blank, if no deterministic match found, return null so Level 4 (Semantic AI) can inspect
        return null
    }
}
