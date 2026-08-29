package com.example.articles.utils

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.articles.models.UserPreferences
import com.example.articles.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TriggerSync @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    operator fun invoke() {
        if (userPreferences.syncDataEnabled) {
            WorkManager.getInstance(context).apply {
                enqueueUniqueWork(
                    SyncManager.NAME,
                    ExistingWorkPolicy.KEEP,
                    SyncManager.sync()
                )
            }
        }
    }
}