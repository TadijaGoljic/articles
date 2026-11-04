package com.example.articles.utils

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.articles.models.UserPreferences
import com.example.articles.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TriggerSync @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    private val executor = Executors.newSingleThreadExecutor()

    operator fun invoke() {
        if (!userPreferences.syncDataEnabled) return
        executor.execute {
            val workManager = WorkManager.getInstance(context)
            val passAlreadyWaiting = workManager.getWorkInfosForUniqueWork(SyncManager.NAME).get()
                .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            if (!passAlreadyWaiting) {
                workManager.enqueueUniqueWork(
                    SyncManager.NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    SyncManager.sync()
                )
            }
        }
    }
}
