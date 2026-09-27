package com.example.core.auth

import android.content.Context
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager private constructor(context: Context) {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            firebaseAuth = null
            firestore = null
        }
        checkSession()
    }

    fun checkSession() {
        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.value = true
            try {
                val auth = firebaseAuth
                val fbUser = auth?.currentUser
                if (fbUser != null) {
                    val profile = loadUserProfileFromFirebase(fbUser, forceRefreshClaim = true)
                    _currentUser.value = profile
                } else {
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                _currentUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads user profile from Firebase Firestore and verifies Firebase ID Token Custom Claims.
     * Admin role is ONLY granted if token custom claim contains `admin: true`.
     * SharedPreferences, local flags, or email strings are NOT trusted.
     */
    private suspend fun loadUserProfileFromFirebase(fbUser: FirebaseUser, forceRefreshClaim: Boolean): UserProfile {
        var isAdmin = false
        try {
            // Verify ID Token and inspect custom claims
            val tokenResult = fbUser.getIdToken(forceRefreshClaim).await()
            isAdmin = (tokenResult.claims["admin"] as? Boolean) == true
        } catch (e: Exception) {
            isAdmin = false
        }

        val role = if (isAdmin) UserRole.ADMIN.name else UserRole.USER.name
        val uid = fbUser.uid
        val email = fbUser.email ?: ""

        val db = firestore
        if (db != null) {
            try {
                val docRef = db.collection("users").document(uid)
                val snapshot = docRef.get().await()

                if (snapshot.exists()) {
                    val name = snapshot.getString("name") ?: fbUser.displayName ?: email.substringBefore("@")
                    val photoUrl = snapshot.getString("photoUrl") ?: fbUser.photoUrl?.toString() ?: ""
                    val selectedClassId = snapshot.getString("selectedClassId") ?: "class_9"
                    val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                    // Update lastLoginAt and ensure role in document reflects token claim
                    val updateMap = hashMapOf<String, Any>(
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "role" to role
                    )
                    docRef.set(updateMap, SetOptions.merge())

                    return UserProfile(
                        uid = uid,
                        name = name,
                        email = email,
                        role = role,
                        photoUrl = photoUrl,
                        selectedClassId = selectedClassId,
                        createdAt = createdAt,
                        updatedAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                } else {
                    // Create fresh user profile document in Firestore
                    val defaultName = fbUser.displayName?.ifBlank { null } ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val userDoc = hashMapOf<String, Any>(
                        "uid" to uid,
                        "name" to defaultName,
                        "email" to email,
                        "role" to role,
                        "photoUrl" to (fbUser.photoUrl?.toString() ?: ""),
                        "selectedClassId" to "class_9",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(userDoc).await()

                    return UserProfile(
                        uid = uid,
                        name = defaultName,
                        email = email,
                        role = role,
                        photoUrl = fbUser.photoUrl?.toString() ?: "",
                        selectedClassId = "class_9",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                // If offline / firestore network fails, construct basic profile from FirebaseUser
            }
        }

        return UserProfile(
            uid = uid,
            name = fbUser.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            role = role,
            photoUrl = fbUser.photoUrl?.toString() ?: "",
            selectedClassId = "class_9",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase Authentication is not available"))
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = authResult.user ?: throw Exception("লগইন ব্যর্থ হয়েছে: ব্যবহারকারী পাওয়া যায়নি")

            // Always force token refresh on login to read newest custom claims
            val profile = loadUserProfileFromFirebase(fbUser, forceRefreshClaim = true)
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        selectedClassId: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase Authentication is not available"))
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fbUser = authResult.user ?: throw Exception("নিবন্ধন ব্যর্থ হয়েছে")
            val uid = fbUser.uid

            // New registrations are always standard USER role. Android client cannot grant admin.
            val initialProfile = UserProfile(
                uid = uid,
                name = name.trim(),
                email = email.trim(),
                role = UserRole.USER.name,
                photoUrl = "",
                selectedClassId = selectedClassId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            // Save to Firestore users collection
            firestore?.let { db ->
                try {
                    val userDoc = hashMapOf<String, Any>(
                        "uid" to uid,
                        "name" to name.trim(),
                        "email" to email.trim(),
                        "role" to UserRole.USER.name,
                        "photoUrl" to "",
                        "selectedClassId" to selectedClassId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("users").document(uid).set(userDoc).await()
                } catch (e: Exception) {
                    // Ignore transient write failure if offline
                }
            }

            _currentUser.value = initialProfile
            Result.success(initialProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase Authentication is not available"))
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String, photoUrl: String = "", selectedClassId: String? = null): Result<UserProfile> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("ব্যবহারকারী লগইন করা নেই"))
        try {
            val updated = current.copy(
                name = name.trim(),
                photoUrl = photoUrl.ifBlank { current.photoUrl },
                selectedClassId = selectedClassId ?: current.selectedClassId,
                updatedAt = System.currentTimeMillis()
            )

            firestore?.let { db ->
                val updates = hashMapOf<String, Any>(
                    "name" to updated.name,
                    "photoUrl" to updated.photoUrl,
                    "selectedClassId" to updated.selectedClassId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Note: Role is intentionally NEVER updated here.
                db.collection("users").document(current.uid).update(updates).await()
            }

            _currentUser.value = updated
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSelectedClass(classId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val updated = user.copy(selectedClassId = classId)
        _currentUser.value = updated

        firestore?.let { db ->
            try {
                db.collection("users").document(user.uid).update(
                    "selectedClassId", classId,
                    "updatedAt", FieldValue.serverTimestamp()
                )
            } catch (e: Exception) {
                // offline
            }
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // ignore
        }
        _currentUser.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
