package com.example.articles

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.example.articles.sync.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArticlesApplication : Application(), Configuration.Provider {

    @Inject lateinit var syncWorkerFactory: SyncWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(invitationsChannel)
        }
    }

    companion object {
        const val LOG_TAG = "ArticlesApplication"
        const val INVITATION_CHANNEL_ID = "invitations"
    }

}