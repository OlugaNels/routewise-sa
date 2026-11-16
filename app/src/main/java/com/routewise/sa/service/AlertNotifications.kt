package com.routewise.sa.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.routewise.sa.MainActivity
import com.routewise.sa.R

/**
 * Shows the high-priority "heads-up" hazard notification.
 * Used by both the Firebase Cloud Messaging service (alerts sent from the Sentinel website)
 * and [HazardNotifier] (new incidents reported by other drivers).
 *
 * Reference: Android Developers (n.d.) Create a notification. https://developer.android.com/develop/ui/views/notifications/build-notification
 */
object AlertNotifications {

    private const val TAG = "AlertNotifications"
    private const val CHANNEL_ID = "routewise_traffic_alerts_channel"

    fun show(context: Context, title: String, message: String, lat: Double, lng: Double) {
        // Android 13+ lets the user refuse notifications, so check before posting
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification skipped: POST_NOTIFICATIONS permission not granted")
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification channels exist from Android 8 (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RouteWise SA Traffic Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority traffic alerts and community hazard reports"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("latitude", lat)
            putExtra("longitude", lng)
            putExtra("notificationTitle", title)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
        Log.i(TAG, "Hazard notification shown: $title")
    }
}
