package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_MESSAGES_ID = "channel_messages"
    const val CHANNEL_FOLLOWS_ID = "channel_follows"

    const val TYPE_NEW_MESSAGE = "NEW_MESSAGE"
    const val TYPE_VOICE_NOTE = "VOICE_NOTE"
    const val TYPE_FOLLOW_REQUEST = "FOLLOW_REQUEST"

    const val EXTRA_OPEN_DESTINATION = "extra_open_destination"
    const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Direct Messages & Voice Notes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for incoming chat messages and audio voice notes"
                enableVibration(true)
            }

            val followsChannel = NotificationChannel(
                CHANNEL_FOLLOWS_ID,
                "Follow Requests & Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when creators or friends follow your profile"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(followsChannel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String,
        conversationId: String? = null
    ) {
        val channelId = if (type == TYPE_FOLLOW_REQUEST) CHANNEL_FOLLOWS_ID else CHANNEL_MESSAGES_ID

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DESTINATION, if (type == TYPE_FOLLOW_REQUEST) "PROFILE" else "MESSAGES")
            conversationId?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(if (type == TYPE_FOLLOW_REQUEST) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = (System.currentTimeMillis() % 100000).toInt()
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted: ${e.message}")
        }
    }
}
