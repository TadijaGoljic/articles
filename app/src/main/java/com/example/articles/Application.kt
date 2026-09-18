package com.example.articles

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.Configuration
import com.example.articles.sync.SyncWorkerFactory
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArticlesApplication : Application(), Configuration.Provider {

    @Inject lateinit var syncWorkerFactory: SyncWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initializePlaces()
    }

    private fun initializePlaces() {
        if (Places.isInitialized()) return
        val apiKey = packageManager
            .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData
            .getString("com.google.android.geo.API_KEY")
        if (!apiKey.isNullOrBlank()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(syncWorkerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val invitationsChannel = NotificationChannel(
                INVITATION_CHANNEL_ID,
                "Group Invitations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for group sharing invitations"
            }
            val groupActivityChannel = NotificationChannel(
                GROUP_ACTIVITY_CHANNEL_ID,
                "Group Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for changes in shared groups, such as comments and purchases"
            }
            val deadlineReminderChannel = NotificationChannel(
                DEADLINE_REMINDER_CHANNEL_ID,
                "Deadline Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for articles with an upcoming deadline"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannels(listOf(invitationsChannel, groupActivityChannel, deadlineReminderChannel))
        }
    }

    companion object {
        const val LOG_TAG = "ArticlesApplication"
        const val INVITATION_CHANNEL_ID = "invitations"
        const val GROUP_ACTIVITY_CHANNEL_ID = "group_activity"
        const val DEADLINE_REMINDER_CHANNEL_ID = "deadline_reminders"
    }

}