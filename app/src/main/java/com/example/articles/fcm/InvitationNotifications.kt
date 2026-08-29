package com.example.articles.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.example.articles.ArticlesActivity
import com.example.articles.ArticlesApplication
import com.example.articles.R
import com.example.articles.ui.composables.INVITATIONS_DEEP_LINK

private const val NOTIFIED_INVITATIONS_PREFS = "notified_invitations"
private const val NOTIFIED_INVITATIONS_KEY = "ids"

fun isInvitationAlreadyNotified(context: Context, invitationId: String): Boolean {
    val prefs = context.getSharedPreferences(NOTIFIED_INVITATIONS_PREFS, Context.MODE_PRIVATE)
    return prefs.getStringSet(NOTIFIED_INVITATIONS_KEY, emptySet())?.contains(invitationId) == true
}

fun markInvitationNotified(context: Context, invitationId: String) {
    val prefs = context.getSharedPreferences(NOTIFIED_INVITATIONS_PREFS, Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(NOTIFIED_INVITATIONS_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
    existing.add(invitationId)
    prefs.edit { putStringSet(NOTIFIED_INVITATIONS_KEY, existing) }
}

fun showInvitationNotification(context: Context, invitationId: String, fromEmail: String, groupName: String) {
    val contentText = context.getString(R.string.new_invitation_notification, fromEmail, groupName)
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        INVITATIONS_DEEP_LINK.toUri(),
        context,
        ArticlesActivity::class.java
    )
    val pendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(deepLinkIntent)
        getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    val notification = NotificationCompat.Builder(context, ArticlesApplication.INVITATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle(context.getString(R.string.invitations))
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    context.getSystemService<NotificationManager>()
        ?.notify(invitationId.hashCode(), notification)
}
