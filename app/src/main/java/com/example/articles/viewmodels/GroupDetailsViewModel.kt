package com.example.articles.viewmodels

import androidx.compose.runtime.currentComposer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.ui.composables.ArticlesDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.GroupDetailsDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private val state = MutableStateFlow(GroupDetailsState())
    val uiState = state

    init {
        viewModelScope.launch {
            combine(
                groupRepository.getGroupById(groupId),
                articleRepository.getArticlesByGroupId(groupId)
            ) { group, articles -> group to articles }
                .collectLatest { (group, articles) ->
                    state.update { currentState ->
                        currentState.copy(
                            articles = articles,
                            group = group
                        )
                    }
            }
        }
    }

    fun handleAction(action: GroupDetailsAction) {
        when (action) {
            is GroupDetailsAction.ArticleLongClick -> onArticleLongClick(action.article)
            is GroupDetailsAction.HideBottomSheet -> onHideBottomSheet()
            is GroupDetailsAction.RemoveArticle -> removeArticle(
                article = state.value.articleToEditOrDelete ?: return
            )
            is GroupDetailsAction.MarkAsBought -> updateArticleStatus(
                article = state.value.articleToEditOrDelete ?: return,
                status = true
            )
            is GroupDetailsAction.MarkAsNotBought -> updateArticleStatus(
                article = state.value.articleToEditOrDelete ?: return,
                status = false
            )
            is GroupDetailsAction.UpdateExpandedStatus -> updateExpandedStatus(
                article = action.article,
                expanded = action.expanded
            )
            is GroupDetailsAction.ExpandAllArticles -> updateAllArticlesExpandedStatus(true)
            is GroupDetailsAction.CollapseAllArticles -> updateAllArticlesExpandedStatus(false)
            is GroupDetailsAction.ShowFilterDropdownMenu -> showFilterDropdownMenu()
            is GroupDetailsAction.HideFilterDropdownMenu -> hideFilterDropdownMenu()
            is GroupDetailsAction.FilterSelected -> onFilterSelected(action.filter)
        }
    }

    private fun onArticleLongClick(article: UIArticle) {
        state.update { currentState ->
            currentState.copy(
                articleToEditOrDelete = article,
                showEditDeleteBottonSheet = true
            )
        }
    }

    private fun onHideBottomSheet() {
        state.update { currentState ->
            currentState.copy(
                articleToEditOrDelete = null,
                showEditDeleteBottonSheet = false
            )
        }
    }

    private fun removeArticle(article: UIArticle) {
        viewModelScope.launch {
            articleRepository.deleteArticleById(article.id)
        }
    }

    private fun updateArticleStatus(article: UIArticle, status: Boolean) {
        viewModelScope.launch {
            articleRepository.upsertArticle(article.copy(bought = status))
        }
    }

    private fun updateExpandedStatus(article: UIArticle, expanded: Boolean) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.map {
                    if (it.id == article.id) it.copy(expanded = expanded) else it
                }
            )
        }
    }

    private fun updateAllArticlesExpandedStatus(status: Boolean) {
        state.update { currentState ->
            currentState.copy(
                articles = currentState.articles.map { it.copy(expanded = status) }
            )
        }
    }

    private fun showFilterDropdownMenu() {
        state.update { currentState ->
            currentState.copy(
                filterDropdownMenuVisible = true
            )
        }
    }

    private fun hideFilterDropdownMenu() {
        state.update { currentState ->
            currentState.copy(
                filterDropdownMenuVisible = true
            )
        }
    }

    private fun onFilterSelected(filter: FilterType) {
        state.update {
            it.copy(filterDropdownMenuVisible = false)
        }
        viewModelScope.launch {
            val articlesFlow = when (filter) {
                FilterType.ALL -> articleRepository.getArticlesByGroupId(groupId)
                FilterType.BOUGHT -> articleRepository.getBoughtArticlesByGroupId(groupId)
                FilterType.UNBOUGHT -> articleRepository.getUnboughtArticlesByGroupId(groupId)
            }
            articlesFlow.collectLatest { articles ->
                state.update {
                    it.copy(articles = articles)
                }
            }
        }
    }

}

enum class FilterType {
    ALL,
    BOUGHT,
    UNBOUGHT,
}

data class GroupDetailsState(
    val group: UIGroup = UIGroup(),
    val articles: List<UIArticle> = emptyList(),
    val articleToEditOrDelete: UIArticle? = null,
    val showEditDeleteBottonSheet: Boolean = false,
    val filterDropdownMenuVisible: Boolean = false,
)

sealed interface GroupDetailsAction {
    data class ArticleLongClick(val article: UIArticle): GroupDetailsAction
    data object HideBottomSheet: GroupDetailsAction
    data object RemoveArticle: GroupDetailsAction
    data object MarkAsBought: GroupDetailsAction
    data object MarkAsNotBought: GroupDetailsAction
    data class UpdateExpandedStatus(val expanded: Boolean, val article: UIArticle): GroupDetailsAction
    data object ExpandAllArticles: GroupDetailsAction
    data object CollapseAllArticles: GroupDetailsAction
    data object ShowFilterDropdownMenu: GroupDetailsAction
    data object HideFilterDropdownMenu: GroupDetailsAction
    data class FilterSelected(val filter: FilterType): GroupDetailsAction
}
