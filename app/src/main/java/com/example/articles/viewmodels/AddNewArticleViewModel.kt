package com.example.articles.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.models.ArticleUnit
import com.example.articles.ui.models.UIArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    companion object {
        const val KEY_INDEX = "index"
        const val KEY_LOCATION = "location"

    }

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun save() {

    }

    fun revalidateLocations(location: LocationData, index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(location = location) else article
                }
            )
        }
    }

    fun updateType(index: Int, type: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) {
                        article.copy(unit = type)
                    } else {
                        article
                    }
                }
            )
        }
    }

    fun updateQuantity(index: Int, quantity: Double?) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) {
                        article.copy(quantity = quantity)
                    } else {
                        article
                    }
                }
            )
        }
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

    fun toggleState(index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) {
                        article.copy(expanded = !article.expanded)
                    } else {
                        article
                    }
                }
            )
        }
    }

}

data class AddNewArticleState(
    val articles: List<UIArticle> = listOf(UIArticle())
)