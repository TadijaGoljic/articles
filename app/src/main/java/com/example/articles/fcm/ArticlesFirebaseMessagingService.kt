package com.example.articles.fcm

import android.util.Log
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.example.articles.sync.SyncManager
import com.example.articles.utils.triggerSync
import android.provider.Settings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ArticlesFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(LOG_TAG, "FCM message received: ${message.data}")
        triggerSync(this)
    }

    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "FCM token refreshed: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Firebase.firestore
            .collection(SyncManager.USERS_COLLECTION_NAME)
            .document(uid)
            .set(mapOf("fcmTokens" to mapOf(deviceId to token)), SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "Failed to save FCM token: ${e.message}")
            }
    }

}
