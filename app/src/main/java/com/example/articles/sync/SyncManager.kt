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
import com.example.articles.ArticlesApplication
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UserPreferences
import com.example.articles.models.toUIArticle
import com.example.articles.models.toUIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.map


class SyncManager(
    context: Context,
    parameters: WorkerParameters,
    private val userPreferences: UserPreferences,
    private val articleRepository: ArticleRepository,
    private val groupRepository: GroupRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val applicationContext: Context,
    private val firestoreModelMapper: FirestoreModelMapper
) : CoroutineWorker(context, parameters) {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()
        val groupsCollection = firestore
            .collection(USERS_COLLECTION_NAME)
            .document(uid)
            .collection(GROUPS_COLLECTION_NAME)
        val articlesCollection = firestore
            .collection(USERS_COLLECTION_NAME)
            .document(uid)
            .collection(ARTICLES_COLLECTION_NAME)
        return try {
            val syncTimestamp = userPreferences.lastSyncTimestamp
            Log.d(ArticlesApplication.LOG_TAG, "Sync started, lastSyncTimestamp=$syncTimestamp")
            syncGroups(groupsCollection, syncTimestamp)
            syncArticles(articlesCollection, syncTimestamp)
            syncSharedGroups(uid, syncTimestamp)
            userPreferences.lastSyncTimestamp = System.currentTimeMillis()
            Log.d(ArticlesApplication.LOG_TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(ArticlesApplication.LOG_TAG, "Sync failed: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun syncArticles(
        articlesCollection: CollectionReference,
        syncTimestamp: Long,
    ) {
        val localArticleChanges = articleRepository
            .getAllArticlesNoMapping()
            .map { list -> list.filter { it.updatedAt > syncTimestamp } }
            .first()
        Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localArticleChanges.size} local article changes")
        uploadLocalArticleChanges(localArticleChanges, articlesCollection)
        val remoteSnapshot = articlesCollection
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()
        val remoteEntities = remoteSnapshot.documents.mapNotNull {
            firestoreModelMapper.toArticleEntity(it, applicationContext)
        }
        Log.d(ArticlesApplication.LOG_TAG, "Fetched ${remoteEntities.size} articles from remote")
        if (remoteEntities.isNotEmpty()) {
            articleRepository.upsertAllArticles(
                remoteEntities.map { it.toUIArticle() }
            )
        }
    }

    private suspend fun syncGroups(
        groupsCollection: CollectionReference,
        syncTimestamp: Long
    ) {
        val localGroupChanges = groupRepository
            .getAllGroupsNoMapping()
            .map { list -> list.filter { it.updatedAt > syncTimestamp } }
            .first()
        Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localGroupChanges.size} local group changes")
        uploadLocalGroupChanges(localGroupChanges, groupsCollection)
        val remoteSnapshot = groupsCollection
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()
        val remoteEntities = remoteSnapshot.documents.mapNotNull {
            firestoreModelMapper.toGroupEntity(it)
        }
        Log.d(ArticlesApplication.LOG_TAG, "Fetched ${remoteEntities.size} groups from remote")
        if (remoteEntities.isNotEmpty()) {
            groupRepository.upsertAllGroups(
                remoteEntities.map { it.toUIGroup() }
            )
        }
    }

    private suspend fun uploadLocalArticleChanges(
        articles: List<ArticleEntity>,
        collection: CollectionReference
    ) {
        articles.forEach { article ->
            val documentId = article.idArticle.toString()
            if (article.deleted) {
                Log.d(ArticlesApplication.LOG_TAG, "Deleting remote article id=$documentId")
                collection.document(documentId).delete().await()
                articleRepository.deleteArticleById(article.idArticle)
            } else {
                Log.d(ArticlesApplication.LOG_TAG, "Uploading article id=$documentId name=${article.name}")
                collection.document(documentId)
                    .set(firestoreModelMapper.toArticlesFirestoreMap(article, applicationContext))
                    .await()
            }
        }
    }

    private suspend fun uploadLocalGroupChanges(
        groups: List<GroupEntity>,
        collection: CollectionReference
    ) {
        // Shared groups are synced via syncSharedGroups — skip them here
        groups.filter { !it.isShared }.forEach { group ->
            val documentId = group.idGroup.toString()
            if (group.deleted) {
                Log.d(ArticlesApplication.LOG_TAG, "Deleting remote group id=$documentId")
                collection.document(documentId).delete().await()
                groupRepository.deleteGroupById(group.idGroup)
            } else {
                Log.d(ArticlesApplication.LOG_TAG, "Uploading group id=$documentId name=${group.name}")
                collection.document(documentId)
                    .set(firestoreModelMapper.toGroupsFirestoreMap(group))
                    .await()
            }
        }
    }

    private suspend fun syncSharedGroups(uid: String, syncTimestamp: Long) {
        val sharedGroupIds = sharedGroupRepository.getSharedGroupIdsForUser(uid)
        Log.d(ArticlesApplication.LOG_TAG, "Syncing ${sharedGroupIds.size} shared groups")

        for (sharedGroupId in sharedGroupIds) {
            // Upload local article changes for this shared group
            val localChanges = articleRepository
                .getAllArticlesNoMapping()
                .map { list ->
                    list.filter {
                        it.groupId == UUID.fromString(sharedGroupId) && it.updatedAt > syncTimestamp
                    }
                }
                .first()
            if (localChanges.isNotEmpty()) {
                Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localChanges.size} article changes for shared group $sharedGroupId")
                sharedGroupRepository.uploadSharedGroupChanges(sharedGroupId, localChanges)
            }

            // Pull latest shared group state and upsert locally
            val (groupEntity, articles) = sharedGroupRepository.syncSharedGroup(sharedGroupId)
            groupRepository.upsertAllGroups(listOf(groupEntity.toUIGroup()))
            if (articles.isNotEmpty()) {
                articleRepository.upsertAllArticles(articles.map { it.toUIArticle() })
            }
        }
    }

    companion object {
        const val NAME = "SyncManager"
        const val USERS_COLLECTION_NAME = "users"
        const val ARTICLES_COLLECTION_NAME = "articles"
        const val GROUPS_COLLECTION_NAME = "groups"
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