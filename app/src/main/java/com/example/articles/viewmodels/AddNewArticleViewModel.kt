package com.example.articles.viewmodels

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import android.net.Uri
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.utils.FileManager
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.triggerSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val fileManager: FileManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.AddNewArticleDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun photoUri(): Uri = fileManager.photoUri()

    fun handleAction(action: AddNewArticleAction) {
        when (action) {
            is AddNewArticleAction.SaveArticle -> saveArticle()
            is AddNewArticleAction.UpdateLocation -> updateLocation(location = action.location)
            is AddNewArticleAction.UpdatePhoto -> updatePhoto(photoUri = action.photoUri)
            is AddNewArticleAction.ShowUnitsDropdownMenu -> updateUnitsDropdownMenuVisibility(visible = true)
            is AddNewArticleAction.HideUnitsDropdownMenu -> updateUnitsDropdownMenuVisibility(visible = false)
            is AddNewArticleAction.UpdateArticleUnit -> updateArticleUnit(unit = action.unit)
        }
    }

    private fun saveArticle() {
        state.update { currentState ->
            currentState.copy(isLoading = true)
        }
        val articleName = state.value.articleName.text.toString()
        val articleDescription = state.value.articleDescription.text.toString()
        val articleQuantity = state.value.articleQuantity.text.toString().toDoubleOrNull()
        if (articleName.isBlank()) {
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    isError = true
                )
            }
            return
        }
        val article = state.value.article.copy(
            groupId = groupId,
            name = articleName,
            description = articleDescription,
            quantity = articleQuantity
        )
        viewModelScope.launch {
            articleRepository.upsertArticle(article)
            triggerSync(context)
            state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    okToSave = true,
                )
            }
        }
    }

    private fun updateLocation(location: Location) {
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

    private fun updateUnitsDropdownMenuVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(unitsDropdownMenuVisible = visible)
        }
    }

    private fun updateArticleUnit(unit: ArticleUnit) {
        state.update { currentState ->
            currentState.copy(
                article = currentState.article.copy(unit = unit)
            )
        }
    }

}

data class AddNewArticleState(
    val article: UIArticle = UIArticle(),
    val articleName: TextFieldState = TextFieldState(),
    val articleDescription: TextFieldState = TextFieldState(),
    val articleQuantity: TextFieldState = TextFieldState(),
    val unitsDropdownMenuVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val okToSave: Boolean = false,
)

sealed interface AddNewArticleAction {
    data object SaveArticle : AddNewArticleAction
    data class UpdateLocation(val location: Location) : AddNewArticleAction
    data class UpdatePhoto(val photoUri: String) : AddNewArticleAction
    data object ShowUnitsDropdownMenu : AddNewArticleAction
    data object HideUnitsDropdownMenu : AddNewArticleAction
    data class UpdateArticleUnit(val unit: ArticleUnit) : AddNewArticleAction
}
