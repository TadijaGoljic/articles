package com.example.articles.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.articles.ArticlesActivity
import com.example.articles.ArticlesApplication
import com.example.articles.R
import com.example.articles.ui.composables.GROUP_DETAILS_DEEP_LINK

class DeadlineReminderWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(DeadlineReminderScheduler.KEY_ARTICLE_ID) ?: return Result.failure()
        val articleName = inputData.getString(DeadlineReminderScheduler.KEY_ARTICLE_NAME) ?: return Result.failure()
        val groupId = inputData.getString(DeadlineReminderScheduler.KEY_GROUP_ID) ?: return Result.failure()

        showDeadlineReminderNotification(
            context = applicationContext,
            groupId = groupId,
            articleId = articleId,
            articleName = articleName
        )
        return Result.success()
    }

}

private fun showDeadlineReminderNotification(
    context: Context,
    groupId: String,
    articleId: String,
    articleName: String
) {
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
    val body = context.getString(R.string.deadline_reminder_body, articleName)
    val notification = NotificationCompat.Builder(context, ArticlesApplication.DEADLINE_REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(context.getString(R.string.deadline_reminder_title))
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    context.getSystemService<NotificationManager>()
        ?.notify(articleId.hashCode(), notification)
}
