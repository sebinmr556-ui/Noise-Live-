package com.example.service

import android.util.Log
import com.example.data.AppRepository
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NoiceFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Cloud Messaging registration token generated: $token")
        try {
            AppRepository.getInstance(applicationContext).updateFcmToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist FCM token: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: "NOICE LIVE"
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "You have a new update"

        val type = data["type"] ?: when {
            body.contains("voice", ignoreCase = true) -> NotificationHelper.TYPE_VOICE_NOTE
            body.contains("follow", ignoreCase = true) -> NotificationHelper.TYPE_FOLLOW_REQUEST
            else -> NotificationHelper.TYPE_NEW_MESSAGE
        }

        val conversationId = data["conversationId"]

        // Trigger native notification
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            type = type,
            conversationId = conversationId
        )

        // Inject into in-app notification state
        try {
            val repository = AppRepository.getInstance(applicationContext)
            val senderName = data["senderName"] ?: title
            val senderAvatar = data["senderAvatar"] ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80"
            if (type == NotificationHelper.TYPE_FOLLOW_REQUEST) {
                repository.handleIncomingFollowNotification(senderName, senderAvatar)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update in-app notification state: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NoiceFCMService"
    }
}
