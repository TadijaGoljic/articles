package com.example.articles.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Launches [block] in [viewModelScope], logging failures instead of letting them crash the app.
 *
 * Repositories write to Room first and Firestore second, so a rejected remote write (e.g.
 * PERMISSION_DENIED) must not take the whole process down with it — the next sync reconciles
 * local state with the server.
 */
fun ViewModel.launchCatching(
    operation: String,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(ArticlesApplication.LOG_TAG, "$operation failed: ${e.message}", e)
        onError(e)
    }
}
