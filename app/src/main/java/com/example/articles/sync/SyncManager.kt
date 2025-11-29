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
import com.example.articles.models.UIArticle
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.google.firebase.Firebase
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

    private val articlesCollection = Firebase.firestore.collection("articles")

    override suspend fun doWork(): Result {
        Log.d("my_result", "start")
        val articlesToSync = articleRepository.getAllArticles().map { articles ->
            articles.filter { article ->
                article.updatedAt > userPreferences.lastSyncTimestamp
            }
        }.first()
        val syncResult = syncArticlesToFirestore(articlesToSync)
        if (syncResult.isSuccess) {
            userPreferences.lastSyncTimestamp = System.currentTimeMillis()
            Log.d("my_result", "success")
            return Result.success()
        }
        Log.d("my_result", "fail")
        return Result.retry()
    }

    private suspend fun syncArticlesToFirestore(articles: List<ArticleEntity>): SyncResult {
        var successCount = 0
        var errorCount = 0
        for (article in articles) {
            try {
                val documentId = article.idArticle.toString().takeIf { it.isNotBlank() }
                    ?: articlesCollection.document().id
                articlesCollection.document(documentId)
                    .set(modelMapper.mapToFirestoreMap(article, applicationContext)).await()
                successCount++
            } catch (e: Exception) {
                Log.e("my_result", "syncArticlesToFirestore: ${e.message}")
                errorCount++
            }
        }
        val snapshot = articlesCollection.get().await()
        snapshot.documents.forEach { document ->
            val entity = modelMapper.toArticleEntity(document, applicationContext)
            val article = if (entity != null) modelMapper.mapToUIArticle(entity) else UIArticle()
            articleRepository.insertAll(listOf(article))
        }
        return SyncResult(
            totalItems = articles.size,
            successfulItems = successCount,
            failedItems = errorCount
        )
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

data class SyncResult(
    val totalItems: Int,
    val successfulItems: Int,
    val failedItems: Int
) {
    val isSuccess: Boolean
        get() = failedItems == 0
}