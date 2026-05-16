package com.example.articles.viewmodels

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.articles.models.UIArticle
import com.example.articles.repositories.ArticleRepository
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.triggerSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddNewArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.AddNewArticleDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private var state = MutableStateFlow(AddNewArticleState())
    val uiState = state

    fun handleAction(action: AddNewArticleAction) {
        when (action) {
            is AddNewArticleAction.SaveArticle -> saveArticle()
        }
    }

    private fun saveArticle() {
        val articleName = state.value.articleName.text.toString()
        val articleDescription = state.value.articleDescription.text.toString()
        val photoUri = state.value.article.photoUri
        if (articleName.isEmpty() && photoUri == null) {
            TODO("Show error if name and photo are empty")
        }
        val article = state.value.article.copy(
            groupId = groupId,
            name = articleName,
            description = articleDescription
        )
        viewModelScope.launch {
            articleRepository.upsertArticle(article)
            triggerSync(context)
        }
    }

}

data class AddNewArticleState(
    val article: UIArticle = UIArticle(),
    val articleName: TextFieldState = TextFieldState(),
    val articleDescription: TextFieldState = TextFieldState(),
)

sealed interface AddNewArticleAction {
    data object SaveArticle : AddNewArticleAction
}
