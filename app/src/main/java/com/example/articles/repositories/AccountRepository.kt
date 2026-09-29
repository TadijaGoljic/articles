package com.example.articles.repositories

import com.example.articles.utils.ARTICLES_COLLECTION
import com.example.articles.utils.ARTICLE_ORDER_SUBCOLLECTION
import com.example.articles.utils.COMMENTS_COLLECTION
import com.example.articles.utils.GROUPS_COLLECTION
import com.example.articles.utils.GROUP_ORDER_SUBCOLLECTION
import com.example.articles.utils.PROFILES_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val sharedGroupRepository: SharedGroupRepository
) {

    private val firestore = Firebase.firestore

    suspend fun deleteUserData(uid: String) {
        sharedGroupRepository.getSharedGroupIdsForUser(uid).forEach { sharedGroupId ->
            val (ownerUid, _) = sharedGroupRepository.getSharedGroupMeta(sharedGroupId)
            if (ownerUid == uid) {
                sharedGroupRepository.deleteSharedGroup(sharedGroupId)
            } else {
                sharedGroupRepository.leaveSharedGroup(sharedGroupId, uid)
            }
        }

        listOf(ARTICLES_COLLECTION, COMMENTS_COLLECTION, GROUPS_COLLECTION).forEach { collection ->
            deleteAll(firestore.collection(collection).whereEqualTo("ownerUid", uid))
        }

        val userDoc = firestore.collection(USERS_COLLECTION).document(uid)
        deleteAll(userDoc.collection(GROUP_ORDER_SUBCOLLECTION))
        deleteAll(userDoc.collection(ARTICLE_ORDER_SUBCOLLECTION))
        deleteDocuments(
            listOf(
                firestore.collection(PROFILES_COLLECTION).document(uid),
                userDoc
            )
        )
    }

    private suspend fun deleteAll(query: Query) {
        deleteDocuments(query.get().await().documents.map { it.reference })
    }

    private suspend fun deleteDocuments(references: List<DocumentReference>) {
        references.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private companion object {
        const val BATCH_LIMIT = 450
    }

}
