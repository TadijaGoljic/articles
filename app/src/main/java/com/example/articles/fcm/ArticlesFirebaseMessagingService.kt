package com.example.articles.fcm

import android.content.Intent
import android.util.Log
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.example.articles.AuthenticationActivity
import com.example.articles.db.ArticleDatabase
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class MessageType(val value: String) {
    SYNC("sync"),
    FORCE_SIGN_OUT("forceSignOut"),
    INVITATION("invitation");

    companion object {
        fun from(value: String?): MessageType? = entries.find { it.value == value }
    }
}

@AndroidEntryPoint
class ArticlesFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var triggerSync: TriggerSync
    @Inject lateinit var database: ArticleDatabase

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(LOG_TAG, "FCM message received: ${message.data}")
        when (MessageType.from(message.data["type"])) {
            MessageType.FORCE_SIGN_OUT -> forceSignOut()
            MessageType.SYNC -> triggerSync()
            MessageType.INVITATION -> handleInvitationMessage(message.data)
            null -> Unit
        }
    }

    private fun handleInvitationMessage(data: Map<String, String>) {
        val invitationId = data["invitationId"] ?: return
        if (isInvitationAlreadyNotified(this, invitationId)) return
        showInvitationNotification(
            context = this,
            invitationId = invitationId,
            fromEmail = data["fromEmail"].orEmpty(),
            groupName = data["groupName"].orEmpty()
        )
        markInvitationNotified(this, invitationId)
    }

    private fun forceSignOut() {
        CoroutineScope(Dispatchers.IO).launch {
            database.clearAllTables()
            Firebase.auth.signOut()
            val intent = Intent(this@ArticlesFirebaseMessagingService, AuthenticationActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "FCM token refreshed: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveFcmToken(this, uid, token)
    }

}
