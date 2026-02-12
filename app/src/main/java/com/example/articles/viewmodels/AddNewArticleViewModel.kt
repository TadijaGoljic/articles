package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.models.ArticleUnit
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.utils.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val fileManager: FileManager
) : ViewModel() {

    companion object {
        const val KEY_INDEX = "index"
        const val KEY_LOCATION = "location"
        const val IMAGE_PREFIX = "article"
        const val IMAGE_SUFFIX = ".jpg"
    }

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun save() = viewModelScope.launch {
        val badArticles = state.value.articles.map {
            it.name.isNullOrBlank() && it.photoUri.isNullOrBlank()
        }
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (badArticles[i]) article.copy(isError = true)
                    else article.copy(isError = false)
                },
            )
        }
        if (badArticles.any { it }) {
            state.update { currentState ->
                currentState.copy(
                    message = R.string.provide_name_photo
                )
            }
        } else {
            state.update { currentState ->
                currentState.copy(
                    articles = currentState.articles.map { article ->
                        article.copy(
                            dateCreated = Date()
                        )
                    }
                )
            }
            articleRepository.insertAll(state.value.articles)
            state.update { currentState ->
                currentState.copy(
                    ok = true
                )
            }
        }
    }

    fun revalidateLocations(location: Location, index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(location = location) else article
                }
            )
        }
    }

    fun updatePhoto(photoUri: String, index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(photoUri = photoUri) else article
                },
                message = null
            )
        }
    }

    fun updateType(index: Int, type: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(unit = type) else article
                }
            )
        }
    }

    fun updateQuantity(index: Int, quantity: Double?) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(quantity = quantity) else article
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

    fun updateName(index: Int, name: String?) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(name = name) else article
                },
                message = null
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
                    if (i == index) article.copy(expanded = !article.expanded) else article
                }
            )
        }
    }

    fun updatePhotoVisible(index: Int) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.mapIndexed { i, article ->
                    if (i == index) article.copy(photoVisible = !article.photoVisible) else article
                }
            )
        }
    }

    fun photoUri() = fileManager.photoUri()

}

data class AddNewArticleState(
    val articles: List<UIArticle> = listOf(UIArticle()),
    val ok: Boolean = false,
    @StringRes val message: Int? = null,
)