package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.ui.utils.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val groupRepository: GroupRepository,
    private val fileManager: FileManager
) : ViewModel() {

    companion object {
        const val KEY_INDEX = "index"
        const val KEY_LOCATION = "location"
    }

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    init {
        viewModelScope.launch {
            groupRepository.getAllGroups().collectLatest { groups ->
                state.update { currentState ->
                    currentState.copy(
                        groupNames = groups.map { it.name }
                    )
                }
            }
        }
    }

    fun handleAction(action: AddNewArticleAction) {
        when (action) {
            is AddNewArticleAction.Save -> save()
            is AddNewArticleAction.UpdateName -> updateName(action.name)
            is AddNewArticleAction.UpdatePhoto -> updatePhoto(action.uri)
            is AddNewArticleAction.UpdateType -> updateType(action.type)
            is AddNewArticleAction.UpdateQuantity -> updateQuantity(action.quantity)
            is AddNewArticleAction.RevalidateLocation -> revalidateLocation(action.location)
            is AddNewArticleAction.ToggleExpanded -> toggleExpanded()
            is AddNewArticleAction.TogglePhotoVisibility -> togglePhotoVisibility()
        }
    }

    fun photoUri() = fileManager.photoUri()

    private fun save() = viewModelScope.launch {
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
                currentState.copy(message = R.string.provide_name_photo)
            }
        } else {
            state.update { currentState ->
                currentState.copy(
                    article = currentState.article.copy(dateCreated = Date())
                )
            }
            articleRepository.insert(state.value.article)
            state.update { currentState ->
                currentState.copy(ok = true)
            }
        }
    }

    private fun revalidateLocation(location: Location) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(location = location)
            )
        }
    }

    private fun updatePhoto(photoUri: String) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(photoUri = photoUri)
            )
        }
    }

    private fun updateType(type: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(unit = type)
            )
        }
    }

    private fun updateQuantity(quantity: Double?) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(quantity = quantity)
            )
        }
    }

    private fun updateName(name: String?) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(name = name),
                message = null
            )
        }
    }

    private fun toggleExpanded() {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(expanded = !currentState.article.expanded)
            )
        }
    }

    private fun togglePhotoVisibility() {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(photoVisible = !currentState.article.photoVisible)
            )
        }
    }

}

data class AddNewArticleState(
    val article: UIArticle = UIArticle(),
    val ok: Boolean = false,
    @StringRes val message: Int? = null,
    val groupNames: List<String> = emptyList(),
)

sealed interface AddNewArticleAction {
    data object Save : AddNewArticleAction
    data class UpdateName(val name: String?) : AddNewArticleAction
    data class UpdatePhoto(val uri: String) : AddNewArticleAction
    data class UpdateType(val type: ArticleUnit) : AddNewArticleAction
    data class UpdateQuantity(val quantity: Double?) : AddNewArticleAction
    data class RevalidateLocation(val location: Location) : AddNewArticleAction
    data object ToggleExpanded : AddNewArticleAction
    data object TogglePhotoVisibility : AddNewArticleAction
}
