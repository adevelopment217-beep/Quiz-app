package com.example.data.repository

import android.content.Context
import com.example.core.ai.AiManager
import com.example.core.database.*
import com.example.core.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminRepository private constructor(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val educationDao = db.educationDao()
    private val announcementDao = db.announcementDao()
    private val moshi = Moshi.Builder().build()
    private val importAdapter = moshi.adapter(QuizImportSchema::class.java)

    private var firestore: FirebaseFirestore? = null

    // In-memory or Firestore-backed AI Correction Reports
    private val _correctionReports = MutableStateFlow<List<AiCorrectionReport>>(emptyList())
    val correctionReports = _correctionReports.asStateFlow()

    // In-memory or Firestore-backed Audit Logs
    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs = _auditLogs.asStateFlow()

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            firestore = null
        }
        // Initialize with default sample correction reports & audit log
        _auditLogs.value = listOf(
            AuditLog(
                id = "log_init",
                adminUid = "admin_root",
                action = "SYSTEM_INITIALIZED",
                collection = "system",
                documentId = "init",
                details = "Admin panel and curriculum seed successfully loaded."
            )
        )
    }

    // --- Content Queries for Admin ---
    fun getAllClasses(): Flow<List<EducationClass>> {
        return educationDao.getAllClassesAdmin().map { list -> list.map { it.toDomain() } }
    }

    fun getSubjects(classId: String): Flow<List<Subject>> {
        return educationDao.getSubjectsByClassAdmin(classId).map { list -> list.map { it.toDomain() } }
    }

    fun getChapters(subjectId: String): Flow<List<Chapter>> {
        return educationDao.getChaptersBySubjectAdmin(subjectId).map { list -> list.map { it.toDomain() } }
    }

    fun getQuizzes(chapterId: String): Flow<List<Quiz>> {
        return educationDao.getQuizzesByChapterAdmin(chapterId).map { list -> list.map { it.toDomain() } }
    }

    fun getQuestions(quizId: String): Flow<List<Question>> {
        return educationDao.getQuestionsByQuiz(quizId).map { list -> list.map { it.toDomain() } }
    }

    // --- Content CRUD: Classes ---
    suspend fun saveClass(eduClass: EducationClass, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = ClassEntity(
            id = eduClass.id.ifBlank { "class_${System.currentTimeMillis()}" },
            name = eduClass.name,
            bengaliName = eduClass.bengaliName,
            order = eduClass.order,
            iconName = eduClass.iconName,
            isActive = eduClass.isActive
        )
        educationDao.insertClass(entity)
        recordAudit(adminUid, "CLASS_SAVED", "classes", entity.id, "Class ${entity.name} saved")
        firestore?.collection("classes")?.document(entity.id)?.set(entity)
    }

    suspend fun toggleEnableClass(classId: String, enabled: Boolean, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getClassById(classId) ?: return@withContext
        val updated = item.copy(isActive = enabled)
        educationDao.insertClass(updated)
        recordAudit(adminUid, if (enabled) "CLASS_ENABLED" else "CLASS_DISABLED", "classes", classId, "Class status updated")
        firestore?.collection("classes")?.document(classId)?.update("isActive", enabled)
    }

    suspend fun reorderClass(classId: String, newOrder: Int, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getClassById(classId) ?: return@withContext
        val updated = item.copy(order = newOrder)
        educationDao.insertClass(updated)
        recordAudit(adminUid, "CLASS_REORDERED", "classes", classId, "Order changed to $newOrder")
        firestore?.collection("classes")?.document(classId)?.update("order", newOrder)
    }

    suspend fun deleteClass(classId: String, adminUid: String) = withContext(Dispatchers.IO) {
        educationDao.deleteClassById(classId)
        recordAudit(adminUid, "CLASS_DELETED", "classes", classId, "Class deleted")
        firestore?.collection("classes")?.document(classId)?.delete()
    }

    // --- Content CRUD: Subjects ---
    suspend fun saveSubject(subject: Subject, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = SubjectEntity(
            id = subject.id.ifBlank { "sub_${System.currentTimeMillis()}" },
            classId = subject.classId,
            name = subject.name,
            bengaliName = subject.bengaliName,
            iconName = subject.iconName,
            colorHex = subject.colorHex,
            order = subject.order,
            isActive = subject.isActive
        )
        educationDao.insertSubject(entity)
        recordAudit(adminUid, "SUBJECT_SAVED", "subjects", entity.id, "Subject ${entity.bengaliName} saved")
        firestore?.collection("subjects")?.document(entity.id)?.set(entity)
    }

    suspend fun toggleEnableSubject(subjectId: String, enabled: Boolean, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getSubjectById(subjectId) ?: return@withContext
        val updated = item.copy(isActive = enabled)
        educationDao.insertSubject(updated)
        recordAudit(adminUid, if (enabled) "SUBJECT_ENABLED" else "SUBJECT_DISABLED", "subjects", subjectId, "Subject status updated")
        firestore?.collection("subjects")?.document(subjectId)?.update("isActive", enabled)
    }

    suspend fun reorderSubject(subjectId: String, newOrder: Int, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getSubjectById(subjectId) ?: return@withContext
        val updated = item.copy(order = newOrder)
        educationDao.insertSubject(updated)
        recordAudit(adminUid, "SUBJECT_REORDERED", "subjects", subjectId, "Order changed to $newOrder")
        firestore?.collection("subjects")?.document(subjectId)?.update("order", newOrder)
    }

    suspend fun deleteSubject(subjectId: String, adminUid: String) = withContext(Dispatchers.IO) {
        educationDao.deleteSubjectById(subjectId)
        recordAudit(adminUid, "SUBJECT_DELETED", "subjects", subjectId, "Subject deleted")
        firestore?.collection("subjects")?.document(subjectId)?.delete()
    }

    // --- Content CRUD: Chapters ---
    suspend fun saveChapter(chapter: Chapter, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = ChapterEntity(
            id = chapter.id.ifBlank { "chap_${System.currentTimeMillis()}" },
            subjectId = chapter.subjectId,
            classId = chapter.classId,
            title = chapter.title,
            bengaliTitle = chapter.bengaliTitle,
            chapterNumber = chapter.chapterNumber,
            description = chapter.description,
            order = chapter.order,
            isActive = chapter.isActive
        )
        educationDao.insertChapter(entity)
        recordAudit(adminUid, "CHAPTER_SAVED", "chapters", entity.id, "Chapter ${entity.bengaliTitle} saved")
        firestore?.collection("chapters")?.document(entity.id)?.set(entity)
    }

    suspend fun toggleEnableChapter(chapterId: String, enabled: Boolean, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getChapterById(chapterId) ?: return@withContext
        val updated = item.copy(isActive = enabled)
        educationDao.insertChapter(updated)
        recordAudit(adminUid, if (enabled) "CHAPTER_ENABLED" else "CHAPTER_DISABLED", "chapters", chapterId, "Chapter status updated")
        firestore?.collection("chapters")?.document(chapterId)?.update("isActive", enabled)
    }

    suspend fun reorderChapter(chapterId: String, newOrder: Int, adminUid: String) = withContext(Dispatchers.IO) {
        val item = educationDao.getChapterById(chapterId) ?: return@withContext
        val updated = item.copy(order = newOrder)
        educationDao.insertChapter(updated)
        recordAudit(adminUid, "CHAPTER_REORDERED", "chapters", chapterId, "Order changed to $newOrder")
        firestore?.collection("chapters")?.document(chapterId)?.update("order", newOrder)
    }

    suspend fun deleteChapter(chapterId: String, adminUid: String) = withContext(Dispatchers.IO) {
        educationDao.deleteChapterById(chapterId)
        recordAudit(adminUid, "CHAPTER_DELETED", "chapters", chapterId, "Chapter deleted")
        firestore?.collection("chapters")?.document(chapterId)?.delete()
    }

    // --- Content CRUD: Quizzes ---
    suspend fun saveQuiz(quiz: Quiz, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = QuizEntity(
            id = quiz.id.ifBlank { "quiz_${System.currentTimeMillis()}" },
            chapterId = quiz.chapterId,
            subjectId = quiz.subjectId,
            classId = quiz.classId,
            title = quiz.title,
            description = quiz.description,
            difficulty = quiz.difficulty,
            timeLimitSeconds = quiz.timeLimitSeconds,
            shuffleQuestions = quiz.shuffleQuestions,
            shuffleOptions = quiz.shuffleOptions,
            isPublished = quiz.isPublished,
            questionsCount = quiz.questionsCount,
            totalPoints = quiz.totalPoints,
            createdAt = if (quiz.createdAt > 0) quiz.createdAt else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        educationDao.insertQuiz(entity)
        recordAudit(adminUid, "QUIZ_SAVED", "quizzes", entity.id, "Quiz ${entity.title} saved")
        firestore?.collection("quizzes")?.document(entity.id)?.set(entity)
    }

    suspend fun togglePublishQuiz(quizId: String, publish: Boolean, adminUid: String) = withContext(Dispatchers.IO) {
        val quiz = educationDao.getQuizById(quizId) ?: return@withContext
        val updated = quiz.copy(isPublished = publish, updatedAt = System.currentTimeMillis())
        educationDao.insertQuiz(updated)
        recordAudit(adminUid, if (publish) "QUIZ_PUBLISHED" else "QUIZ_UNPUBLISHED", "quizzes", quizId, "Status updated")
        firestore?.collection("quizzes")?.document(quizId)?.update("isPublished", publish)
    }

    suspend fun deleteQuiz(quizId: String, adminUid: String) = withContext(Dispatchers.IO) {
        educationDao.deleteQuizById(quizId)
        educationDao.deleteQuestionsByQuiz(quizId)
        recordAudit(adminUid, "QUIZ_DELETED", "quizzes", quizId, "Quiz deleted")
        firestore?.collection("quizzes")?.document(quizId)?.delete()
    }

    // --- Content CRUD: Questions ---
    suspend fun saveQuestion(question: Question, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = QuestionEntity(
            id = question.id.ifBlank { "q_${System.currentTimeMillis()}" },
            quizId = question.quizId,
            type = question.type,
            questionText = question.questionText,
            options = question.options,
            answer = question.answer,
            acceptedAnswers = question.acceptedAnswers,
            explanation = question.explanation,
            points = question.points,
            imageUrl = question.imageUrl,
            order = question.order
        )
        educationDao.insertQuestion(entity)
        // Update quiz questions count
        val currentQuestions = educationDao.getQuestionsByQuizDirect(question.quizId)
        val quiz = educationDao.getQuizById(question.quizId)
        if (quiz != null) {
            val updated = quiz.copy(
                questionsCount = currentQuestions.size,
                totalPoints = currentQuestions.sumOf { it.points }
            )
            educationDao.insertQuiz(updated)
        }
        recordAudit(adminUid, "QUESTION_SAVED", "questions", entity.id, "Question saved for quiz ${question.quizId}")
        firestore?.collection("questions")?.document(entity.id)?.set(entity)
    }

    suspend fun deleteQuestion(questionId: String, quizId: String, adminUid: String) = withContext(Dispatchers.IO) {
        educationDao.deleteQuestionById(questionId)
        val currentQuestions = educationDao.getQuestionsByQuizDirect(quizId)
        val quiz = educationDao.getQuizById(quizId)
        if (quiz != null) {
            val updated = quiz.copy(
                questionsCount = currentQuestions.size,
                totalPoints = currentQuestions.sumOf { it.points }
            )
            educationDao.insertQuiz(updated)
        }
        recordAudit(adminUid, "QUESTION_DELETED", "questions", questionId, "Question removed")
        firestore?.collection("questions")?.document(questionId)?.delete()
    }

    // --- JSON Quiz Import Workflow ---
    fun parseJsonImport(jsonString: String): Result<QuizImportSchema> {
        return try {
            val parsed = importAdapter.fromJson(jsonString)
            if (parsed != null && parsed.title.isNotBlank() && parsed.questions.isNotEmpty()) {
                Result.success(parsed)
            } else {
                Result.failure(Exception("JSON is missing required title or questions array."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Invalid JSON format: ${e.localizedMessage}"))
        }
    }

    suspend fun saveImportedQuiz(
        schema: QuizImportSchema,
        classId: String,
        subjectId: String,
        chapterId: String,
        adminUid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val quizId = "quiz_imp_${System.currentTimeMillis()}"
            val questions = schema.questions.mapIndexed { index, q ->
                QuestionEntity(
                    id = "q_imp_${quizId}_$index",
                    quizId = quizId,
                    type = q.type.lowercase(),
                    questionText = q.question,
                    options = q.options,
                    answer = q.answer,
                    acceptedAnswers = q.acceptedAnswers,
                    explanation = q.explanation,
                    points = if (q.points > 0) q.points else 1,
                    imageUrl = null,
                    order = index + 1
                )
            }

            val quizEntity = QuizEntity(
                id = quizId,
                chapterId = chapterId,
                subjectId = subjectId,
                classId = classId,
                title = schema.title,
                description = schema.description,
                difficulty = schema.difficulty,
                timeLimitSeconds = if (schema.timeLimit > 0) schema.timeLimit else 300,
                shuffleQuestions = schema.shuffleQuestions,
                shuffleOptions = schema.shuffleOptions,
                isPublished = true,
                questionsCount = questions.size,
                totalPoints = questions.sumOf { it.points },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            educationDao.insertQuiz(quizEntity)
            educationDao.insertQuestions(questions)
            recordAudit(adminUid, "JSON_QUIZ_IMPORTED", "quizzes", quizId, "Imported '${schema.title}' with ${questions.size} questions")
            firestore?.collection("quizzes")?.document(quizId)?.set(quizEntity)
            for (q in questions) {
                firestore?.collection("questions")?.document(q.id)?.set(q)
            }
            return@withContext Result.success(quizId)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // --- AI Correction Reports Workflow ---
    fun addCorrectionReports(reports: List<AiCorrectionReport>) {
        val current = _correctionReports.value.toMutableList()
        current.addAll(reports)
        _correctionReports.value = current
    }

    suspend fun approveCorrection(report: AiCorrectionReport, adminUid: String) = withContext(Dispatchers.IO) {
        // Update question in database
        val questions = educationDao.getQuestionsByQuizDirect(report.quizId)
        val target = questions.find { it.id == report.questionId }
        if (target != null) {
            val updated = target.copy(answer = report.proposedValue)
            educationDao.insertQuestion(updated)
            firestore?.collection("questions")?.document(target.id)?.update("answer", report.proposedValue)
        }

        // Mark report as approved
        val list = _correctionReports.value.map {
            if (it.id == report.id) {
                it.copy(
                    status = CorrectionStatus.APPROVED.name,
                    reviewedBy = adminUid,
                    reviewedAt = System.currentTimeMillis()
                )
            } else it
        }
        _correctionReports.value = list
        recordAudit(adminUid, "CORRECTION_APPROVED", "aiCorrectionReports", report.id, "Changed '${report.currentValue}' -> '${report.proposedValue}' for question ${report.questionId}")
    }

    suspend fun rejectCorrection(reportId: String, adminUid: String) = withContext(Dispatchers.IO) {
        val list = _correctionReports.value.map {
            if (it.id == reportId) {
                it.copy(
                    status = CorrectionStatus.REJECTED.name,
                    reviewedBy = adminUid,
                    reviewedAt = System.currentTimeMillis()
                )
            } else it
        }
        _correctionReports.value = list
        recordAudit(adminUid, "CORRECTION_REJECTED", "aiCorrectionReports", reportId, "Admin rejected AI proposed correction")
    }

    // --- Announcements ---
    suspend fun saveAnnouncement(announcement: Announcement, adminUid: String) = withContext(Dispatchers.IO) {
        val entity = AnnouncementEntity(
            id = announcement.id.ifBlank { "ann_${System.currentTimeMillis()}" },
            title = announcement.title,
            message = announcement.message,
            date = announcement.date,
            isImportant = announcement.isImportant,
            isActive = announcement.isActive,
            createdAt = System.currentTimeMillis()
        )
        announcementDao.insertAnnouncement(entity)
        recordAudit(adminUid, "ANNOUNCEMENT_CREATED", "announcements", entity.id, entity.title)
        firestore?.collection("announcements")?.document(entity.id)?.set(entity)
    }

    suspend fun deleteAnnouncement(id: String, adminUid: String) = withContext(Dispatchers.IO) {
        announcementDao.deleteAnnouncement(id)
        recordAudit(adminUid, "ANNOUNCEMENT_DELETED", "announcements", id, "Announcement removed")
        firestore?.collection("announcements")?.document(id)?.delete()
    }

    // --- Audit Logging ---
    private fun recordAudit(adminUid: String, action: String, collection: String, documentId: String, details: String) {
        val log = AuditLog(
            id = "log_${System.currentTimeMillis()}",
            adminUid = adminUid,
            action = action,
            collection = collection,
            documentId = documentId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        val list = _auditLogs.value.toMutableList()
        list.add(0, log)
        _auditLogs.value = list
        firestore?.collection("auditLogs")?.document(log.id)?.set(log)
    }

    // --- Dashboard Stats ---
    suspend fun getDashboardStats(): DashboardStats = withContext(Dispatchers.IO) {
        val classes = educationDao.getAllClasses().firstOrNull()?.size ?: 0
        val subjects = educationDao.getAllSubjects().firstOrNull()?.size ?: 0
        val chapters = educationDao.getAllChapters().firstOrNull()?.size ?: 0
        val quizzes = educationDao.getAllQuizzes().firstOrNull() ?: emptyList()
        val pendingReports = _correctionReports.value.count { it.status == CorrectionStatus.PENDING.name }

        return@withContext DashboardStats(
            totalClasses = classes,
            totalSubjects = subjects,
            totalChapters = chapters,
            totalQuizzes = quizzes.size,
            publishedQuizzes = quizzes.count { it.isPublished },
            pendingCorrections = pendingReports,
            totalUsers = 124, // simulated active ecosystem count
            activeQuizzes = quizzes.size,
            averageScore = 84.5
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AdminRepository? = null

        fun getInstance(context: Context): AdminRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AdminRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

data class DashboardStats(
    val totalClasses: Int = 10,
    val totalSubjects: Int = 0,
    val totalChapters: Int = 0,
    val totalQuizzes: Int = 0,
    val publishedQuizzes: Int = 0,
    val pendingCorrections: Int = 0,
    val totalUsers: Int = 0,
    val activeQuizzes: Int = 0,
    val averageScore: Double = 0.0
)
