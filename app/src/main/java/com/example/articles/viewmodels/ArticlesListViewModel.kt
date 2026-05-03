package com.example.articles.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.articles.models.UIArticle
import com.example.articles.models.toUIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var state = MutableStateFlow(ArticlesListState())
    val uiState = state

    init {
        viewModelScope.launch {
            articleRepository.getAllArticles().collectLatest { articles ->
                state.update { currentState ->
                    currentState.copy(
                        articles = articles
                            .map { it.toUIArticle() }
                            .filter { !it.deleted }
                    )
                }
            }
            syncData()
        }
    }

    fun handleAction(action: ArticlesListAction) {
        when (action) {
            is ArticlesListAction.SyncData ->
                syncData()
            is ArticlesListAction.ToggleCheckStatus ->
                toggleCheckStatus(action.index, action.newValue)
            is ArticlesListAction.TogglePhoto ->
                togglePhoto(action.index)
            is ArticlesListAction.StartShopping ->
                startShopping(action.index)
            is ArticlesListAction.StopShopping ->
                stopShopping(action.deleteArticles)
            is ArticlesListAction.BuyArticle ->
                buy(action.article)
        }
    }

    private fun syncData() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncManager.NAME,
            ExistingWorkPolicy.REPLACE,
            SyncManager.sync()
        )
    }

    private fun toggleCheckStatus(index: Int, newValue: Boolean) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(checked = newValue) else article
                }
            )
        }
    }

    private fun togglePhoto(index: Int) {
        uiState.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(photoVisible = !article.photoVisible) else article
                }
            )
        }
    }

    private fun startShopping(index: Int?) {
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

    private fun stopShopping(deleteArticles: Boolean) {
        viewModelScope.launch {
            if (deleteArticles) {
                uiState.value.articles.filter { it.checked }.forEach { article ->
                    articleRepository.markDeleted(article.id, System.currentTimeMillis())
                }
                syncData()
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

    private fun buy(article: UIArticle) {
        viewModelScope.launch {
            articleRepository.markDeleted(article.id, System.currentTimeMillis())
            syncData()
        }
    }

}

data class ArticlesListState(
    val articles: List<UIArticle> = emptyList(),
    val shoppingModeActive: Boolean = false,
)

sealed interface ArticlesListAction {
    data object SyncData : ArticlesListAction
    data class ToggleCheckStatus(val index: Int, val newValue: Boolean) : ArticlesListAction
    data class TogglePhoto(val index: Int) : ArticlesListAction
    data class StartShopping(val index: Int?) : ArticlesListAction
    data class StopShopping(val deleteArticles: Boolean) : ArticlesListAction
    data class BuyArticle(val article: UIArticle) : ArticlesListAction
}