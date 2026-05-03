package com.example.articles.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private var state = MutableStateFlow(GroupState())
    val uiState = state

    init {
        viewModelScope.launch {
            combine(
                groupRepository.getAllGroups(),
                articleRepository.getAllUngroupedArticles()
            ) { groups, articles -> groups to articles }
            .collectLatest { (groups, articles) ->
                state.update { currentState ->
                    currentState.copy(
                        groupNames = groups.map { it.name },
                        ungroupedArticles = articles
                    )
                }
            }
        }
    }

    fun handleAction(action: GroupAction) {
        when (action) {
            is GroupAction.CreateGroup -> createGroup(action.name)
            is GroupAction.ArticleClicked -> handleArticleClick(action.article, action.checked)
        }
    }

    private fun createGroup(name: String) = viewModelScope.launch {
        state.update { it.copy(isLoading = true) }
        groupRepository.addNewGroup(UIGroup(name = name))
        state.update { it.copy(isLoading = false) }
    }

    private fun handleArticleClick(article: UIArticle, checked: Boolean) {
        state.update { currentState ->
            currentState.copy(
                articlesToAdd =
                    if (checked) currentState.articlesToAdd + article
                    else currentState.articlesToAdd - article
            )
        }
    }

}

data class GroupState(
    val name: TextFieldState = TextFieldState(""),
    val groupNames: List<String> = emptyList(),
    val ungroupedArticles: List<UIArticle> = emptyList(),
    val articlesToAdd: List<UIArticle> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface GroupAction {
    data class CreateGroup(val name: String) : GroupAction
    data class ArticleClicked(val article: UIArticle, val checked: Boolean) : GroupAction
}
