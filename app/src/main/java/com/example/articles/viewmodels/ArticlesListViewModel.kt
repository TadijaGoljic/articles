package com.example.articles.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.articles.models.ModelMapper
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.sync.SyncManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val modelMapper: ModelMapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var state = MutableStateFlow(ArticlesListState())
    val uiState = state

    private val articlesCollection = Firebase.firestore.collection("articles")

    init {
        viewModelScope.launch {
            articleRepository.getAllArticles().collectLatest { articles ->
                state.update { currentState ->
                    currentState.copy(
                        articles = articles.map { modelMapper.mapToUIArticle(it) }
                    )
                }
            }
            syncData()
        }
    }

    fun syncData() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncManager.NAME,
            ExistingWorkPolicy.KEEP,
            SyncManager.sync()
        )
    }

    fun updateCheckStatus(index: Int, newValue: Boolean) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(checked = newValue) else article
                }
            )
        }
    }

    fun togglePhoto(index: Int) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(photoVisible = !article.photoVisible) else article
                }
            )
        }
    }

    fun startShopping(index: Int?) {
        uiState.update { currentState ->
            currentState.copy(
                shoppingModeActive = true,
            )
        }
        if (index != null) {
            uiState.update { currentState ->
                currentState.copy(
                    articles = currentState.articles.mapIndexed { i, article ->
                        if (i == index) article.copy(checked = true) else article
                    }
                )
            }
        }
    }

    fun stopShopping(deleteArticles: Boolean) {
        viewModelScope.launch {
            if (deleteArticles) {
                uiState.value.articles.filter { it.checked }.forEach { article ->
                    articleRepository.delete(article.id)
                    deleteArticleFromFirestore(article.id.toString())
                }
            }
            uiState.update { currentState ->
                currentState.copy(
                    shoppingModeActive = false,
                    articles = currentState.articles.map { article ->
                        article.copy(checked = false)
                    }
                )
            }
        }
    }

    fun buy(article: UIArticle) {
        viewModelScope.launch {
            articleRepository.delete(article.id)
            deleteArticleFromFirestore(article.id.toString())
        }
    }

    private suspend fun deleteArticleFromFirestore(articleId: String) = try {
        articlesCollection.document(articleId).delete().await()
    } catch (e: Exception) {
        Log.e("my_result", "deleteArticleFromFirestore: ${e.message}")
    }

}

data class ArticlesListState(
    val articles: List<UIArticle> = emptyList(),
    val shoppingModeActive: Boolean = false,
)