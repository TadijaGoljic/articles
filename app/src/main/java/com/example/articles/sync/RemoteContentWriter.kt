package com.example.articles.sync

import android.util.Log
import com.example.articles.ArticlesApplication
import com.example.articles.models.UserPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteContentWriter @Inject constructor(
    private val userPreferences: UserPreferences
) {

    private val firestore by lazy { Firebase.firestore }

    private val enabled: Boolean
        get() = userPreferences.syncDataEnabled

    // Writes are not awaited: the Firestore client applies them to its local cache and
    // queues them until the server is reachable, so the UI never waits for the network.
    fun set(document: DocumentReference, data: Any) {
        if (!enabled) return
        document.set(data).logFailure { "set ${document.path}" }
    }

    fun delete(document: DocumentReference) {
        if (!enabled) return
        document.delete().logFailure { "delete ${document.path}" }
    }

    fun deleteAll(documents: List<DocumentReference>) {
        if (!enabled || documents.isEmpty()) return
        val batch = firestore.batch()
        documents.forEach { batch.delete(it) }
        batch.commit().logFailure { "delete ${documents.size} documents" }
    }

    private fun Task<Void>.logFailure(operation: () -> String) {
        addOnFailureListener({ it.run() }) { e ->
            Log.e(ArticlesApplication.LOG_TAG, "Remote ${operation()} failed: ${e.message}", e)
        }
    }

}
