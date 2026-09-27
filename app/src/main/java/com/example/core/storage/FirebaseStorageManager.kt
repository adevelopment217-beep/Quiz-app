package com.example.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirebaseStorageManager private constructor(context: Context) {

    private var storage: FirebaseStorage? = null

    init {
        try {
            storage = FirebaseStorage.getInstance()
        } catch (e: Exception) {
            storage = null
        }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val s = storage ?: return@withContext Result.failure(Exception("Firebase Storage is not initialized"))
        try {
            val ref = s.reference.child("users/$userId/profile.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putFile(imageUri, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileBitmap(userId: String, bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        val s = storage ?: return@withContext Result.failure(Exception("Firebase Storage is not initialized"))
        try {
            val ref = s.reference.child("users/$userId/profile.jpg")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(data, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadQuizMedia(quizId: String, fileName: String, data: ByteArray, contentType: String = "image/jpeg"): Result<String> = withContext(Dispatchers.IO) {
        val s = storage ?: return@withContext Result.failure(Exception("Firebase Storage is not initialized"))
        try {
            val uniqueName = "${UUID.randomUUID()}_$fileName"
            val ref = s.reference.child("quizzes/$quizId/$uniqueName")
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()
            ref.putBytes(data, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseStorageManager? = null

        fun getInstance(context: Context): FirebaseStorageManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseStorageManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
