package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.modelMapers.mapToUIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.composables.screens.ArticlesList
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

    val state = MutableStateFlow(ArticlesListState())

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

}

data class ArticlesListState(
    val articles: List<UIArticle> = emptyList()
)