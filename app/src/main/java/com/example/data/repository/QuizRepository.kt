package com.example.data.repository

import android.content.Context
import com.example.core.database.*
import com.example.core.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class QuizRepository private constructor(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val educationDao = db.educationDao()
    private val attemptDao = db.attemptDao()
    private val favoriteDao = db.favoriteDao()
    private val announcementDao = db.announcementDao()

    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            firestore = null
        }
        // Ensure initial seeds are populated in Room
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
            syncFromFirestoreIfAvailable()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val existingClasses = educationDao.getAllClasses().firstOrNull()
        if (existingClasses.isNullOrEmpty()) {
            educationDao.insertClasses(CurriculumSeedData.getInitialClasses())
            educationDao.insertSubjects(CurriculumSeedData.getInitialSubjects())
            educationDao.insertChapters(CurriculumSeedData.getInitialChapters())
            educationDao.insertQuizzes(CurriculumSeedData.getInitialQuizzes())
            educationDao.insertQuestions(CurriculumSeedData.getInitialQuestions())
            announcementDao.insertAnnouncements(CurriculumSeedData.getInitialAnnouncements())
        }
    }

    private suspend fun syncFromFirestoreIfAvailable() {
        val fs = firestore ?: return
        try {
            // 1. Sync Classes
            val classDocs = fs.collection("classes").get().await()
            if (!classDocs.isEmpty) {
                val classList = classDocs.documents.mapNotNull { doc ->
                    ClassEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        bengaliName = doc.getString("bengaliName") ?: "",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        iconName = doc.getString("iconName") ?: "school",
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
                if (classList.isNotEmpty()) educationDao.insertClasses(classList)
            }

            // 2. Sync Subjects
            val subDocs = fs.collection("subjects").get().await()
            if (!subDocs.isEmpty) {
                val subList = subDocs.documents.mapNotNull { doc ->
                    SubjectEntity(
                        id = doc.id,
                        classId = doc.getString("classId") ?: "",
                        name = doc.getString("name") ?: "",
                        bengaliName = doc.getString("bengaliName") ?: "",
                        iconName = doc.getString("iconName") ?: "book",
                        colorHex = doc.getString("colorHex") ?: "#4F46E5",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
                if (subList.isNotEmpty()) educationDao.insertSubjects(subList)
            }

            // 3. Sync Chapters
            val chapDocs = fs.collection("chapters").get().await()
            if (!chapDocs.isEmpty) {
                val chapList = chapDocs.documents.mapNotNull { doc ->
                    ChapterEntity(
                        id = doc.id,
                        subjectId = doc.getString("subjectId") ?: "",
                        classId = doc.getString("classId") ?: "",
                        title = doc.getString("title") ?: "",
                        bengaliTitle = doc.getString("bengaliTitle") ?: "",
                        chapterNumber = doc.getLong("chapterNumber")?.toInt() ?: 1,
                        description = doc.getString("description") ?: "",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                }
                if (chapList.isNotEmpty()) educationDao.insertChapters(chapList)
            }

            // 4. Sync Quizzes
            val quizDocs = fs.collection("quizzes").get().await()
            if (!quizDocs.isEmpty) {
                val quizList = quizDocs.documents.mapNotNull { doc ->
                    QuizEntity(
                        id = doc.id,
                        chapterId = doc.getString("chapterId") ?: "",
                        subjectId = doc.getString("subjectId") ?: "",
                        classId = doc.getString("classId") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        difficulty = doc.getString("difficulty") ?: "medium",
                        timeLimitSeconds = doc.getLong("timeLimitSeconds")?.toInt() ?: 300,
                        shuffleQuestions = doc.getBoolean("shuffleQuestions") ?: true,
                        shuffleOptions = doc.getBoolean("shuffleOptions") ?: true,
                        isPublished = doc.getBoolean("isPublished") ?: true,
                        questionsCount = doc.getLong("questionsCount")?.toInt() ?: 0,
                        totalPoints = doc.getLong("totalPoints")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }
                if (quizList.isNotEmpty()) educationDao.insertQuizzes(quizList)
            }

            // 5. Sync Questions
            val questionDocs = fs.collection("questions").get().await()
            if (!questionDocs.isEmpty) {
                val qList = questionDocs.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val opts = doc.get("options") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val accepted = doc.get("acceptedAnswers") as? List<String> ?: emptyList()
                    QuestionEntity(
                        id = doc.id,
                        quizId = doc.getString("quizId") ?: "",
                        type = doc.getString("type") ?: "mcq",
                        questionText = doc.getString("questionText") ?: "",
                        options = opts,
                        answer = doc.getString("answer") ?: "",
                        acceptedAnswers = accepted,
                        explanation = doc.getString("explanation") ?: "",
                        points = doc.getLong("points")?.toInt() ?: 1,
                        imageUrl = doc.getString("imageUrl"),
                        order = doc.getLong("order")?.toInt() ?: 0
                    )
                }
                if (qList.isNotEmpty()) educationDao.insertQuestions(qList)
            }

            // 6. Sync Announcements
            val annDocs = fs.collection("announcements").get().await()
            if (!annDocs.isEmpty) {
                val annList = annDocs.documents.mapNotNull { doc ->
                    AnnouncementEntity(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        date = doc.getString("date") ?: "",
                        isImportant = doc.getBoolean("isImportant") ?: false,
                        isActive = doc.getBoolean("isActive") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                if (annList.isNotEmpty()) announcementDao.insertAnnouncements(annList)
            }
        } catch (e: Exception) {
            // Offline or credentials not ready, room cache keeps working
        }
    }

    fun getClasses(): Flow<List<EducationClass>> {
        return educationDao.getAllClasses().map { list -> list.map { it.toDomain() } }
    }

    fun getSubjects(classId: String): Flow<List<Subject>> {
        return educationDao.getSubjectsByClass(classId).map { list -> list.map { it.toDomain() } }
    }

    fun getAllSubjects(): Flow<List<Subject>> {
        return educationDao.getAllSubjects().map { list -> list.map { it.toDomain() } }
    }

    fun getChapters(subjectId: String): Flow<List<Chapter>> {
        return educationDao.getChaptersBySubject(subjectId).map { list -> list.map { it.toDomain() } }
    }

    fun getQuizzes(chapterId: String): Flow<List<Quiz>> {
        return educationDao.getQuizzesByChapter(chapterId).map { list -> list.map { it.toDomain() } }
    }

    fun getQuizzesByClass(classId: String): Flow<List<Quiz>> {
        return educationDao.getQuizzesByClass(classId).map { list -> list.map { it.toDomain() } }
    }

    fun getAllQuizzes(): Flow<List<Quiz>> {
        return educationDao.getAllQuizzes().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getQuiz(quizId: String): Quiz? = withContext(Dispatchers.IO) {
        return@withContext educationDao.getQuizById(quizId)?.toDomain()
    }

    fun getQuestions(quizId: String): Flow<List<Question>> {
        return educationDao.getQuestionsByQuiz(quizId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getQuestionsDirect(quizId: String): List<Question> = withContext(Dispatchers.IO) {
        return@withContext educationDao.getQuestionsByQuizDirect(quizId).map { it.toDomain() }
    }

    suspend fun recordAttempt(attempt: QuizAttempt) = withContext(Dispatchers.IO) {
        val entity = AttemptEntity(
            id = attempt.id,
            quizId = attempt.quizId,
            quizTitle = attempt.quizTitle,
            userId = attempt.userId,
            mode = attempt.mode,
            startedAt = attempt.startedAt,
            completedAt = attempt.completedAt,
            score = attempt.score,
            totalPoints = attempt.totalPoints,
            correctCount = attempt.correctCount,
            incorrectCount = attempt.incorrectCount,
            unansweredCount = attempt.unansweredCount,
            accuracyPercentage = attempt.accuracyPercentage,
            timeTakenSeconds = attempt.timeTakenSeconds,
            answersJson = attempt.answersJson,
            isSynced = false
        )
        attemptDao.insertAttempt(entity)

        // Try syncing to Firestore
        firestore?.let { fs ->
            try {
                val map = hashMapOf(
                    "id" to attempt.id,
                    "quizId" to attempt.quizId,
                    "quizTitle" to attempt.quizTitle,
                    "userId" to attempt.userId,
                    "mode" to attempt.mode,
                    "score" to attempt.score,
                    "totalPoints" to attempt.totalPoints,
                    "correctCount" to attempt.correctCount,
                    "incorrectCount" to attempt.incorrectCount,
                    "unansweredCount" to attempt.unansweredCount,
                    "accuracyPercentage" to attempt.accuracyPercentage,
                    "timeTakenSeconds" to attempt.timeTakenSeconds,
                    "completedAt" to attempt.completedAt
                )
                fs.collection("attempts").document(attempt.id).set(map).await()
                attemptDao.markAttemptSynced(attempt.id)
            } catch (e: Exception) {
                // Stays unsynced in Room
            }
        }
    }

    fun getUserAttempts(userId: String): Flow<List<QuizAttempt>> {
        return attemptDao.getAttemptsByUser(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getBestAttempt(quizId: String, userId: String): QuizAttempt? = withContext(Dispatchers.IO) {
        return@withContext attemptDao.getBestAttempt(quizId, userId)?.toDomain()
    }

    fun getFavorites(userId: String): Flow<List<FavoriteEntity>> {
        return favoriteDao.getFavorites(userId)
    }

    fun isFavorite(userId: String, targetId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(userId, targetId)
    }

    suspend fun toggleFavorite(userId: String, targetType: String, targetId: String, title: String, subtitle: String) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(userId, targetId).firstOrNull() ?: false
        if (isFav) {
            favoriteDao.removeFavorite(userId, targetId)
            firestore?.collection("favorites")?.document("${userId}_$targetId")?.delete()
        } else {
            val entity = FavoriteEntity(
                id = "${userId}_$targetId",
                userId = userId,
                targetType = targetType,
                targetId = targetId,
                title = title,
                subtitle = subtitle,
                timestamp = System.currentTimeMillis()
            )
            favoriteDao.addFavorite(entity)
            firestore?.let { fs ->
                try {
                    val map = hashMapOf(
                        "userId" to userId,
                        "targetType" to targetType,
                        "targetId" to targetId,
                        "title" to title,
                        "subtitle" to subtitle,
                        "timestamp" to System.currentTimeMillis()
                    )
                    fs.collection("favorites").document("${userId}_$targetId").set(map)
                } catch (e: Exception) {
                    // offline
                }
            }
        }
    }

    fun getAnnouncements(): Flow<List<Announcement>> {
        return announcementDao.getActiveAnnouncements().map { list -> list.map { it.toDomain() } }
    }

    companion object {
        @Volatile
        private var INSTANCE: QuizRepository? = null

        fun getInstance(context: Context): QuizRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = QuizRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
