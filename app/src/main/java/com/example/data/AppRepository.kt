package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.UserProfile
import com.example.model.VipTier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppRepository private constructor(private val context: Context) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = auth.currentUser?.uid ?: "user_default_1",
            username = "",
            fullName = "",
            avatarUrl = "",
            bio = "",
            isNewUser = true,
            profileSetupCompleted = false,
            joinedAtMillis = System.currentTimeMillis(),
            vipLevel = 1,
            vipTier = VipTier.MEMBER,
            coins = 1200L
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: _currentUser.value.id
        if (uid.isNotBlank()) {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to user profile: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data != null) {
                            val profile = UserProfile.fromMap(data)
                            _currentUser.value = profile
                        }
                    }
                }
        }
    }

    suspend fun registerUserProfile(
        displayName: String,
        username: String,
        avatarUri: String,
        bio: String = "",
        country: String = "Global",
        gender: String = "Unspecified"
    ): Result<UserProfile> {
        return try {
            val uid = auth.currentUser?.uid ?: _currentUser.value.id
            val updated = _currentUser.value.copy(
                id = uid,
                fullName = displayName.trim(),
                username = username.trim().removePrefix("@"),
                avatarUrl = avatarUri,
                bio = bio.trim(),
                country = country,
                gender = gender,
                isNewUser = false,
                profileSetupCompleted = true,
                joinedAtMillis = if (_currentUser.value.joinedAtMillis > 0) _currentUser.value.joinedAtMillis else System.currentTimeMillis()
            )

            // Update in-memory state immediately for instant UI responsiveness
            _currentUser.value = updated

            // Persist to Firestore
            firestore.collection("users").document(uid)
                .set(updated.toMap(), SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully registered user profile: ${updated.username} ($uid)")
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun markUserAsActive(userId: String) {
        val uid = userId.ifBlank { _currentUser.value.id }
        _currentUser.value = _currentUser.value.copy(isNewUser = false)
        repositoryScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("isNewUser", false)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Could not mark user as active: ${e.message}")
            }
        }
    }

    fun updateFcmToken(token: String) {
        _fcmToken.value = token
        val uid = auth.currentUser?.uid ?: _currentUser.value.id
        if (uid.isNotBlank() && token.isNotBlank()) {
            repositoryScope.launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update FCM token in Firestore: ${e.message}")
                }
            }
        }
    }

    fun handleIncomingFollowNotification(senderName: String, senderAvatar: String) {
        Log.d(TAG, "Follow notification received from $senderName")
    }

    companion object {
        private const val TAG = "AppRepository"

        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
