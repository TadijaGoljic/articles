package com.example.articles.sync

import com.example.articles.models.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteContentWriter @Inject constructor(
    private val userPreferences: UserPreferences
) {

    private val firestore = Firebase.firestore

    private val enabled: Boolean
        get() = userPreferences.syncDataEnabled

    suspend fun set(document: DocumentReference, data: Any) {
        if (!enabled) return
        document.set(data).await()
    }

    suspend fun delete(document: DocumentReference) {
        if (!enabled) return
        document.delete().await()
    }

    suspend fun deleteAll(documents: List<DocumentReference>) {
        if (!enabled || documents.isEmpty()) return
        val batch = firestore.batch()
        documents.forEach { batch.delete(it) }
        batch.commit().await()
    }

}
