package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.EducationClass
import com.example.core.model.Subject
import com.example.core.model.Chapter
import com.example.core.model.Quiz
import com.example.core.model.Question
import com.example.core.model.QuizAttempt
import com.example.core.model.Announcement

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bengaliName: String,
    val order: Int,
    val iconName: String,
    val isActive: Boolean
) {
    fun toDomain() = EducationClass(id, name, bengaliName, order, iconName, isActive)
}

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val name: String,
    val bengaliName: String,
    val iconName: String,
    val colorHex: String,
    val order: Int,
    val isActive: Boolean
) {
    fun toDomain() = Subject(id, classId, name, bengaliName, iconName, colorHex, order, isActive)
}

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val classId: String,
    val title: String,
    val bengaliTitle: String,
    val chapterNumber: Int,
    val description: String,
    val order: Int,
    val isActive: Boolean
) {
    fun toDomain() = Chapter(id, subjectId, classId, title, bengaliTitle, chapterNumber, description, order, isActive)
}

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val subjectId: String,
    val classId: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val timeLimitSeconds: Int,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val isPublished: Boolean,
    val questionsCount: Int,
    val totalPoints: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = Quiz(
        id, chapterId, subjectId, classId, title, description,
        difficulty, timeLimitSeconds, shuffleQuestions, shuffleOptions,
        isPublished, questionsCount, totalPoints, createdAt, updatedAt
    )
}

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val type: String,
    val questionText: String,
    val options: List<String>,
    val answer: String,
    val acceptedAnswers: List<String>,
    val explanation: String,
    val points: Int,
    val imageUrl: String?,
    val order: Int
) {
    fun toDomain() = Question(id, quizId, type, questionText, options, answer, acceptedAnswers, explanation, points, imageUrl, order)
}

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val quizTitle: String,
    val userId: String,
    val mode: String,
    val startedAt: Long,
    val completedAt: Long,
    val score: Int,
    val totalPoints: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val accuracyPercentage: Double,
    val timeTakenSeconds: Int,
    val answersJson: String,
    val isSynced: Boolean
) {
    fun toDomain() = QuizAttempt(
        id, quizId, quizTitle, userId, mode, startedAt, completedAt,
        score, totalPoints, correctCount, incorrectCount, unansweredCount,
        accuracyPercentage, timeTakenSeconds, answersJson, isSynced
    )
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String, // e.g. "quiz_${quizId}_${userId}"
    val userId: String,
    val targetType: String, // QUIZ, CHAPTER
    val targetId: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val date: String,
    val isImportant: Boolean,
    val isActive: Boolean,
    val createdAt: Long
) {
    fun toDomain() = Announcement(id, title, message, date, isImportant, isActive, createdAt)
}
