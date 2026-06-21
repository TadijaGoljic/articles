package com.example.articles.repositories

import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.utils.ARTICLES_COLLECTION
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

        firestore.collection(SHARED_GROUPS_COLLECTION).document(sharedGroupId)
            .set(mapOf(
                "sharedGroupId" to sharedGroupId,
                "name" to group.name,
                "updatedAt" to System.currentTimeMillis(),
                "deleted" to false,
                "members" to listOf(fromUid, toUid)
            ))
            .await()

        // Copy articles from user's Firestore collection into the shared group
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
            sharedArticlesCollection.document(articleEntity.idArticle.toString())
                .set(firestoreModelMapper.toArticlesFirestoreMap(articleEntity))
                .await()
        }

        return sharedGroupId
    }

    suspend fun syncSharedGroup(sharedGroupId: String): Pair<GroupEntity, List<ArticleEntity>> {
        val groupSnapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION).document(sharedGroupId)
            .get()
            .await()

        val data = groupSnapshot.data ?: error("Shared group $sharedGroupId not found")
        val groupEntity = GroupEntity(
            idGroup = UUID.fromString(sharedGroupId),
            name = data["name"] as? String ?: "",
            updatedAt = data["updatedAt"] as? Long ?: 0L,
            deleted = data["deleted"] as? Boolean ?: false,
            isShared = true,
            sharedGroupId = sharedGroupId
        )

        val articlesSnapshot = firestore
            .collection(SHARED_GROUPS_COLLECTION).document(sharedGroupId)
            .collection(ARTICLES_COLLECTION)
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
        val collection = firestore
            .collection(SHARED_GROUPS_COLLECTION).document(sharedGroupId)
            .collection(ARTICLES_COLLECTION)

        articles.forEach { article ->
            val documentId = article.idArticle.toString()
            if (article.deleted) {
                collection.document(documentId).delete().await()
            } else {
                collection.document(documentId)
                    .set(firestoreModelMapper.toArticlesFirestoreMap(article))
                    .await()
            }
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

}
