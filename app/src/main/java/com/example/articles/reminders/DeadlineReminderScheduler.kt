package com.example.articles.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.articles.db.entities.ArticleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleOrCancel(article: ArticleEntity) {
        val dateTo = article.dateTo
        if (dateTo == null || article.bought) {
            cancel(article.idArticle)
            return
        }

        val reminderAt = Calendar.getInstance().apply {
            timeInMillis = dateTo
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR_OF_DAY)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val delay = reminderAt - System.currentTimeMillis()
        if (delay <= 0) {
            cancel(article.idArticle)
            return
        }

        val request = OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    KEY_ARTICLE_ID to article.idArticle.toString(),
                    KEY_ARTICLE_NAME to article.name,
                    KEY_GROUP_ID to article.groupId.toString()
                )
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(article.idArticle), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(articleId: UUID) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(articleId))
    }

    private fun workName(articleId: UUID) = "$WORK_NAME_PREFIX$articleId"

    companion object {
        private const val REMINDER_HOUR_OF_DAY = 9
        private const val WORK_NAME_PREFIX = "deadline_reminder_"
        const val KEY_ARTICLE_ID = "articleId"
        const val KEY_ARTICLE_NAME = "articleName"
        const val KEY_GROUP_ID = "groupId"
    }

}
