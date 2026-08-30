package com.example.articles.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.articles.models.UIArticle
import com.example.articles.models.UIComment
import com.example.articles.models.UIGroup
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.CommentRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.ui.composables.ArticlesDestinations
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val commentRepository: CommentRepository,
    private val triggerSync: TriggerSync,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<ArticlesDestinations.GroupDetailsDestination>()
    private val groupId: UUID = UUID.fromString(args.groupId)

    private val state = MutableStateFlow(GroupDetailsState())
    val uiState = state.asStateFlow()

    private var latestGroupComments: List<UIComment> = emptyList()

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
        viewModelScope.launch {
            commentRepository.getAllCommentsForGroup(groupId).collectLatest { allComments ->
                latestGroupComments = allComments.filter { it.articleId == null }
                val articleCommentCounts = allComments
                    .mapNotNull { comment -> comment.articleId?.let { it to comment } }
                    .groupingBy { (articleId, _) -> articleId }
                    .eachCount()
                state.update { currentState ->
                    currentState.copy(
                        articleCommentCounts = articleCommentCounts,
                        groupComments = latestGroupComments
                    )
                }
            }
        }
    }

    fun handleAction(action: GroupDetailsAction) {
        when (action) {
            is GroupDetailsAction.ArticleLongClick -> onArticleLongClick(article = action.article)
            is GroupDetailsAction.HideBottomSheet -> onHideBottomSheet()
            is GroupDetailsAction.RemoveArticle -> removeArticle(article = state.value.articleToEditOrDelete ?: return)
            is GroupDetailsAction.MarkAsBought -> updateArticleStatus(article = state.value.articleToEditOrDelete ?: return, status = true)
            is GroupDetailsAction.MarkAsNotBought -> updateArticleStatus(article = state.value.articleToEditOrDelete ?: return, status = false)
            is GroupDetailsAction.UpdateExpandedStatus -> updateExpandedStatus(article = action.article, expanded = action.expanded)
            is GroupDetailsAction.ExpandAllArticles -> updateAllArticlesExpandedStatus(status = true)
            is GroupDetailsAction.CollapseAllArticles -> updateAllArticlesExpandedStatus(status = false)
            is GroupDetailsAction.ShowFilterDropdownMenu -> showFilterDropdownMenu()
            is GroupDetailsAction.HideFilterDropdownMenu -> hideFilterDropdownMenu()
            is GroupDetailsAction.FilterSelected -> onFilterSelected(filter = action.filter)
            is GroupDetailsAction.SelectMode -> selectMode(mode = action.mode)
            is GroupDetailsAction.ShowArticleComments -> showArticleComments(article = action.article)
            is GroupDetailsAction.HideCommentsSheet -> hideArticleComments()
            is GroupDetailsAction.AddComment -> addComment(text = action.text)
            is GroupDetailsAction.DeleteComment -> deleteComment(comment = action.comment)
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
            articleRepository.deleteArticleById(articleId = article.id)
            commentRepository.deleteCommentsByArticleId(groupId = article.groupId, articleId = article.id)
        }
    }

    private fun selectMode(mode: GroupDetailsMode) {
        state.update { currentState -> currentState.copy(mode = mode) }
    }

    private fun showArticleComments(article: UIArticle) {
        state.update { currentState ->
            currentState.copy(
                showEditDeleteBottonSheet = false,
                showCommentsSheet = true,
                articleCommentsTarget = article,
                articleComments = emptyList()
            )
        }
        viewModelScope.launch {
            commentRepository.getCommentsForArticle(articleId = article.id).collectLatest { comments ->
                state.update { it.copy(articleComments = comments) }
            }
        }
    }

    private fun hideArticleComments() {
        state.update { currentState ->
            currentState.copy(
                showCommentsSheet = false,
                articleCommentsTarget = null,
                articleComments = emptyList()
            )
        }
    }

    private fun addComment(text: String) {
        if (text.isBlank()) return
        val currentUser = Firebase.auth.currentUser ?: return
        val articleId = state.value.articleCommentsTarget?.id
        viewModelScope.launch {
            commentRepository.upsertComment(
                UIComment(
                    groupId = groupId,
                    articleId = articleId,
                    authorUid = currentUser.uid,
                    authorEmail = currentUser.email ?: "",
                    text = text
                )
            )
            triggerSync()
        }
    }

    private fun deleteComment(comment: UIComment) {
        viewModelScope.launch {
            commentRepository.deleteCommentById(comment.id)
            triggerSync()
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
                filterDropdownMenuVisible = false
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

enum class GroupDetailsMode {
    ARTICLES,
    COMMENTS,
}

data class GroupDetailsState(
    val group: UIGroup = UIGroup(),
    val articles: List<UIArticle> = emptyList(),
    val articleToEditOrDelete: UIArticle? = null,
    val showEditDeleteBottonSheet: Boolean = false,
    val filterDropdownMenuVisible: Boolean = false,
    val mode: GroupDetailsMode = GroupDetailsMode.ARTICLES,
    val groupComments: List<UIComment> = emptyList(),
    val articleCommentCounts: Map<UUID, Int> = emptyMap(),
    val showCommentsSheet: Boolean = false,
    val articleCommentsTarget: UIArticle? = null,
    val articleComments: List<UIComment> = emptyList(),
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
    data class SelectMode(val mode: GroupDetailsMode): GroupDetailsAction
    data class ShowArticleComments(val article: UIArticle): GroupDetailsAction
    data object HideCommentsSheet: GroupDetailsAction
    data class AddComment(val text: String): GroupDetailsAction
    data class DeleteComment(val comment: UIComment): GroupDetailsAction
}
