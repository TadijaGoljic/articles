package com.example.articles

import android.app.Application
import androidx.work.Configuration
import com.example.articles.sync.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ArticlesApplication : Application(), Configuration.Provider {

    @Inject lateinit var syncWorkerFactory: SyncWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(syncWorkerFactory)
            .build()

}