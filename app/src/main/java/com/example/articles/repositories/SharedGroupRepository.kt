package com.example.articles.repositories

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.SHARED_GROUPS_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
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

        val articlesSnapshot = firestore
            .collection("users").document(fromUid)
            .collection("articles")
            .whereEqualTo("groupId", group.id.toString())
            .get()
            .await()

        val sharedArticlesCollection = firestore
            .collection(SHARED_GROUPS_COLLECTION).document(sharedGroupId)
            .collection(ARTICLES_COLLECTION)

        articlesSnapshot.documents.forEach { doc ->
            val articleEntity = firestoreModelMapper.toArticleEntity(doc) ?: return@forEach
            sharedArticlesCollection
                .document(articleEntity.idArticle.toString())
                .set(firestoreModelMapper.toArticlesFirestoreMap(articleEntity))
                .await()
        }

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
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .collection(ARTICLES_COLLECTION)
            .whereGreaterThan("updatedAt", 0)
            .get()
            .await()

        val (tombstoned, live) = articlesSnapshot.documents.partition { it.getBoolean("deleted") == true }
        tombstoned.forEach { it.reference.delete() }

        val articles = live.mapNotNull { doc ->
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
        val collection = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
            .collection(ARTICLES_COLLECTION)

        articles.forEach { article ->
            collection
                .document(article.idArticle.toString())
                .set(firestoreModelMapper.toArticlesFirestoreMap(article))
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
            val articles = docRef
                .collection(ARTICLES_COLLECTION)
                .get()
                .await()
            val batch = firestore.batch()
            articles.documents.forEach { batch.delete(it.reference) }
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
        val (tombstoned, live) = snapshot.documents.partition { it.getBoolean("deleted") == true }
        tombstoned.forEach { doc ->
            val sharedGroupId = doc.getString("sharedGroupId") ?: doc.id
            deleteSharedGroup(sharedGroupId)
        }
        return live.mapNotNull { it.getString("sharedGroupId") }
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
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection("profile")
                .document("info")
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

    suspend fun deleteSharedGroup(sharedGroupId: String) {
        val docRef = firestore
            .collection(SHARED_GROUPS_COLLECTION)
            .document(sharedGroupId)
        val articles = docRef
            .collection(ARTICLES_COLLECTION)
            .get()
            .await()
        val batch = firestore.batch()
        articles.documents.forEach { batch.delete(it.reference) }
        batch.delete(docRef)
        batch.commit().await()
    }

}

data class GroupMember(val uid: String, val email: String)
