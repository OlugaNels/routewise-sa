package com.routewise.sa.service

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.routewise.sa.data.SettingsManager

/**
 * Receives push messages sent through Firebase Cloud Messaging (for example an emergency broadcast
 * from the Sentinel Command Center) and shows them as notifications.
 *
 * Reference: Google (n.d.) Receive messages in an Android app. https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class RouteWiseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Only the length is logged: the token identifies this phone, so it is not written to the log
        Log.d(TAG, "FCM device token refreshed (length ${token.length})")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "⚠️ RouteWise SA Traffic Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Traffic or hazard update along your corridor."
        val lat = remoteMessage.data["latitude"]?.toDoubleOrNull() ?: -25.9983
        val lng = remoteMessage.data["longitude"]?.toDoubleOrNull() ?: 28.1278
        Log.i(TAG, "Push message received: $title")

        // Honour the "Hazard notifications" switch in Settings (added in 1.2.0)
        if (!SettingsManager(this).getToggle(SettingsManager.KEY_HAZARD_NOTIFICATIONS)) {
            Log.d(TAG, "Push message not shown: hazard notifications are switched off")
            return
        }

        AlertNotifications.show(this, title, body, lat, lng)

        // Save incoming notification to Cloud Firestore ("notifications" collection)
        saveNotificationToFirestore(title, body, lat, lng)
    }

    private fun saveNotificationToFirestore(title: String, message: String, lat: Double, lng: Double) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val notifMap = hashMapOf(
                "id" to "notif_${System.currentTimeMillis()}",
                "title" to title,
                "message" to message,
                "latitude" to lat,
                "longitude" to lng,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "impactBonus" to 25
            )
            firestore.collection("notifications").add(notifMap)
                .addOnFailureListener { e -> Log.e(TAG, "Could not save notification to Firestore: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save notification to Firestore: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "RouteWiseFCM"
    }
}
