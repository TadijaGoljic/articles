package com.example.articles.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.example.articles.db.ArticleEntity
import com.example.articles.models.ModelMapper
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.viewmodels.ArticlesListViewModel.Companion.USERS_COLLECTION_NAME
import com.example.articles.viewmodels.ArticlesListViewModel.Companion.ARTICLES_COLLECTION_NAME
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit


class SyncManager(
    context: Context,
    parameters: WorkerParameters,
    private val userPreferences: UserPreferences,
    private val articleRepository: ArticleRepository,
    private val applicationContext: Context,
    private val modelMapper: ModelMapper
) : CoroutineWorker(context, parameters) {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()
        val articlesCollection = firestore
            .collection(USERS_COLLECTION_NAME)
            .document(uid)
            .collection(ARTICLES_COLLECTION_NAME)
        return try {
            sync(articlesCollection)
            Result.success()
        } catch (e: Exception) {
            Log.e("my_result", "Sync failed: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun sync(
        articlesCollection: CollectionReference
    ) {
        val localChanges = articleRepository
            .getAllArticles()
            .map { list ->
                list.forEach { Log.d("my_result", "${it.updatedAt}") }
                list.filter { it.updatedAt > userPreferences.lastSyncTimestamp }
            }
            .first()
        uploadLocalChanges(localChanges, articlesCollection)
        val remoteSnapshot = articlesCollection
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()
        val remoteEntities = remoteSnapshot.documents.mapNotNull {
            modelMapper.toArticleEntity(it, applicationContext)
        }
        if (remoteEntities.isNotEmpty()) {
            articleRepository.deleteAll()
            articleRepository.insertAll(
                remoteEntities.map { modelMapper.mapToUIArticle(it) }
            )
        }
        userPreferences.lastSyncTimestamp = System.currentTimeMillis()
    }

    private suspend fun uploadLocalChanges(
        articles: List<ArticleEntity>,
        collection: CollectionReference
    ) {
        for (article in articles) {
            val documentId = article.idArticle.toString()
            if (article.deleted) {
                collection.document(documentId).delete().await()
                articleRepository.delete(article.idArticle)
            } else {
                collection.document(documentId)
                    .set(modelMapper.mapToFirestoreMap(article, applicationContext))
                    .await()
            }
        }
    }

    companion object {
        const val NAME = "SyncManager"
        fun sync() = OneTimeWorkRequestBuilder<SyncManager>()
            .setConstraints(Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
    }

}