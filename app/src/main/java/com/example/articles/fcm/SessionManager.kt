package com.example.articles.fcm

import com.example.articles.db.ArticleDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val database: ArticleDatabase
) {
    suspend fun clearLocalDataAndSignOut() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        Firebase.auth.signOut()
    }
}