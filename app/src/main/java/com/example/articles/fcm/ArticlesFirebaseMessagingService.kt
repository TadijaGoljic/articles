package com.example.articles.fcm

import android.util.Log
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import android.provider.Settings
import com.example.articles.utils.TriggerSync
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import javax.inject.Inject

class ArticlesFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var triggerSync: TriggerSync

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(LOG_TAG, "FCM message received: ${message.data}")
        triggerSync()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "FCM token refreshed: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Firebase.firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .set(mapOf("fcmTokens" to mapOf(deviceId to token)), SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "Failed to save FCM token: ${e.message}")
            }
    }

}
