package com.example.articles.sync

import android.content.Context
import androidx.work.DelegatingWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import javax.inject.Inject
import javax.inject.Singleton

class SyncManagerFactory(
    private val userPreferences: UserPreferences,
    private val articleRepository: ArticleRepository,
    private val groupRepository: GroupRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val firestoreModelMapper: FirestoreModelMapper,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            SyncManager::class.java.name ->
                SyncManager(
                    appContext,
                    workerParameters,
                    userPreferences,
                    articleRepository,
                    groupRepository,
                    sharedGroupRepository,
                    firestoreModelMapper
                )
            else ->
                null
        }
    }
}

@Singleton
class SyncWorkerFactory @Inject constructor(
    userPreferences: UserPreferences,
    articleRepository: ArticleRepository,
    groupRepository: GroupRepository,
    sharedGroupRepository: SharedGroupRepository,
    firestoreModelMapper: FirestoreModelMapper,
) : DelegatingWorkerFactory() {
    init {
        addFactory(
            SyncManagerFactory(
                userPreferences = userPreferences,
                articleRepository = articleRepository,
                groupRepository = groupRepository,
                sharedGroupRepository = sharedGroupRepository,
                firestoreModelMapper = firestoreModelMapper
            )
        )
    }
}