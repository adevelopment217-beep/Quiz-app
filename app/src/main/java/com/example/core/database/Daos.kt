package com.example.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EducationDao {
    // Classes
    @Query("SELECT * FROM classes WHERE isActive = 1 ORDER BY `order` ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY `order` ASC")
    fun getAllClassesAdmin(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    // Subjects
    @Query("SELECT * FROM subjects WHERE classId = :classId AND isActive = 1 ORDER BY `order` ASC")
    fun getSubjectsByClass(classId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE classId = :classId ORDER BY `order` ASC")
    fun getSubjectsByClassAdmin(classId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE isActive = 1")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subjectEntity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    // Chapters
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isActive = 1 ORDER BY `order` ASC")
    fun getChaptersBySubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY `order` ASC")
    fun getChaptersBySubjectAdmin(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE isActive = 1")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapterEntity: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: String)

    // Quizzes
    @Query("SELECT * FROM quizzes WHERE chapterId = :chapterId AND isPublished = 1 ORDER BY createdAt DESC")
    fun getQuizzesByChapter(chapterId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun getQuizzesByChapterAdmin(chapterId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE classId = :classId AND isPublished = 1")
    fun getQuizzesByClass(classId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId LIMIT 1")
    suspend fun getQuizById(quizId: String): QuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quizEntity: QuizEntity)

    @Query("DELETE FROM quizzes WHERE id = :id")
    suspend fun deleteQuizById(id: String)

    // Questions
    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY `order` ASC")
    fun getQuestionsByQuiz(quizId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY `order` ASC")
    suspend fun getQuestionsByQuizDirect(quizId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: String)

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsByQuiz(quizId: String)
}

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE userId = :userId ORDER BY completedAt DESC")
    fun getAttemptsByUser(userId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE quizId = :quizId AND userId = :userId ORDER BY score DESC LIMIT 1")
    suspend fun getBestAttempt(quizId: String, userId: String): AttemptEntity?

    @Query("SELECT * FROM attempts WHERE isSynced = 0")
    suspend fun getUnsyncedAttempts(): List<AttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("UPDATE attempts SET isSynced = 1 WHERE id = :id")
    suspend fun markAttemptSynced(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND targetId = :targetId)")
    fun isFavorite(userId: String, targetId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND targetId = :targetId")
    suspend fun removeFavorite(userId: String, targetId: String)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(items: List<AnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(item: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}
