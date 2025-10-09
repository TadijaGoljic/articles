package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.modelMapers.mapToUIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.models.UIArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private var state = MutableStateFlow(ArticlesListState())
    val uiState = state

    init {
        viewModelScope.launch {
            articleRepository.getAllArticles().collectLatest { articles ->
                state.update { currentState ->
                    currentState.copy(
                        articles = articles.map { it.mapToUIArticle() }
                    )
                }
            }
        }
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
                articleRepository.deleteAll(
                    uiState.value.articles.filter { it.checked }
                )
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
            articleRepository.deleteAll(
                listOf(article)
            )
        }
    }

}

data class ArticlesListState(
    val articles: List<UIArticle> = emptyList(),
    val shoppingModeActive: Boolean = false,
)