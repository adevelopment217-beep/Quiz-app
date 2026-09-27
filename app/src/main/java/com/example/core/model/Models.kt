package com.example.core.model

import com.squareup.moshi.JsonClass

enum class UserRole {
    USER,
    ADMIN
}

@JsonClass(generateAdapter = true)
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.USER.name,
    val photoUrl: String = "",
    val selectedClassId: String = "class_9",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role.equals(UserRole.ADMIN.name, ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class EducationClass(
    val id: String = "",
    val name: String = "",
    val bengaliName: String = "",
    val order: Int = 0,
    val iconName: String = "school",
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Subject(
    val id: String = "",
    val classId: String = "",
    val name: String = "",
    val bengaliName: String = "",
    val iconName: String = "book",
    val colorHex: String = "#4F46E5",
    val order: Int = 0,
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Chapter(
    val id: String = "",
    val subjectId: String = "",
    val classId: String = "",
    val title: String = "",
    val bengaliTitle: String = "",
    val chapterNumber: Int = 1,
    val description: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Quiz(
    val id: String = "",
    val chapterId: String = "",
    val subjectId: String = "",
    val classId: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: String = "medium", // easy, medium, hard
    val timeLimitSeconds: Int = 300, // 0 = untimed
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val isPublished: Boolean = true,
    val questionsCount: Int = 0,
    val totalPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class QuestionType {
    MCQ,
    FILL_BLANK
}

@JsonClass(generateAdapter = true)
data class Question(
    val id: String = "",
    val quizId: String = "",
    val type: String = QuestionType.MCQ.name.lowercase(),
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = "",
    val points: Int = 1,
    val imageUrl: String? = null,
    val order: Int = 0
)

@JsonClass(generateAdapter = true)
data class UserAnswerRecord(
    val questionId: String = "",
    val selectedAnswer: String = "",
    val isCorrect: Boolean = false,
    val pointsEarned: Int = 0,
    val evaluationStatus: String = "DETERMINISTIC", // EXACT, NORMALIZED, ACCEPTED, AI_SEMANTIC, INCORRECT, UNCERTAIN
    val explanation: String = ""
)

@JsonClass(generateAdapter = true)
data class QuizAttempt(
    val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val userId: String = "",
    val mode: String = "EXAM", // PRACTICE or EXAM
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val score: Int = 0,
    val totalPoints: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val accuracyPercentage: Double = 0.0,
    val timeTakenSeconds: Int = 0,
    val answersJson: String = "", // JSON serialized Map<String, UserAnswerRecord>
    val isSynced: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AiEvaluationResult(
    val status: String = "UNCERTAIN", // CORRECT, INCORRECT, UNCERTAIN
    val confidence: Double = 0.0,
    val reason: String = "",
    val normalizedAnswer: String = "",
    val suggestedCorrectAnswer: String = "",
    val explanation: String = ""
)

enum class CorrectionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@JsonClass(generateAdapter = true)
data class AiCorrectionReport(
    val id: String = "",
    val quizId: String = "",
    val questionId: String = "",
    val quizTitle: String = "",
    val questionText: String = "",
    val issueType: String = "ANSWER_KEY_ERROR", // ANSWER_KEY_ERROR, AMBIGUOUS, DUPLICATE_OPTION, MALFORMED, TYPO
    val currentValue: String = "",
    val proposedValue: String = "",
    val reason: String = "",
    val confidence: Double = 0.95,
    val aiModel: String = "gemini-3.5-flash",
    val status: String = CorrectionStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class AiProviderConfig(
    val provider: String = "gemini", // gemini, openai, openrouter, custom
    val model: String = "gemini-3.5-flash",
    val baseUrl: String = "https://generativelanguage.googleapis.com/",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "You are MedhaQuiz AI, an empathetic, highly knowledgeable educational tutor specializing in Bangladesh Classes 1-10 curriculum.",
    val isEnabled: Boolean = true,
    val isAssistantEnabled: Boolean = true,
    val isSemanticEvalEnabled: Boolean = true,
    val isQualityCheckEnabled: Boolean = true,
    val apiKey: String = ""
)

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val date: String = "",
    val isImportant: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AuditLog(
    val id: String = "",
    val adminUid: String = "",
    val action: String = "", // QUIZ_CREATED, QUIZ_UPDATED, CORRECTION_APPROVED, etc.
    val collection: String = "",
    val documentId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class QuizImportSchema(
    val version: Int = 1,
    val title: String = "",
    val description: String = "",
    val difficulty: String = "medium",
    val timeLimit: Int = 300,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val questions: List<ImportQuestionSchema> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ImportQuestionSchema(
    val type: String = "mcq",
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String = "",
    val points: Int = 1
)
