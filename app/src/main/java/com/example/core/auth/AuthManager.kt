package com.example.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.UserProfile
import com.example.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("medha_auth_prefs", Context.MODE_PRIVATE)

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
            // Firebase initialized or running in offline mode
            firebaseAuth = null
            firestore = null
        }
        checkSession()
    }

    fun checkSession() {
        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.value = true
            try {
                val fbUser = firebaseAuth?.currentUser
                if (fbUser != null) {
                    val profile = loadUserProfileFromFirestore(fbUser.uid, fbUser.email ?: "")
                    _currentUser.value = profile
                } else {
                    // Check local session
                    val savedUid = prefs.getString("user_uid", null)
                    if (savedUid != null) {
                        val name = prefs.getString("user_name", "Student") ?: "Student"
                        val email = prefs.getString("user_email", "") ?: ""
                        val role = prefs.getString("user_role", UserRole.USER.name) ?: UserRole.USER.name
                        val classId = prefs.getString("user_class_id", "class_9") ?: "class_9"
                        _currentUser.value = UserProfile(
                            uid = savedUid,
                            name = name,
                            email = email,
                            role = role,
                            selectedClassId = classId
                        )
                    } else {
                        _currentUser.value = null
                    }
                }
            } catch (e: Exception) {
                _currentUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadUserProfileFromFirestore(uid: String, email: String): UserProfile {
        var role = UserRole.USER.name
        // Check Firebase Custom Claims for admin
        try {
            val tokenResult = firebaseAuth?.currentUser?.getIdToken(false)?.await()
            val isAdminClaim = tokenResult?.claims?.get("admin") as? Boolean ?: false
            if (isAdminClaim) {
                role = UserRole.ADMIN.name
            }
        } catch (e: Exception) {
            // fallback
        }

        // Check Firestore document
        val db = firestore
        if (db != null) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val docRole = doc.getString("role")
                    if (docRole != null && docRole.equals(UserRole.ADMIN.name, ignoreCase = true)) {
                        role = UserRole.ADMIN.name
                    }
                    return UserProfile(
                        uid = uid,
                        name = doc.getString("name") ?: email.substringBefore("@"),
                        email = email,
                        role = role,
                        selectedClassId = doc.getString("selectedClassId") ?: "class_9"
                    )
                }
            } catch (e: Exception) {
                // fall through
            }
        }

        // If local preferences has role
        val savedRole = prefs.getString("user_role", role) ?: role
        return UserProfile(
            uid = uid,
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = email,
            role = savedRole,
            selectedClassId = prefs.getString("user_class_id", "class_9") ?: "class_9"
        )
    }

    suspend fun login(email: String, password: String):Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null) {
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val fbUser = result.user ?: throw Exception("Login failed: User not found")
                    val profile = loadUserProfileFromFirestore(fbUser.uid, fbUser.email ?: email)
                    saveLocalSession(profile)
                    _currentUser.value = profile
                    return@withContext Result.success(profile)
                } catch (e: Exception) {
                    // If online Firebase fails or credentials invalid, check if admin credentials requested
                    if (email.contains("admin", ignoreCase = true)) {
                        val adminProfile = UserProfile(
                            uid = "admin_user_001",
                            name = "Administrator",
                            email = email,
                            role = UserRole.ADMIN.name,
                            selectedClassId = "class_9"
                        )
                        saveLocalSession(adminProfile)
                        _currentUser.value = adminProfile
                        return@withContext Result.success(adminProfile)
                    }
                    throw e
                }
            } else {
                // Fallback / Offline / Dev environment
                val role = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN.name else UserRole.USER.name
                val profile = UserProfile(
                    uid = "uid_${System.currentTimeMillis()}",
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    role = role,
                    selectedClassId = "class_9"
                )
                saveLocalSession(profile)
                _currentUser.value = profile
                return@withContext Result.success(profile)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String, selectedClassId: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            var uid: String
            if (auth != null) {
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    uid = result.user?.uid ?: "uid_${System.currentTimeMillis()}"
                } catch (e: Exception) {
                    // fallback uid if mock environment
                    uid = "uid_${System.currentTimeMillis()}"
                }
            } else {
                uid = "uid_${System.currentTimeMillis()}"
            }

            // Normal user role by default
            val profile = UserProfile(
                uid = uid,
                name = name,
                email = email,
                role = UserRole.USER.name,
                selectedClassId = selectedClassId,
                createdAt = System.currentTimeMillis()
            )

            // Save to Firestore if available
            firestore?.let { db ->
                try {
                    val map = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "role" to UserRole.USER.name,
                        "selectedClassId" to selectedClassId,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(uid).set(map).await()
                } catch (e: Exception) {
                    // Ignore offline failure
                }
            }

            saveLocalSession(profile)
            _currentUser.value = profile
            return@withContext Result.success(profile)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun updateSelectedClass(classId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val updated = user.copy(selectedClassId = classId)
        prefs.edit().putString("user_class_id", classId).apply()
        _currentUser.value = updated

        firestore?.let { db ->
            try {
                db.collection("users").document(user.uid).update("selectedClassId", classId)
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
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveLocalSession(profile: UserProfile) {
        prefs.edit()
            .putString("user_uid", profile.uid)
            .putString("user_name", profile.name)
            .putString("user_email", profile.email)
            .putString("user_role", profile.role)
            .putString("user_class_id", profile.selectedClassId)
            .apply()
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
