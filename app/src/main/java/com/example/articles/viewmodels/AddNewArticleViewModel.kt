package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.models.UIArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun save() {

    }

    fun remove(index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.toMutableList().apply {
                    removeAt(index)
                }
            )
        }
    }

    fun updateName(index: Int, name: String) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) {
                        article.copy(name = name)
                    } else {
                        article
                    }
                }
            )
        }
    }

    fun addNewArticle() {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.toMutableList().apply {
                    add(UIArticle())
                }
            )
        }
    }

}

data class AddNewArticleState(
    val articles: List<UIArticle> = listOf(UIArticle())
)