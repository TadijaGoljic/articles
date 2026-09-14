package com.example.articles.repositories

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.CommentEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.COMMENTS_COLLECTION
import com.example.articles.utils.PROFILES_COLLECTION
import com.example.articles.utils.SHARED_GROUPS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedGroupRepository @Inject constructor(
    private val firestoreModelMapper: FirestoreModelMapper
) {

    private val firestore = Firebase.firestore

    suspend fun createSharedGroup(group: UIGroup, fromUid: String, toUid: String): String {
        val sharedGroupId = UUID.randomUUID().toString()

        firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .set(
                mapOf(
                    "sharedGroupId" to sharedGroupId,
                    "name" to group.name,
                    "updatedAt" to System.currentTimeMillis(),
                    "members" to listOf(fromUid, toUid),
                    "fromUid" to fromUid,
                    "fromGroupId" to group.id.toString()
                )
            )
            .await()

        return sharedGroupId
    }

    suspend fun syncSharedGroup(sharedGroupId: String): Pair<GroupEntity, List<ArticleEntity>> {
        val groupSnapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .get()
            .await()

        val data = groupSnapshot.data ?: error("Shared group $sharedGroupId not found")
        val groupEntity = GroupEntity(
            idGroup = UUID.fromString(sharedGroupId),
            name = data["name"] as? String ?: "",
            updatedAt = data["updatedAt"] as? Long ?: 0L,
            isShared = true,
            sharedGroupId = sharedGroupId,
            ownerUid = data["fromUid"] as? String
        )

        val articlesSnapshot = firestore
            .collection(ARTICLES_COLLECTION)
            .whereEqualTo("sharedGroupId", sharedGroupId)
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()

        val articles = articlesSnapshot.documents.mapNotNull { doc ->
            firestoreModelMapper.toArticleEntity(doc)?.copy(
                groupId = UUID.fromString(sharedGroupId)
            )
        }

        return Pair(groupEntity, articles)
    }

    suspend fun uploadSharedGroupChanges(
        sharedGroupId: String,
        articles: List<ArticleEntity>
    ) {
        val collection = firestore.collection(ARTICLES_COLLECTION)

        articles.forEach { article ->
            collection
                .document(article.idArticle.toString())
                .set(firestoreModelMapper.toArticlesFirestoreMap(article, sharedGroupId = sharedGroupId))
                .await()
        }
    }

    suspend fun syncSharedGroupComments(sharedGroupId: String): List<CommentEntity> {
        val commentsSnapshot = firestore
            .collection(COMMENTS_COLLECTION)
            .whereEqualTo("sharedGroupId", sharedGroupId)
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()

        return commentsSnapshot.documents.mapNotNull { doc ->
            firestoreModelMapper.toCommentEntity(doc)?.copy(
                groupId = UUID.fromString(sharedGroupId)
            )
        }
    }

    suspend fun uploadSharedGroupCommentChanges(
        sharedGroupId: String,
        comments: List<CommentEntity>
    ) {
        val collection = firestore.collection(COMMENTS_COLLECTION)

        comments.forEach { comment ->
            collection
                .document(comment.idComment.toString())
                .set(firestoreModelMapper.toCommentsFirestoreMap(comment, sharedGroupId = sharedGroupId))
                .await()
        }
    }

    suspend fun getSharedGroupMeta(sharedGroupId: String): Pair<String?, String?> {
        val snapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .get()
            .await()
        return snapshot.getString("fromUid") to snapshot.getString("fromGroupId")
    }

    suspend fun leaveSharedGroup(sharedGroupId: String, uid: String) {
        val docRef = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
        val snapshot = docRef
            .get()
            .await()
        val data = snapshot.data ?: return
        val members = (data["members"] as? List<*>)?.filterIsInstance<String>() ?: return
        val remaining = members.filter { it != uid }
        if (remaining.isEmpty()) {
            val articles = firestore
                .collection(ARTICLES_COLLECTION)
                .whereEqualTo("sharedGroupId", sharedGroupId)
                .get()
                .await()
            val comments = firestore
                .collection(COMMENTS_COLLECTION)
                .whereEqualTo("sharedGroupId", sharedGroupId)
                .get()
                .await()
            val batch = firestore.batch()
            articles.documents.forEach { batch.delete(it.reference) }
            comments.documents.forEach { batch.delete(it.reference) }
            batch.delete(docRef)
            batch
                .commit()
                .await()
        } else {
            docRef.update("members", remaining).await()
        }
    }

    suspend fun getSharedGroupIdsForUser(uid: String): List<String> {
        val snapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .whereArrayContains("members", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.getString("sharedGroupId") }
    }

    suspend fun getMembers(sharedGroupId: String): List<GroupMember> {
        val snapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .get()
            .await()
        val memberUids = (snapshot.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return memberUids.mapNotNull { uid ->
            val profile = firestore
                .collection(PROFILES_COLLECTION)
                .document(uid)
                .get()
                .await()
            val email = profile.getString("email") ?: return@mapNotNull null
            GroupMember(uid = uid, email = email)
        }
    }

    suspend fun removeMember(sharedGroupId: String, memberUid: String) {
        val docRef = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
        val snapshot = docRef
            .get()
            .await()
        val members = (snapshot.get("members") as? List<*>)?.filterIsInstance<String>() ?: return
        docRef
            .update("members", members - memberUid)
            .await()
    }

    suspend fun renameSharedGroup(sharedGroupId: String, newName: String) {
        firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .update("name", newName)
            .await()
    }

    suspend fun deleteSharedGroup(sharedGroupId: String) {
        val docRef = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
        val articles = firestore
            .collection(ARTICLES_COLLECTION)
            .whereEqualTo("sharedGroupId", sharedGroupId)
            .get()
            .await()
        val comments = firestore
            .collection(COMMENTS_COLLECTION)
            .whereEqualTo("sharedGroupId", sharedGroupId)
            .get()
            .await()
        val batch = firestore.batch()
        articles.documents.forEach { batch.delete(it.reference) }
        comments.documents.forEach { batch.delete(it.reference) }
        batch.delete(docRef)
        batch
            .commit()
            .await()
    }

}

data class GroupMember(val uid: String, val email: String)
