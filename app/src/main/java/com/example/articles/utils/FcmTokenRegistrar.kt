package com.example.articles.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

private const val DEVICE_ID_PREFS = "device_id"
private const val DEVICE_ID_KEY = "id"

private fun getOrCreateDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences(DEVICE_ID_PREFS, Context.MODE_PRIVATE)
    prefs.getString(DEVICE_ID_KEY, null)?.let { return it }
    val id = UUID.randomUUID().toString()
    prefs.edit { putString(DEVICE_ID_KEY, id) }
    return id
}

fun saveFcmToken(context: Context, uid: String, token: String) {
    val deviceId = getOrCreateDeviceId(context)
    Firebase.firestore
        .collection(USERS_COLLECTION)
        .document(uid)
        .set(mapOf("fcmTokens" to mapOf(deviceId to token)), SetOptions.merge())
        .addOnFailureListener { e ->
            Log.e(LOG_TAG, "Failed to save FCM token: ${e.message}")
        }
}

@Suppress("DEPRECATION")
fun registerFcmToken(context: Context, uid: String) {
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> saveFcmToken(context, uid, token) }
        .addOnFailureListener { e ->
            Log.e(LOG_TAG, "Failed to fetch FCM token: ${e.message}")
        }
}
