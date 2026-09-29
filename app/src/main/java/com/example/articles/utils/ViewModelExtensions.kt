package com.example.articles.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
