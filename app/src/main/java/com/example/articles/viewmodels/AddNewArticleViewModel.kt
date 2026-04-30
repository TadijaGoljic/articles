package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
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
    }

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun save() = viewModelScope.launch {
        val isBadArticle = state.value.article.let { article ->
            article.name.isNullOrBlank() && article.photoUri.isNullOrBlank()
        }
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.let { article ->
                    if (isBadArticle) article.copy(isError = true)
                    else article.copy(isError = false)
                },
            )
        }
        if (isBadArticle) {
            state.update { currentState ->
                currentState.copy(
                    message = R.string.provide_name_photo
                )
            }
        } else {
            state.update { currentState ->
                currentState.copy(
                    article = currentState.article.copy(
                        dateCreated = Date()
                    )
                )
            }
            articleRepository.insert(state.value.article)
            state.update { currentState ->
                currentState.copy(
                    ok = true
                )
            }
        }
    }

    fun revalidateLocations(location: Location) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(location = location)
            )
        }
    }

    fun updatePhoto(photoUri: String) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(photoUri = photoUri)
            )
        }
    }

    fun updateType(type: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(unit = type)
            )
        }
    }

    fun updateQuantity(quantity: Double?) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(quantity = quantity)
            )
        }
    }

    fun updateName(name: String?) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(name = name),
                message = null
            )
        }
    }

    fun toggleExpanded() {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(expanded = !currentState.article.expanded)
            )
        }
    }

    fun togglePhotoVisibility() {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(photoVisible = !currentState.article.photoVisible)
            )
        }
    }

    fun photoUri() = fileManager.photoUri()

}

data class AddNewArticleState(
    val article: UIArticle = UIArticle(),
    val ok: Boolean = false,
    @StringRes val message: Int? = null,
)