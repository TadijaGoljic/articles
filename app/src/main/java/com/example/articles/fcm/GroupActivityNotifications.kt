package com.example.articles.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.example.articles.ArticlesActivity
import com.example.articles.ArticlesApplication
import com.example.articles.R
import com.example.articles.ui.composables.GROUP_DETAILS_DEEP_LINK

fun showGroupActivityNotification(context: Context, groupId: String, title: String, body: String) {
    val deepLinkUri = GROUP_DETAILS_DEEP_LINK.replace("{groupId}", groupId)
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        deepLinkUri.toUri(),
        context,
        ArticlesActivity::class.java
    )
    val pendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(deepLinkIntent)
        getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    val notification = NotificationCompat.Builder(context, ArticlesApplication.GROUP_ACTIVITY_CHANNEL_ID)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(title.ifBlank { context.getString(R.string.group_activity_notification_fallback_title) })
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setGroup(groupId)
        .build()

    context.getSystemService<NotificationManager>()
        ?.notify((groupId + body + System.nanoTime()).hashCode(), notification)
}
