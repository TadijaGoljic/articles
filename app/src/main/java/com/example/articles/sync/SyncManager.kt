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
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UserPreferences
import com.example.articles.models.mappers.toUIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.CommentRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.SharedGroupRepository
import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.ARTICLE_ORDER_SUBCOLLECTION
import com.example.articles.utils.COMMENTS_COLLECTION
import com.example.articles.utils.GROUPS_COLLECTION
import com.example.articles.utils.GROUP_ORDER_SUBCOLLECTION
import com.example.articles.utils.USERS_COLLECTION
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
    private val commentRepository: CommentRepository,
    private val sharedGroupRepository: SharedGroupRepository,
    private val firestoreModelMapper: FirestoreModelMapper
) : CoroutineWorker(context, parameters) {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()
        val groupsCollection = firestore.collection(GROUPS_COLLECTION)
        val articlesCollection = firestore.collection(ARTICLES_COLLECTION)
        val commentsCollection = firestore.collection(COMMENTS_COLLECTION)
        return try {
            val syncTimestamp = userPreferences.lastSyncTimestamp
            Log.d(ArticlesApplication.LOG_TAG, "Sync started, lastSyncTimestamp=$syncTimestamp")
            syncSharedGroups(uid, syncTimestamp)
            syncGroups(groupsCollection, uid, syncTimestamp)
            syncArticles(articlesCollection, uid, syncTimestamp)
            syncComments(commentsCollection, uid, syncTimestamp)
            syncOrdering(uid)
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
        uid: String,
        syncTimestamp: Long,
    ) {
        val privateGroupIds = groupRepository.getAllGroupsNoMapping()
            .map { list -> list.filter { !it.isShared }.map { it.idGroup } }
            .first()
        val localArticles = articleRepository
            .getAllArticlesNoMapping()
            .map { list -> list.filter { it.groupId in privateGroupIds } }
            .first()
        val localArticleChanges = localArticles.filter { it.updatedAt > syncTimestamp }
        Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localArticleChanges.size} local article changes")
        uploadLocalArticleChanges(localArticleChanges, articlesCollection, uid)
        val remoteEntities = privateGroupIds
            .map { it.toString() }
            .chunked(WHERE_IN_CHUNK_SIZE)
            .flatMap { chunk ->
                articlesCollection
                    .whereEqualTo("ownerUid", uid)
                    .whereIn("groupId", chunk)
                    .whereGreaterThan("updatedAt", 0)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { firestoreModelMapper.toArticleEntity(it) }
            }
        Log.d(ArticlesApplication.LOG_TAG, "Fetched ${remoteEntities.size} articles from remote")
        if (remoteEntities.isNotEmpty()) {
            val positionById = localArticles.associate { it.idArticle to it.position }
            articleRepository.upsertAllArticlesLocal(
                remoteEntities.map { it.copy(position = positionById[it.idArticle]) }
            )
        }
        val remoteIds = remoteEntities.map { it.idArticle }.toSet()
        val staleLocalIds = localArticles.map { it.idArticle }.filterNot { it in remoteIds }
        if (staleLocalIds.isNotEmpty()) {
            Log.d(ArticlesApplication.LOG_TAG, "Removing ${staleLocalIds.size} articles deleted remotely")
            articleRepository.hardDeleteLocalArticlesByIds(staleLocalIds)
        }
    }

    private suspend fun syncComments(
        commentsCollection: CollectionReference,
        uid: String,
        syncTimestamp: Long,
    ) {
        val sharedGroupIds = groupRepository.getAllGroupsNoMapping()
            .map { list -> list.filter { it.isShared }.map { it.idGroup }.toSet() }
            .first()
        val localComments = commentRepository
            .getAllCommentsNoMapping()
            .map { list -> list.filter { it.groupId !in sharedGroupIds } }
            .first()
        val localCommentChanges = localComments.filter { it.updatedAt > syncTimestamp }
        Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localCommentChanges.size} local comment changes")
        uploadLocalCommentChanges(localCommentChanges, commentsCollection, uid)
        val remoteSnapshot = commentsCollection
            .whereEqualTo("ownerUid", uid)
            .whereEqualTo("sharedGroupId", null)
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()
        val remoteEntities = remoteSnapshot.documents.mapNotNull {
            firestoreModelMapper.toCommentEntity(it)
        }
        Log.d(ArticlesApplication.LOG_TAG, "Fetched ${remoteEntities.size} comments from remote")
        if (remoteEntities.isNotEmpty()) {
            commentRepository.upsertAllCommentsLocal(remoteEntities)
        }
        val remoteIds = remoteEntities.map { it.idComment }.toSet()
        val staleLocalIds = localComments.map { it.idComment }.filterNot { it in remoteIds }
        if (staleLocalIds.isNotEmpty()) {
            Log.d(ArticlesApplication.LOG_TAG, "Removing ${staleLocalIds.size} comments deleted remotely")
            commentRepository.hardDeleteLocalCommentsByIds(staleLocalIds)
        }
    }

    private suspend fun syncGroups(
        groupsCollection: CollectionReference,
        uid: String,
        syncTimestamp: Long
    ) {
        val allLocalGroups = groupRepository.getAllGroupsNoMapping().first()
        val locallySharedIds = allLocalGroups.filter { it.isShared }.map { it.idGroup }.toSet()
        val localGroups = allLocalGroups.filter { !it.isShared }
        val localGroupChanges = localGroups.filter { it.updatedAt > syncTimestamp }
        Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localGroupChanges.size} local group changes")
        uploadLocalGroupChanges(localGroupChanges, groupsCollection, uid)
        val remoteSnapshot = groupsCollection
            .whereEqualTo("ownerUid", uid)
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()
        val remoteEntities = remoteSnapshot.documents
            .mapNotNull { firestoreModelMapper.toGroupEntity(it) }
            .filter { it.idGroup !in locallySharedIds }
        Log.d(ArticlesApplication.LOG_TAG, "Fetched ${remoteEntities.size} groups from remote")
        if (remoteEntities.isNotEmpty()) {
            val positionById = allLocalGroups.associate { it.idGroup to it.position }
            groupRepository.upsertAllGroupsLocal(
                remoteEntities.map { it.toUIGroup().copy(position = positionById[it.idGroup]) }
            )
        }
        val remoteIds = remoteEntities.map { it.idGroup }.toSet()
        val staleLocalIds = localGroups.map { it.idGroup }.filterNot { it in remoteIds }
        if (staleLocalIds.isNotEmpty()) {
            Log.d(ArticlesApplication.LOG_TAG, "Removing ${staleLocalIds.size} groups deleted remotely")
            groupRepository.hardDeleteLocalGroupsByIds(staleLocalIds)
        }
    }

    private suspend fun uploadLocalArticleChanges(
        articles: List<ArticleEntity>,
        collection: CollectionReference,
        uid: String
    ) {
        articles.forEach { article ->
            val documentId = article.idArticle.toString()
            Log.d(ArticlesApplication.LOG_TAG, "Uploading article id=$documentId name=${article.name}")
            collection.document(documentId)
                .set(firestoreModelMapper.toArticlesFirestoreMap(article, ownerUid = uid))
                .await()
        }
    }

    private suspend fun uploadLocalGroupChanges(
        groups: List<GroupEntity>,
        collection: CollectionReference,
        uid: String
    ) {
        groups.filter { !it.isShared }.forEach { group ->
            val documentId = group.idGroup.toString()
            Log.d(ArticlesApplication.LOG_TAG, "Uploading group id=$documentId name=${group.name}")
            collection.document(documentId)
                .set(firestoreModelMapper.toGroupsFirestoreMap(group.copy(ownerUid = uid)))
                .await()
        }
    }

    private suspend fun uploadLocalCommentChanges(
        comments: List<CommentEntity>,
        collection: CollectionReference,
        uid: String
    ) {
        comments.forEach { comment ->
            val documentId = comment.idComment.toString()
            Log.d(ArticlesApplication.LOG_TAG, "Uploading comment id=$documentId")
            collection.document(documentId)
                .set(firestoreModelMapper.toCommentsFirestoreMap(comment, ownerUid = uid))
                .await()
        }
    }

    private suspend fun syncSharedGroups(uid: String, syncTimestamp: Long) {
        val sharedGroupIds = sharedGroupRepository.getSharedGroupIdsForUser(uid)
        Log.d(ArticlesApplication.LOG_TAG, "Syncing ${sharedGroupIds.size} shared groups")

        removeOrphanedSharedGroups(uid, sharedGroupIds.toSet())

        for (sharedGroupId in sharedGroupIds) {
            val (fromUid, fromGroupId) = sharedGroupRepository.getSharedGroupMeta(sharedGroupId)
            val ownerLocalGroup = if (uid == fromUid && fromGroupId != null) {
                groupRepository.getGroupByIdSuspend(UUID.fromString(fromGroupId))
            } else null
            val sharedIdLocalGroup = groupRepository.getGroupBySharedGroupId(sharedGroupId)

            val existingLocalGroup = ownerLocalGroup ?: sharedIdLocalGroup
            val localGroupId = existingLocalGroup?.idGroup ?: UUID.fromString(sharedGroupId)
            val isFirstSyncAsShared = existingLocalGroup?.isShared != true

            if (sharedIdLocalGroup != null && sharedIdLocalGroup.idGroup != localGroupId) {
                Log.d(ArticlesApplication.LOG_TAG, "Merging duplicate shared group ${sharedIdLocalGroup.idGroup} into $localGroupId")
                articleRepository.reassignArticlesGroupIdLocal(sharedIdLocalGroup.idGroup, localGroupId)
                commentRepository.reassignCommentsGroupIdLocal(sharedIdLocalGroup.idGroup, localGroupId)
                groupRepository.deleteGroupLocalById(sharedIdLocalGroup.idGroup)
            }

            val localChanges = articleRepository
                .getAllArticlesNoMapping()
                .map { list ->
                    list.filter {
                        it.groupId == localGroupId && (isFirstSyncAsShared || it.updatedAt > syncTimestamp)
                    }
                }
                .first()
            if (localChanges.isNotEmpty()) {
                Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localChanges.size} article changes for shared group $sharedGroupId")
                sharedGroupRepository.uploadSharedGroupChanges(sharedGroupId, localChanges)
            }

            val (groupEntity, articles) = sharedGroupRepository.syncSharedGroup(sharedGroupId)
            groupRepository.upsertGroupLocal(
                groupEntity.copy(
                    idGroup = localGroupId,
                    sharedGroupId = sharedGroupId,
                    position = existingLocalGroup?.position
                ).toUIGroup()
            )
            if (articles.isNotEmpty()) {
                val existingArticlePositions = articleRepository
                    .getArticlesByGroupIdSuspend(localGroupId)
                    .associate { it.idArticle to it.position }
                articleRepository.upsertAllArticlesLocal(
                    articles.map {
                        it.copy(groupId = localGroupId, position = existingArticlePositions[it.idArticle])
                    }
                )
            }

            val remoteArticleIds = articles.map { it.idArticle }.toSet()
            val localArticleIds = articleRepository.getArticlesByGroupIdSuspend(localGroupId).map { it.idArticle }
            val staleLocalIds = localArticleIds.filterNot { it in remoteArticleIds }
            if (staleLocalIds.isNotEmpty()) {
                Log.d(ArticlesApplication.LOG_TAG, "Removing ${staleLocalIds.size} shared articles deleted remotely for group $sharedGroupId")
                articleRepository.hardDeleteLocalArticlesByIds(staleLocalIds)
            }

            val localCommentChanges = commentRepository
                .getCommentsByGroupIdSuspend(localGroupId)
                .filter { isFirstSyncAsShared || it.updatedAt > syncTimestamp }
            if (localCommentChanges.isNotEmpty()) {
                Log.d(ArticlesApplication.LOG_TAG, "Uploading ${localCommentChanges.size} comment changes for shared group $sharedGroupId")
                sharedGroupRepository.uploadSharedGroupCommentChanges(sharedGroupId, localCommentChanges)
            }

            val comments = sharedGroupRepository.syncSharedGroupComments(sharedGroupId)
            if (comments.isNotEmpty()) {
                commentRepository.upsertAllCommentsLocal(
                    comments.map { it.copy(groupId = localGroupId) }
                )
            }
            val remoteCommentIds = comments.map { it.idComment }.toSet()
            val localCommentIds = commentRepository.getCommentsByGroupIdSuspend(localGroupId).map { it.idComment }
            val staleCommentIds = localCommentIds.filterNot { it in remoteCommentIds }
            if (staleCommentIds.isNotEmpty()) {
                Log.d(ArticlesApplication.LOG_TAG, "Removing ${staleCommentIds.size} shared comments deleted remotely for group $sharedGroupId")
                commentRepository.hardDeleteLocalCommentsByIds(staleCommentIds)
            }
        }
    }

    private suspend fun syncOrdering(uid: String) {
        val userDoc = firestore.collection(USERS_COLLECTION).document(uid)
        val groupOrderSnapshot = userDoc.collection(GROUP_ORDER_SUBCOLLECTION)
            .get()
            .await()
        groupOrderSnapshot.documents.forEach { doc ->
            val position = doc.getDouble("position") ?: return@forEach
            val groupId = runCatching { UUID.fromString(doc.id) }.getOrNull() ?: return@forEach
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            groupRepository.applyPulledGroupPosition(groupId, position, updatedAt)
        }

        val articleOrderSnapshot = userDoc.collection(ARTICLE_ORDER_SUBCOLLECTION)
            .get()
            .await()
        articleOrderSnapshot.documents.forEach { doc ->
            val position = doc.getDouble("position") ?: return@forEach
            val articleId = runCatching { UUID.fromString(doc.id) }.getOrNull() ?: return@forEach
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            articleRepository.applyPulledArticlePosition(articleId, position, updatedAt)
        }

        val unresolvedGroups = groupRepository.getAllGroupsNoMapping().first()
            .filter { it.position == null && !it.isArchived }
        unresolvedGroups.forEach { group ->
            groupRepository.updateGroupPosition(group.idGroup, groupRepository.getNextGroupPosition())
        }

        val unresolvedArticlesByGroup = articleRepository.getAllArticlesNoMapping().first()
            .filter { it.position == null }
            .groupBy { it.groupId }
        unresolvedArticlesByGroup.forEach { (groupId, articles) ->
            articles.forEach { article ->
                val position = articleRepository.getNextArticlePosition(groupId)
                articleRepository.updateArticlePosition(article.idArticle, groupId, position)
            }
        }
    }

    private suspend fun removeOrphanedSharedGroups(uid: String, remoteSharedGroupIds: Set<String>) {
        val localSharedGroups = groupRepository.getAllGroupsNoMapping()
            .map { list -> list.filter { it.isShared && it.sharedGroupId != null } }
            .first()
        val orphaned = localSharedGroups.filter { it.sharedGroupId !in remoteSharedGroupIds }
        orphaned.forEach { group ->
            if (group.ownerUid == uid) {
                Log.d(ArticlesApplication.LOG_TAG, "Reverting shared group id=${group.idGroup} to personal (still owner)")
                groupRepository.upsertGroupLocal(group.copy(isShared = false, sharedGroupId = null).toUIGroup())
            } else {
                Log.d(ArticlesApplication.LOG_TAG, "Removing local access to shared group id=${group.idGroup} (no longer a member)")
                articleRepository.hardDeleteLocalArticlesByGroupId(group.idGroup)
                commentRepository.hardDeleteLocalCommentsByGroupId(group.idGroup)
                groupRepository.deleteGroupLocalById(group.idGroup)
            }
        }
    }

    companion object {
        const val NAME = "SyncManager"
        private const val WHERE_IN_CHUNK_SIZE = 30
        fun sync() = OneTimeWorkRequestBuilder<SyncManager>()
            .setConstraints(Constraints.Builder()
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